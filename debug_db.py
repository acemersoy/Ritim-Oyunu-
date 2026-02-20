import asyncio
import os
import sys

# Add backend to path
sys.path.append(os.path.join(os.getcwd(), "backend"))

from sqlalchemy import select
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from app.storage.database import SongRecord, ChartRecord

# Setup DB connection
DB_PATH = os.path.join(os.getcwd(), "backend", "rhythm_game.db")
DATABASE_URL = f"sqlite+aiosqlite:///{DB_PATH}"

print(f"Connecting to DB at: {DB_PATH}")

engine = create_async_engine(DATABASE_URL, echo=False)
async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

TARGET_SONG_ID = "43610b0d-b59a-4a7d-af98-82d0c4852b5f"

async def check_db():
    async with async_session() as session:
        # Check Song
        stmt = select(SongRecord).where(SongRecord.id == TARGET_SONG_ID)
        result = await session.execute(stmt)
        song = result.scalar_one_or_none()
        
        if not song:
            print("Song NOT found.")
            return

        print(f"Song Found: {song.filename}")
        print(f"Status: {song.status}")
        print(f"ID: {song.id}")

        # Check Charts
        stmt = select(ChartRecord).where(ChartRecord.song_id == TARGET_SONG_ID)
        result = await session.execute(stmt)
        charts = result.scalars().all()

        print(f"Found {len(charts)} charts.")
        for c in charts:
            print(f" - Difficulty: {c.difficulty} (ID: {c.id})")

if __name__ == "__main__":
    asyncio.run(check_db())
