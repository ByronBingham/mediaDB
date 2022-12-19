import glob
from multiprocessing import Pool
import os
import sys
from enum import Enum
from dataIngester.ProcessingGroup import ProcessingGroup
from pathlib import Path
import json
import hashlib
import psycopg2


#DBEnum = Enum('DBEnum',['ART', 'MUSIC', 'VIDEOS', 'MUSIC_PLAYLISTS', 'NSFW_IMAGES'])
class DBEnum(Enum):
    ART = 'bmedia_schema.art'
    MEMES = 'bmedia_schema.memes'
    MUSIC = 'bmedia_schema.music'
    VIDEOS = 'bmedia_schema.videos'
    MUSIC_PLAYLISTS = 'bmedia_schema.music_playlists'
    NSFW_IMAGES = 'bmedia_schema.nsfw_images'

DB_NAME = "bmedia"


# each forked process should create its own connection to the DB
db_conn = None

def get_valid_files(path: str, valid_extensions: list, jfif_webm_to_jpg: bool = False) -> list:
    """
    Given the specified base directory and list of valid file types (extensions), searches
    the directory for all files recursively
    """
    files = []
    if not path.is_dir():
        print("ERROR: the specified path is not a directory")
        return []

    for file in glob.glob(path + "**/*", recursive=True):
        file_path = Path(file)
        if os.path.isfile(file) and file_path.suffix in valid_extensions:

            if jfif_webm_to_jpg and file_path.suffix() in ['.jfif', '.webp']:
                file_path = file_path.rename(file_path.with_suffix('.jpg'))
                files.append(os.path.abspath(file_path))
            else:
                files.append(file)

    return files

def process_group(group: ProcessingGroup):
    """
    Processes a group
    """
    # set DB connection
    db_conn = psycopg2.connect(host=group.db_host, user=group.db_user, password=group.db_password, dbname=DB_NAME)

    if group.target_db in ['ART', 'NSFW_IMAGES']:
        process_image_group(group)
    elif group.target_db in ['VIDEOS']:
        process_video_group(group)
    elif group.target_db in ['MUSIC']:
        process_music_group(group)
    elif group.target_db in ['MUSIC_PLAYLISTS']:
        process_music_playlist_group(group)
    else:
        print("ERROR: the target database \"" + group.target_db + "\" is not a valid target. Skipping this processing group...")

    db_conn.close()

def process_image_group(group: ProcessingGroup):
    """
    Process a group of image files
    """
    files = []
    for dir in group.source_dirs:
        files.append(get_valid_files(path=dir, valid_extensions=group.valid_extensions))
    
    # get the data ready for inserting into DB
    values = []
    for file in files:
        values.append(get_image_data_for_db_insert(file))

    # send INSERT to DB
    cursor = db_conn.cursor()
    query = f'INSERT INTO {group.target_db.value} (md5, filename, file_path) VALUES '
    for value in values:
        query += value + ','
    query = query[:-1]
    query += 'ON CONFLICT (md5, filename) DO UPDATE;'

    cursor.execute()
    db_conn.commit()

    cursor.close()

def get_image_data_for_db_insert(file: Path):
    """
    Creates a string that can be appended to the VALUES part of an INSERT query
    """
    md5 = hashlib.md5(file).hexdigest()
    filename = os.path.basename(file)
    full_path = os.path.abspath(file)    

    return f'(\'{md5}\', \'{filename}\', \'{full_path}\')'

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

    json_text = open(config_path, 'r')
    json_obj = json.loads(json_text)
    return json_obj


if __name__ == "__main__":
    n = len(sys.argv)
    if n > 1 or n < 1:
        print("Incorrect number of arguments passed. This script should take one argument that points to the config file.")
        exit()
    
    config_path = Path(sys.argv[0])
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
        processing_groups.append(ProcessingGroup(name=group['name'], source_dirs=group['source_dirs'], valid_extensions=["valid_extensions"], target_db=group_db_enum,auto_tag=auto_tag, jfif_webm_to_jpg=jfif_webm_to_jpg))    

    # process groups
    processing_pool = Pool(processes=config['num_of_processes'])
    processing_pool.map(process_group, processing_groups)

    print("Finished processing all Processing groups.\nExiting...")
    exit()


