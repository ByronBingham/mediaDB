# TODO
 - make script that creates database based on config file
   - change PK from filename to file_path
 - add more metadata to tables
   - music: artist, albumn, genre (just use tags maybe?), etc. link to cover art?
 - add prepared statements to API to speed up queries?
 - optimize ingester's check for existing files
 - web ui
   - add filters
     - resolution
     - size
   - change size of thumbnails based on veiwport size
 - acutally use json schemas to validate configs for API/ingester

## Ideas
 - Use DeepDanbooru to add tags to un-tagged images
  - https://github.com/KichangKim/DeepDanbooru