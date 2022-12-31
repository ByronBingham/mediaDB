from enum import Enum

class DBEnum(Enum):
    ART = 'bmedia_schema.art'
    MEMES = 'bmedia_schema.memes'
    MUSIC = 'bmedia_schema.music'
    VIDEOS = 'bmedia_schema.videos'
    MUSIC_PLAYLISTS = 'bmedia_schema.music_playlists'
    NSFW_IMAGES = 'bmedia_schema.nsfw_images'