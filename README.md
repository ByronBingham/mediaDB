# Media Database
Database project to store personal data on local servers. The goal of this project is to make sorting through media easier than digging through directories that contain hundreds/thousands of media items.

## Structure/Components

A PostgreSQL database is used to store all metadata about media. The acutal data is stored on the file sytstem, and the database includes paths to the actual files. An API is used by clients to make requests to the database and filesystem. A data ingester is used to populate the database and is responsible for processing data before entry into the database (e.g. auto-tagging images).

# Software Requirements
 - PostgreSQL v15.1
 - Java v11 (Adopt Open JDK)
 - PostgreSQL JDBC Driver 42.5.1
 - DeepDanbooru 1.0 (required for image auto-tagging)
   - Cuda v11.2
 - NodeJS v18.13.0
 - Apache Tomcat
 - Docker (optional)

## DeepDanbooru

DeepDanbooru (DD) is required to use the auto-tagging feature of the data ingester. It requires some setup before using.

Follow these steps to prepare your environment to use DeepDanbooru:
 - Clone the DD repo and cd into it. `https://github.com/KichangKim/DeepDanbooru`
 - Install DD's python dependencies: `pip install -r requirements.txt`
   - To use GPU acceleration, you will need to install the CUDA toolkit: `https://developer.nvidia.com/cuda-11.2.0-download-archive`
   - You may want to use a virtual environment to isolate your pip install's from your global python depencies (do this before installing requirments)
     - Run `python -m venv venv` to create a virtual environment
     - Then run `venv/Scripts/activate` to activate and start using the virtual environment
 - In the DD repo, run `python ./setup.py install`
 - Copy the contents of `[mediaDB repo dir]/dddata/` into `[DeepDanbooru repo dir]/`
 - Download the pretrained model `https://github.com/KichangKim/DeepDanbooru/releases/download/v3-20211112-sgd-e28/deepdanbooru-v3-20211112-sgd-e28.zip`
   - Unzip and copy `[unzipped dir]/model-resnet_custom_v3.h5` to `[DeepDanbooru repo dir]/`

After completing these steps, you should be able to call `deepdanbooru` (this assumes your virtual environment is activated on the terminal you are using, or you installed DD globally)

# Usage

The media DB can be run locally, but this readme will focus on how to use Docker to spin up the mediaDB (the ingester is not yet dockerized, so it will have instructions to run locally).

## Before Building/Running Docker Containers

### Database

The database can either be created from scratch, or if you already have a local database that is populated, you can dump the existing database and use it as the base for the docker database. A database is automatically created when starting up the Docker container.

Before running Docker, you must edit the `database/db_config_Template.json` file and rename it to `db_config.json`. Documentation of this config file can be found in `docs/db_config_schema.html`.

#### Using Existing Database:
If you have a local DB that you want to import to a Docker container, you can create a dump of the database by running the following command:

`pg_dump -U postgres -c -Ft -f dump.tar [name of database]`

Then, copy the dump file to `database/dump.tar` and un-comment the last two lines of `docker/database/DatabaseDockerfile`.

### API

The API uses one config file, and it must be initialized before running in Docker. A template config file can be found in `database/db_config_Template.json`. Fill out this template, and rename it to `db_config.json`. Documentation of this config file can be found in `docs/db_config_schema.html`. This is the same `db_config.json` that the database uses to create its tables. The API also uses a SSL cert; see the Web UI section below for instruction on making the cert.

### Web UI

Before running Docker, you must build the webapps you want and place them in `tomcat/webapps`. This directory can be wherever, but the path must be relative to where the docker-compose file is run. You must also create a keystore with a cert for tomcat and place the `.keystore` file in `./tomcat/conf/.keystore`. You can run `./tomcat/createSSLCert.[bat/sh]` to quickly create a cert. It would be better to set up a cert authority service, but that is not covered in this repo, at least not yet.

#### Building a Webapp

To generate a template webapp, cd into `webapp_template/` and run `runWebpack.sh` or `runWebpack.bat` (depending on your OS). This will generate a `dist` folder. Copy this folder to the `tomcat/webapps` (discussed above) and rename the `dist` folder to whatever you would like. Remember what you rename this folder to, you will use it later to access the webapp.

The two files from the template webapp you will want to edit are `template.css` and `settings.js`.

#### Customizing a Webapp

##### Template.css

There are two variables that are meant to be edited: `accent-color-primary` and `background-color-primary`. These just change the background and accent color of the webapp. You do not need to edit this file if you don't want to customize any of the template webapp's styling.

##### Settings.js

Here is a list the properties contained in `settings.js`. You must fill out this file before starting Docker or you will get errors:
 - `db_table_name`: This should be the name of the image DB table you want the webapp to pull from. This should match a table name from your `db_config.json`.
 - `default_images_per_page`: Number of images shown per page.
 - `webapp_name`: Short name of the webapp.
 - `webapp_long_name`: Long name of the webapp.
 - `server_addr`: This address should point to the tomcat server's address. This is the address you will use to access the webapp. Should include the port (default `8080`).
 - `api_addr`: This address should point to the API server's address. The webapp uses this address to make requests to the database and filesystem. Should include the port (default `38001`).
 - `thumb_height`: Size of the images shown on the webapp.

### Compose File

The docker-compose file contains all of the settings to build/run the docker containers. You must copy and modify `compose_exmple.yml`; it should be renamed to `compose.yml` after editing it.

Here are a list of parameters in the compose file that need to be changed:
 - `database/volumes`: `[host data dir]` should be changed to a path on the docker host system that will store the databases data outside of the container.
 - `volumes/cifs_mount`: 
   - `[file share address]`: should be changed to the address of a file share (SMB, etc.)
   - `[share dir]`: should be changed to the path of the fileshare on the share host system
   - `[username]` and `[password]`: should be changed to the username/password that has access to the share

If the media data is stored in the same filesystem as the API and data ingester, you could change the `cifs_mount` volume to a normal mount (not a fileshare).

### MEDIA_SHARE
This environment variable should point to a share that contains all the media for the entire database. The path of all files must start in this folder. This was done to make pathing easier in Docker containers. In a container, this will point to one dir, but you could mount multiple volumes (from different host dirs/drives) into this folder if you want to add dirs for different dirs/shares/etc. This variable will be different for every system; for example, inside the API's docker container, this variable is set to `/share`, but on a Windows machine used to run the data ingester, it might be `Z:\`.

## Building/Running Docker Containers

Once you have completed all of the prerequisite steps above, you should be able to run the following command. It should be run in the base directory of this repo:

`docker-compose up`

This will build docker images for the database, API, and webserver. You should then be able to access your webapps at `http://[docker host ip]:8080/[webapp name]`.

## Data Ingester

Currently, the data ingester is not dockerized. To run the DD auto-tagging in a Docker container (efficiently) would require installing docker plugins for GPU acceleration. This may be implemented in the future, but for now, it is inteded to run standalone on a normal Windows or Linux OS.

Before running the data ingester, edit `ingesterConfig_Example.json` and rename it to `ingesterConfig.json`. Documentation of this config file can be found in `docs/ingesterConfig_schema.html`.

After editing the config file, run the `runIngester` script (`dataIngester.jar` and `ingesterConfig.json` must be in the same directory).

# NOTES
 - If you are having issues with encoding on Windows (e.g. Japanese characters in files breaking the ingestion script), change your system locale to use UTF-8
 - If you get an error like "FileNotFoundError: [WinError 206] The filename or extension is too long", set the `chunk_size` property in the ingester config to a smaller
  number. This issue can occur if the command to DeepDanbooru is too long