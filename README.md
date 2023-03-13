# Media Database
Database project to store personal data on local servers. The goal of this project is to make sorting through media easier than digging through directories that contain hundreds/thousands of media items.

# Software Requirements
 - PostgreSQL v15.1
 - Java v16 (Adopt Open JDK)
 - PostgreSQL JDBC Driver 42.5.1
 - DeepDanbooru 1.0
   - Cuda v11.2
 - NodeJS v18.13.0

## Installing Python Requirements

## DeepDanbooru
To build DeepDanbooru...
After building DeepDanbooru all Python dependencies, cd into `./DeepDanbooru` and run `pyhton ./setup.py install`. 

## Dumping DB For Docker:
run this:
`pg_dump -U postgres -c -Ft -f dump.tar [name of database]`

## MEDIA_SHARE
This environment variable should point to a share that contains all the media for the entire database. The path of all files must start in this folder. This was done to make pathing easier in Docker containers.
In a container, this will point to one dir, but you could mount multiple volumes (from different host dirs/drives) into this folder if you want to add dirs for different dirs/shares/etc.

## NOTES
 - If you are having issues with encoding on Windows (e.g. Japanese characters in files breaking the ingestion script), change your system locale to use UTF-8
 - If you get an error like "FileNotFoundError: [WinError 206] The filename or extension is too long", set the `chunk_size` property in the ingester config to a smaller
  number. This issue can occur if the command to DeepDanbooru is too long