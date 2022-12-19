from dataIngester.ingester import DBEnum


class ProcessingGroup:
    def __init__(self, name: str, source_dirs: list, target_db: DBEnum, valid_extensions: list, db_host: str, db_user: str, db_password: str, auto_tag: bool = False, jfif_webm_to_jpg: bool = False) -> None:
        self.name = name
        self.source_dirs = source_dirs
        self.target_db = target_db
        self.auto_tag = auto_tag
        self.jfif_webm_to_jpg = jfif_webm_to_jpg
        self.valid_extensions = valid_extensions
        self.db_host = db_host
        self.db_user = db_user
        self.db_password = db_password
