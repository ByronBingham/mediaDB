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
DROP DATABASE IF EXISTS bmedia;
DROP ROLE IF EXISTS bmedia_admin;
DROP ROLE IF EXISTS bmedia_user;

-- Create users
CREATE USER bmedia_admin WITH PASSWORD 'changeme';
CREATE ROLE bmedia_user WITH PASSWORD 'changeme';

-- Create database
CREATE DATABASE bmedia OWNER bmedia_admin;
\c bmedia;

-- Create schema
CREATE SCHEMA bmedia_schema AUTHORIZATION bmedia_admin;

-- Create main tables
CREATE TABLE bmedia_schema.art(
    md5 VARCHAR(32),
    filename VARCHAR(255),
    file_path VARCHAR(255),
    resolution_width INTEGER,
    resolution_height INTEGER,
    file_size_bytes INTEGER,
    PRIMARY KEY (md5, filename),
    UNIQUE (file_path)
);
CREATE INDEX art_index ON bmedia_schema.art(resolution_width, resolution_height, file_size_bytes);

CREATE TABLE bmedia_schema.music(
    md5 VARCHAR(32),
    filename VARCHAR(255),
    song_name VARCHAR(255),
    file_path VARCHAR(255),
    user_rating FLOAT,
    PRIMARY KEY (md5, filename),
    UNIQUE (file_path)
);

CREATE TABLE bmedia_schema.music_playlists(
    playlist_name VARCHAR(255),
    file_path VARCHAR(255),        
    PRIMARY KEY (playlist_name),
    UNIQUE (file_path)
);

CREATE TABLE bmedia_schema.videos(
    md5 VARCHAR(32),
    filename VARCHAR(255),
    file_path VARCHAR(255),
    PRIMARY KEY (md5, filename),
    UNIQUE (file_path)
);

CREATE TABLE bmedia_schema.tags(
    tag_name VARCHAR(255),
    nsfw BOOLEAN,
    PRIMARY KEY (tag_name),
    UNIQUE (tag_name)
);

CREATE TABLE bmedia_schema.tag_categories(
    tag_category_name VARCHAR(255),
    nsfw BOOLEAN,
    PRIMARY KEY (tag_category_name),
    UNIQUE (tag_category_name)
);

-- Create join tables
CREATE TABLE bmedia_schema.art_tags_join(
    md5 VARCHAR(32),
    filename VARCHAR(255),
    tag_name VARCHAR(255),
    FOREIGN KEY (md5, filename) REFERENCES bmedia_schema.art(md5, filename),
    FOREIGN KEY (tag_name) REFERENCES bmedia_schema.tags(tag_name),
    UNIQUE (md5, filename, tag_name)
);

CREATE TABLE bmedia_schema.music_tags_join(
    md5 VARCHAR(32),
    filename VARCHAR(255),
    tag_name VARCHAR(255),
    FOREIGN KEY (md5, filename) REFERENCES bmedia_schema.music(md5, filename),
    FOREIGN KEY (tag_name) REFERENCES bmedia_schema.tags(tag_name),
    UNIQUE (md5, filename, tag_name)
);

CREATE TABLE bmedia_schema.music_playlists_tags_join(
    playlist_name VARCHAR(255),
    tag_name VARCHAR(255),
    FOREIGN KEY (playlist_name) REFERENCES bmedia_schema.music_playlists(playlist_name),
    FOREIGN KEY (tag_name) REFERENCES bmedia_schema.tags(tag_name),
    UNIQUE (playlist_name, tag_name)
);

CREATE TABLE bmedia_schema.videos_tags_join(
    md5 VARCHAR(32),
    filename VARCHAR(255),
    tag_name VARCHAR(255),
    FOREIGN KEY (md5, filename) REFERENCES bmedia_schema.videos(md5, filename),
    FOREIGN KEY (tag_name) REFERENCES bmedia_schema.tags(tag_name),
    UNIQUE (md5, filename, tag_name)
);

CREATE TABLE bmedia_schema.tag_category_join(
    tag_name VARCHAR(255),
    tag_category_name VARCHAR(255),
    FOREIGN KEY (tag_name) REFERENCES bmedia_schema.tags(tag_name),
    FOREIGN KEY (tag_category_name) REFERENCES bmedia_schema.tag_categories(tag_category_name),
    UNIQUE (tag_name, tag_category_name)
);

-- Create other image tables
CREATE TABLE bmedia_schema.nsfw_images (LIKE bmedia_schema.art INCLUDING ALL);
CREATE TABLE bmedia_schema.nsfw_images_tags_join (LIKE bmedia_schema.art_tags_join INCLUDING ALL);
CREATE INDEX nsfw_images_index ON bmedia_schema.nsfw_images(resolution_width, resolution_height, file_size_bytes);

CREATE TABLE bmedia_schema.memes (LIKE bmedia_schema.art INCLUDING ALL);
CREATE TABLE bmedia_schema.memes_tags_join (LIKE bmedia_schema.art_tags_join INCLUDING ALL);
CREATE INDEX memes_index ON bmedia_schema.memes(resolution_width, resolution_height, file_size_bytes);

-- Set permissions
GRANT CREATE, CONNECT, TEMPORARY ON DATABASE bmedia TO bmedia_admin;
GRANT CONNECT ON DATABASE bmedia TO bmedia_user;

GRANT CREATE, USAGE ON SCHEMA bmedia_schema TO bmedia_admin;

GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON ALL TABLES IN SCHEMA bmedia_schema TO bmedia_admin;
GRANT SELECT ON ALL TABLES IN SCHEMA bmedia_schema TO bmedia_admin;