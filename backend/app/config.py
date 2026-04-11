import os


class Settings:
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "8000"))
    CORS_ORIGINS: str = os.getenv("CORS_ORIGINS", "*")
    UPLOAD_DIR: str = os.getenv("UPLOAD_DIR", os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads"))
    WAV_DIR: str = os.getenv("WAV_DIR", os.getenv("UPLOAD_DIR", os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads")))
    DB_PATH: str = os.getenv("DB_PATH", os.path.join(os.path.dirname(os.path.dirname(__file__)), "rhythm_game.db"))
    MAX_UPLOAD_MB: int = int(os.getenv("MAX_UPLOAD_MB", "200"))

    @property
    def database_url(self) -> str:
        return f"sqlite+aiosqlite:///{self.DB_PATH}"

    @property
    def max_upload_bytes(self) -> int:
        return self.MAX_UPLOAD_MB * 1024 * 1024

    @property
    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.CORS_ORIGINS.split(",")]


settings = Settings()
