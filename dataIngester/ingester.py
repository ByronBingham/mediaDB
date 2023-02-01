import glob
import math
from multiprocessing import Pool
import os
import sys
from ProcessingGroup import ProcessingGroup
from pathlib import Path
import json
import hashlib
import psycopg2
from PIL import Image
from DBEnum import DBEnum
from tagger import get_filtered_tags

DB_NAME = "bmedia"

# each forked process should create its own connection to the DB
db_conn = None


def get_valid_files(path: str, valid_extensions: list, jfif_webm_to_jpg: bool = False) -> list:
    """
    Given the specified base directory and list of valid file types (extensions), searches
    the directory for all files recursively
    """
    files = []
    path = Path(path)
    if not path.is_dir():
        print("ERROR: the specified path is not a directory")
        return []

    for file in Path(path).rglob('*'):
        file_path = Path(file)
        if os.path.isfile(file) and file_path.suffix in valid_extensions:

            if jfif_webm_to_jpg and file_path.suffix in ['.jfif', '.webp']:
                file_path = file_path.rename(file_path.with_suffix('.jpg'))
                files.append(str(os.path.abspath(file_path)))
            else:
                files.append(str(file))

    return files


def process_group(group):
    """
    Processes a group
    """
    # set DB connection
    global db_conn
    db_conn = psycopg2.connect(host=group.db_host, user=group.db_user, password=group.db_password, dbname=DB_NAME)

    if group.target_db in [DBEnum.ART, DBEnum.NSFW_IMAGES, DBEnum.MEMES]:
        process_image_group(group)
    elif group.target_db in [DBEnum.VIDEOS]:
        process_video_group(group)
    elif group.target_db in [DBEnum.MUSIC]:
        process_music_group(group)
    elif group.target_db in [DBEnum.MUSIC_PLAYLISTS]:
        process_music_playlist_group(group)
    else:
        print(
            "ERROR: the target database \"" + group.target_db.value + "\" is not a valid target. Skipping this processing group...")

    try:
        db_conn.close()
    except:
        print("Something went wrong trying to close DB connection")


def process_image_group(group: ProcessingGroup):
    """
    Process a group of image files
    """

    files = []
    for dir in group.source_dirs:
        files.extend(get_valid_files(path=dir, valid_extensions=group.valid_extensions, jfif_webm_to_jpg=group.jfif_webm_to_jpg))

    print("INFO: Found " + str(len(files)) + " files")

    # check if any of the files are already in the database
    if group.skip_existing:
        newFiles = []
        for file in files:
            path = Path(file)
            md5 = get_md5_for_file(path.resolve())
            filename = path.name

            query = f'SELECT md5, filename FROM {group.target_db.value} WHERE md5 = \'{md5}\' AND filename = \'{filename}\';'
            result = do_query(query=query)

            if len(result) < 1:
                newFiles.append(file)
        files = newFiles

    print("INFO: Processing " + str(len(files)) + " new files")

    chunk_size = group.chunk_size
    if chunk_size < 1:
        chunk_size = len(files)
    
    # proccess chunks of files
    current_chunk = 1
    total_chunks = math.ceil(len(files) / chunk_size)
    for i in range(0, len(files), chunk_size):
        end_index = i + chunk_size
        if end_index > len(files) - 1:
            end_index = len(files) - 1
        files_chunk = files[i:end_index + 1] # end of list range is exclusive
        values = []
        tag_values = ""
        tag_join_values = ""
        for file in files_chunk:
            # get the data ready for inserting into DB
            file = file.replace("\\", "/")
            query_vals, join_data = get_image_data_for_db_insert(file)
            values.append(query_vals)

        if len(values) < 1:
            print("WARNING: No valid files found")
            return

        # send INSERT to DB for image files
        query = f'INSERT INTO {group.target_db.value} (md5, filename, file_path, resolution_width, resolution_height, file_size_bytes) VALUES \n'
        for value in values:
            query += value + ',\n'
        query = query[:-2]
        query += '\n ON CONFLICT (md5, filename) DO NOTHING;'

        print("INFO: Applying changes from group " + group.name + " to Database")
        do_query(query)

        # get data ready for tags if specified
        if group.auto_tag:
            print("INFO: Processing tags for images")

            # get tag data from DeepDanbooru
            tags_out = get_filtered_tags(files_chunk, group.tag_prob_thres)

            # get list of all tags found in this group
            all_tags = []
            for image_tags in list(tags_out.values()):
                for tag, prob in image_tags:
                    all_tags.append(tag)

            for tag in all_tags:
                # add tag to DB if not already existing
                cleaned_tag = tag.replace('\'','\'\'' )
                tag_values += f"('{cleaned_tag}', false),\n" # default 'false' for nsfw. set manually after adding to DB

            for key in tags_out.keys():
                filename = Path(key).name
                tag_data = tags_out[key]
                for tag, prob in tag_data:
                    md5 = get_md5_for_file(Path(key).resolve())
                    cleaned_tag = tag.replace('\'','\'\'' )
                    tag_join_values += f'(\'{md5}\', \'{filename}\', \'{cleaned_tag}\'),\n'

            tags_query = "INSERT INTO bmedia_schema.tags (tag_name, nsfw) VALUES \n" + tag_values[:-2] + "\n ON CONFLICT (tag_name) DO NOTHING;"
            tag_join_query = f"INSERT INTO {group.target_db.value}_tags_join (md5, filename, tag_name) VALUES \n" + tag_join_values[:-2] + "\n ON CONFLICT (md5, filename, tag_name) DO NOTHING;"

            do_query(tags_query)
            do_query(tag_join_query)
        print("INFO: Done processing chunk " + str(current_chunk) + "/" + str(total_chunks))
        current_chunk += 1

    print("INFO: Done processing group " + group.name)
    

