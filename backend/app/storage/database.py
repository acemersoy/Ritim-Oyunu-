import os
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase
from sqlalchemy import Column, String, Integer, Float, Text, DateTime, Enum as SAEnum
import enum
from datetime import datetime, timezone


DB_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "rhythm_game.db")
DATABASE_URL = f"sqlite+aiosqlite:///{DB_PATH}"

engine = create_async_engine(DATABASE_URL, echo=False)
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


async def init_db():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)


async def get_session() -> AsyncSession:
    async with async_session() as session:
        yield session
