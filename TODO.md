# TODO
 - add more metadata to tables
   - music: artist, albumn, genre (just use tags maybe?), etc. link to cover art?
 - add GUI to edit tags and tag categories
 - remove entry from DB if file moved/doesn't exist
 - add prepared statements to API to speed up queries?
 - optimize ingester's check for existing files
 - web ui
   - add DB selector or implement web ui as a template so it's easy to create multiple webui's
   - add filters
     - resolution
     - size
   - change size of thumbnails based on veiwport size
   - add tag editor page
   - add tag add/delete option to image viewer

## Ideas
 - Use DeepDanbooru to add tags to un-tagged images
  - https://github.com/KichangKim/DeepDanbooru