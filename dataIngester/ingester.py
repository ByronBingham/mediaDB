import sys
from enum import Enum
from dataIngester.ProcessingGroup import ProcessingGroup
from pathlib import Path
import json


DBEnum = Enum('DBEnum',['ART', 'MUSIC', 'VIDEOS', 'MUSIC_PLAYLISTS', 'NSFW_IMAGES'])


def process_file(file_path: str, target_db: DBEnum):
    """
    Completely processes a file.
    Loads file, calculates MD5, tags data, and uploads data to the DB
    """
    print()

def process_group(group: ProcessingGroup):
    """
    Processes a group
    """
    print()

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
        if 'jfif_to_jpg' in group.keys():
            jfif_to_jpg = group['jfif_to_jpg']
        processing_groups.append(ProcessingGroup(name=group['name'], source_dirs=group['source_dirs'], target_db=group_db_enum,auto_tag=auto_tag, jfif_to_jpg=jfif_to_jpg))