def do_query(query: str):
    cursor = db_conn.cursor()
    '''
    file_tmp = open("./tmp_out.txt", 'w')
    file_tmp.write("--------------------------------------------------------------------------------------------")
    file_tmp.write(query)
    file_tmp.write("--------------------------------------------------------------------------------------------")
    file_tmp.close()
    '''
    cursor.execute(query)
    result = None
    if cursor.description != None:
        result = cursor.fetchall()
    db_conn.commit()
    cursor.close()

    return result

def get_md5_for_file(path: Path):
    file_data = open(path, "rb")
    file_data = file_data.read()
    return hashlib.md5(file_data).hexdigest()


def get_image_data_for_db_insert(file: Path):
    """
    Creates a string that can be appended to the VALUES part of an INSERT query
    """
    md5 = get_md5_for_file(file)
    image = Image.open(file)
    resolution_width, resolution_height = image.size
    file_size_bytes = os.path.getsize(file)
    filename = os.path.basename(file)
    full_path = os.path.abspath(file).replace("\\", "/")

    return (f'(\'{md5}\', \'{filename}\', \'{full_path}\', {resolution_width}, {resolution_height}, {file_size_bytes})', (md5, filename))


def process_video_group(group: ProcessingGroup):
    """
    Process a group of video files
    """


def process_music_group(group: ProcessingGroup):
    """
    Process a group of image files
    """


def process_music_playlist_group(group: ProcessingGroup):
    """
    Process a group of image files
    """


def parse_config(config_path: Path) -> dict:
    """
    Reads and parses the main config file.
    """

    json_text = open(config_path, 'r').read()
    json_obj = json.loads(json_text)
    return json_obj


if __name__ == "__main__":
    n = len(sys.argv)
    print(sys.argv)
    if n > 2 or n < 2:
        print(
            "Incorrect number of arguments passed. This script should take one argument that points to the config file.")
        exit()

    config_path = Path(sys.argv[1])
    config = None
    if config_path.exists() and config_path.is_file():
        config = parse_config(config_path)
    else:
        print("The config path passed in is either not a file or does not exist.")
        exit()

    if config == None:
        print("Something went wrong parsing the config.")
        exit()

    process = config['num_of_processes']

    # Create processing groups from config
    config_groups_arr = config['ingest_groups']
    processing_groups = []
    chunk_size = config["chunk_size"]
    for group in config_groups_arr:
        group_db_enum = DBEnum[group['target_db']]
        auto_tag = False
        tag_prob_thres = 1.0
        if 'auto_tag' in group.keys():
            auto_tag = group['auto_tag']
            if 'tag_prob_thres' in group.keys():
                tag_prob_thres = group['tag_prob_thres']
        jfif_to_jpg = False
        if 'jfif_webm_to_jpg' in group.keys():
            jfif_webm_to_jpg = group['jfif_webm_to_jpg']
        skip_existing = False
        if 'skip_existing' in group.keys():
            skip_existing = group['skip_existing']

        group = ProcessingGroup(name=group['name'], source_dirs=group['source_dirs'],
                                valid_extensions=group['valid_extensions'], target_db=group_db_enum, auto_tag=auto_tag,
                                jfif_webm_to_jpg=jfif_webm_to_jpg, db_host=config['db_host'], db_user=config['db_user'],
                                db_password=config['db_password'], tag_prob_thres=tag_prob_thres, chunk_size=chunk_size,
                                skip_existing=skip_existing)
        processing_groups.append(group)

    # process groups
    if config['num_of_processes'] > 1:
        processing_pool = Pool(processes=config['num_of_processes'])
        processing_pool.map(process_group, processing_groups)
    else:
        for group in processing_groups:
            process_group(group)

    print("Finished processing all Processing groups.\nExiting...")
    exit()
