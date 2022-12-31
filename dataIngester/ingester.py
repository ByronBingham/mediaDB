import glob
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

    for file in glob.glob(str(path) + "**/*", recursive=True):
        file_path = Path(file)
        if os.path.isfile(file) and file_path.suffix in valid_extensions:

            if jfif_webm_to_jpg and file_path.suffix() in ['.jfif', '.webp']:
                file_path = file_path.rename(file_path.with_suffix('.jpg'))
                files.append(os.path.abspath(file_path))
            else:
                files.append(file)

    return files


def process_group(group):
    """
    Processes a group
    """
    # set DB connection
    global db_conn
    db_conn = psycopg2.connect(host=group.db_host, user=group.db_user, password=group.db_password, dbname=DB_NAME)

    if group.target_db in [DBEnum.ART, DBEnum.NSFW_IMAGES]:
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
        files.extend(get_valid_files(path=dir, valid_extensions=group.valid_extensions))

    # get the data ready for inserting into DB
    values = []
    for file in files:
        file = file.replace("\\", "/")
        values.append(get_image_data_for_db_insert(file))

    if len(values) < 1:
        print("WARNING: No valid files found")
        return

    # send INSERT to DB
    cursor = db_conn.cursor()
    query = f'INSERT INTO {group.target_db.value} (md5, filename, file_path, resolution_width, resolution_height, file_size_bytes) VALUES '
    for value in values:
        query += value + ','
    query = query[:-1]
    query += ' ON CONFLICT (md5, filename) DO NOTHING;'

    cursor.execute(query)
    db_conn.commit()

    # add tag relations
    # TODO: implement
    """
    if group.auto_tag:
        for file in files:
            tags = get_tags_for_image(file)
    """
    cursor.close()


def get_tags_for_image(file: Path):
    """
    Uses DeepDanbooru to get the tags for an image
    """
    return []

def get_image_data_for_db_insert(file: Path):
    """
    Creates a string that can be appended to the VALUES part of an INSERT query
    """
    file_data = open(file, "rb")
    file_data = file_data.read()
    md5 = hashlib.md5(file_data).hexdigest()
    image = Image.open(file)
    resolution_width, resolution_height = image.size
    file_size_bytes = os.path.getsize(file)
    filename = os.path.basename(file)
    full_path = os.path.abspath(file).replace("\\", "/")

    return f'(\'{md5}\', \'{filename}\', \'{full_path}\', {resolution_width}, {resolution_height}, {file_size_bytes})'


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
    for group in config_groups_arr:
        group_db_enum = DBEnum[group['target_db']]
        auto_tag = False
        if 'group_db_enum' in group.keys():
            auto_tag = group['group_db_enum']
        jfif_to_jpg = False
        if 'jfif_webm_to_jpg' in group.keys():
            jfif_webm_to_jpg = group['jfif_webm_to_jpg']

        group = ProcessingGroup(name=group['name'], source_dirs=group['source_dirs'],
                                valid_extensions=group['valid_extensions'], target_db=group_db_enum, auto_tag=auto_tag,
                                jfif_webm_to_jpg=jfif_webm_to_jpg, db_host=config['db_host'], db_user=config['db_user'],
                                db_password=config['db_password'])
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
