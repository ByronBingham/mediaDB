from dataIngester.ingester import DBEnum


class ProcessingGroup:
    def __init__(self, name: str, source_dirs: list, target_db: DBEnum, auto_tag: bool = False, jfif_to_jpg: bool = False) -> None:
        self.name = name
        self.source_dirs = source_dirs
        self.target_db = target_db
        self.auto_tag = auto_tag
        self.jfif_to_jpg = jfif_to_jpg
