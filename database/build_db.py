import json
from pathlib import Path
import sys
from jsonschema import ValidationError, validate


def parse_config(config_path: Path, schema_path: Path):
    print("Parsing Config")
    config_data = json.loads(config_path.read_text())
    schema_data = json.loads(schema_path.read_text())
    try:
        validate(instance=config_data, schema=schema_data)
    except ValidationError as e:
        print("Validation for \"" + str(config_path.absolute()) + "\" failed. Reason:")
        print(e)
        exit(-1)
    
    return config_data

def create_sql_script(config_data: dict):
    out = ""

    out += "-- Create Users\n"
    out += "CREATE USER " + config_data["admin_username"] + " WITH PASSWORD '" + config_data["admin_password"] + "';\n"
    out += "CREATE USER " + config_data["query_username"] + " WITH PASSWORD '" + config_data["query_password"] + "';\n\n"

    out += "-- Create Database\n"
    out += "CREATE DATABASE " + config_data["database_name"] + " OWNER " + config_data["admin_username"] + ";\n"
    out += "\c " + config_data["database_name"] + ";\n\n"

    out += "-- Create Schema\n"
    out += "CREATE SCHEMA " + config_data["database_schema"] + " AUTHORIZATION " + config_data["admin_username"] + ";\n\n"

    out += "-- Create Static Tables\n"
    out += "CREATE TABLE " + config_data["database_schema"] + ".tags(\
tag_name VARCHAR(255),\
nsfw BOOLEAN,\
PRIMARY KEY (tag_name),\
UNIQUE (tag_name));\n\n"
    out += "CREATE TABLE " + config_data["database_schema"] + ".collections(\
collection_name VARCHAR(255),\
nsfw BOOLEAN,\
PRIMARY KEY (collection_name),\
UNIQUE (collection_name));\n\n"
    
    out += "-- Create User-specified Tables\n"
    for table_spec in config_data["tables"]:
        if table_spec["table_type"] == "image":
            out += "CREATE TABLE " + config_data["database_schema"] + "." + table_spec["table_name"] +"(\
id BIGSERIAL UNIQUE,\
md5 VARCHAR(32),\
filename VARCHAR(255),\
file_path VARCHAR(255) UNIQUE,\
resolution_width INTEGER,\
resolution_height INTEGER,\
file_size_bytes INTEGER,\
date_added TIMESTAMP NOT NULL DEFAULT NOW(),\
PRIMARY KEY (id));\n"
            out += "CREATE INDEX " + table_spec["table_name"] + "_index ON " + config_data["database_schema"] + "." + table_spec["table_name"] + "(resolution_width, resolution_height, file_size_bytes, md5);\n"

            out += "CREATE TABLE " + config_data["database_schema"] + "." + table_spec["table_name"] + "_tags_join(\
id BIGINT,\
tag_name VARCHAR(255),\
FOREIGN KEY (id) REFERENCES " + config_data["database_schema"] + "." + table_spec["table_name"] + "(id),\
FOREIGN KEY (tag_name) REFERENCES " + config_data["database_schema"] + ".tags(tag_name),\
UNIQUE (id, tag_name));\n\n"
        elif table_spec["table_type"] == "music":
            out += "CREATE TABLE " + config_data["database_schema"] + "." + table_spec["table_name"] + "(\
id BIGSERIAL UNIQUE,\
md5 VARCHAR(32),\
filename VARCHAR(255),\
file_path VARCHAR(255) UNIQUE\
);\n"
            out += "CREATE INDEX " + table_spec["table_name"] + "_index ON " + config_data["database_schema"] + "." + table_spec["table_name"] + "(md5);\n"
            out += "CREATE TABLE " + config_data["database_schema"] + "." + table_spec["table_name"] + "_tags_join(\
id BIGINT,\
tag_name VARCHAR(255),\
FOREIGN KEY (id) REFERENCES " + config_data["database_schema"] + "." + table_spec["table_name"] + "(id),\
FOREIGN KEY (tag_name) REFERENCES " + config_data["database_schema"] + "." + table_spec["table_name"] + "_playlists(playlist_name),\
UNIQUE (id, tag_name));\n\n"
            out += "CREATE TABLE " + config_data["database_schema"] + "." + table_spec["table_name"] + "_playlists(\
playlist_name VARCHAR(255),\
PRIMARY KEY (playlist_name),\
UNIQUE (playlist_name));\n\n"
            out += "CREATE TABLE " + config_data["database_schema"] + "." + table_spec["table_name"] + "_playlists_join(\
id BIGINT,\
playlist_name VARCHAR(255),\
FOREIGN KEY (id) REFERENCES " + config_data["database_schema"] + "." + table_spec["table_name"] + "(id),\
FOREIGN KEY (playlist_name) REFERENCES " + config_data["database_schema"] + "." + table_spec["table_name"] + "_playlists(playlist_name),\
UNIQUE (id, playlist_name));\n\n"
            
        else:
            print("Table type " + table_spec["table_type"] + " is invalid or not yet implemented")
            continue

    out += "GRANT CREATE, CONNECT, TEMPORARY ON DATABASE " + config_data["database_name"] + " TO " + config_data["admin_username"] + ";\n"
    out += "GRANT CONNECT ON DATABASE " + config_data["database_name"] + "  TO " + config_data["query_username"] + ";\n"
    out += "GRANT CREATE, USAGE ON SCHEMA " + config_data["database_schema"] + " TO " + config_data["admin_username"] + ";\n"
    out += "GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON ALL TABLES IN SCHEMA " + config_data["database_schema"] + " TO " + config_data["admin_username"] + ";\n"
    out += "GRANT SELECT ON ALL TABLES IN SCHEMA " + config_data["database_schema"] + " TO " + config_data["admin_username"] + ";\n"
    out += "GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA " + config_data["database_schema"] + " TO " + config_data["admin_username"] + ";"

    outFile = open("build_db.sql", "w")
    outFile.write(out)
    print("Create SQL script \"build_db.sql.sql\"")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Missing parameters. Usage: `build_db [schema path] [config path]`")
        exit()
    print("Generating SQL script")
    schema_path = Path(sys.argv[1])
    config_path = Path(sys.argv[2])

    if not config_path.exists():
        print("Specified config does not exist. Exiting...")
        exit(-1)
    config_data = parse_config(config_path=config_path, schema_path=schema_path)
    create_sql_script(config_data=config_data)

