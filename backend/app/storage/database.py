from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase
from sqlalchemy import Column, String, Integer, Float, Text, DateTime
import enum
from datetime import datetime, timezone

from app.config import settings

engine = create_async_engine(settings.database_url, echo=False)
async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)


class Base(DeclarativeBase):
    pass


class SongStatus(str, enum.Enum):
    PROCESSING = "processing"
    READY = "ready"
    ERROR = "error"


class SongRecord(Base):
    __tablename__ = "songs"

    id = Column(String, primary_key=True)
    filename = Column(String, nullable=False)
    file_hash = Column(String, nullable=True, index=True)
    duration_ms = Column(Integer, nullable=True)
    bpm = Column(Float, nullable=True)
    upload_date = Column(DateTime, default=lambda: datetime.now(timezone.utc))
    status = Column(String, default=SongStatus.PROCESSING.value)
    error_message = Column(Text, nullable=True)
    share_token = Column(String, nullable=True, unique=True)


class ChartRecord(Base):
    __tablename__ = "charts"

    id = Column(String, primary_key=True)
    song_id = Column(String, nullable=False)
    difficulty = Column(String, nullable=False)
    notes_json = Column(Text, nullable=False)


class UserRecord(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True)  # Android Device ID or UUID
    username = Column(String, nullable=False)
    level = Column(Integer, default=1)
    total_xp = Column(Integer, default=0)
    league = Column(String, default="Bronz")
    highest_score = Column(Integer, default=0)
    last_sync = Column(DateTime, default=lambda: datetime.now(timezone.utc))


class ScoreRecord(Base):
    __tablename__ = "user_scores"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String, nullable=False)
    song_id = Column(String, nullable=False)
    score = Column(Integer, nullable=False)
    accuracy = Column(Float, nullable=False)
    max_combo = Column(Integer, nullable=False)
    difficulty = Column(String, nullable=False)
    played_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))


async def init_db():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
        # Migrate: add file_hash column if missing (for existing DBs)
        try:
            await conn.execute(
                __import__('sqlalchemy').text(
                    "ALTER TABLE songs ADD COLUMN file_hash TEXT"
                )
            )
        except Exception:
            pass  # Column already exists


async def get_session() -> AsyncSession:
    async with async_session() as session:
        yield session
