-- make sure there's no leftovers
DROP TABLE IF EXISTS bmedia_schema.tag_category_join;
DROP TABLE IF EXISTS bmedia_schema.videos_tags_join;
DROP TABLE IF EXISTS bmedia_schema.music_playlists_tags_join;
DROP TABLE IF EXISTS bmedia_schema.music_tags_join;
DROP TABLE IF EXISTS bmedia_schema.art_tags_join;
DROP TABLE IF EXISTS bmedia_schema.nsfw_images_tags_join;
DROP TABLE IF EXISTS bmedia_schema.memes_tags_join;

DROP TABLE IF EXISTS bmedia_schema.tags;
DROP TABLE IF EXISTS bmedia_schema.videos;
DROP TABLE IF EXISTS bmedia_schema.tag_categories;
DROP TABLE IF EXISTS bmedia_schema.music_playlists;
DROP TABLE IF EXISTS bmedia_schema.music;
DROP TABLE IF EXISTS bmedia_schema.art;
DROP TABLE IF EXISTS bmedia_schema.nsfw_images;
DROP TABLE IF EXISTS bmedia_schema.memes;

DROP SCHEMA IF EXISTS bmedia_schema;
DROP DATABASE IF EXISTS bmedia WITH (FORCE);
DROP ROLE IF EXISTS bmedia_admin;
DROP ROLE IF EXISTS bmedia_user;