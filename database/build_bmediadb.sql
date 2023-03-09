-- Create Users
CREATE USER bmedia_admin WITH PASSWORD 'changeme';
CREATE USER bmedia_user WITH PASSWORD 'changeme';

-- Create Database
CREATE DATABASE bmediadb OWNER bmedia_admin;
\c bmediadb;

-- Create Schema
CREATE SCHEMA bmedia_schema AUTHORIZATION bmedia_admin;

-- Create Static Tables
CREATE TABLE bmedia_schema.tags(tag_name VARCHAR(255),nsfw BOOLEAN,PRIMARY KEY (tag_name),UNIQUE (tag_name));

-- Create User-specified Tables
CREATE TABLE bmedia_schema.art(id BIGSERIAL UNIQUE,md5 VARCHAR(32),filename VARCHAR(255),file_path VARCHAR(255) UNIQUE,resolution_width INTEGER,resolution_height INTEGER,file_size_bytes INTEGER,date_added TIMESTAMP NOT NULL DEFAULT NOW(),PRIMARY KEY (id));
CREATE INDEX art_index ON bmedia_schema.art(resolution_width, resolution_height, file_size_bytes);
CREATE TABLE bmedia_schema.art_tags_join(id BIGINT,tag_name VARCHAR(255),FOREIGN KEY (id) REFERENCES bmedia_schema.art(id),FOREIGN KEY (tag_name) REFERENCES bmedia_schema.tags(tag_name),UNIQUE (id, tag_name));

CREATE TABLE bmedia_schema.nsfw_images(id BIGSERIAL UNIQUE,md5 VARCHAR(32),filename VARCHAR(255),file_path VARCHAR(255) UNIQUE,resolution_width INTEGER,resolution_height INTEGER,file_size_bytes INTEGER,date_added TIMESTAMP NOT NULL DEFAULT NOW(),PRIMARY KEY (id));
CREATE INDEX nsfw_images_index ON bmedia_schema.nsfw_images(resolution_width, resolution_height, file_size_bytes);
CREATE TABLE bmedia_schema.nsfw_images_tags_join(id BIGINT,tag_name VARCHAR(255),FOREIGN KEY (id) REFERENCES bmedia_schema.nsfw_images(id),FOREIGN KEY (tag_name) REFERENCES bmedia_schema.tags(tag_name),UNIQUE (id, tag_name));

GRANT CREATE, CONNECT, TEMPORARY ON DATABASE bmediadb TO bmedia_admin;
GRANT CONNECT ON DATABASE bmediadb  TO bmedia_user;
GRANT CREATE, USAGE ON SCHEMA bmedia_schema TO bmedia_admin;
GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON ALL TABLES IN SCHEMA bmedia_schema TO bmedia_admin;
GRANT SELECT ON ALL TABLES IN SCHEMA bmedia_schema TO bmedia_admin;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA bmedia_schema TO bmedia_admin;