import json
import os
import uuid
import asyncio
from concurrent.futures import ThreadPoolExecutor
from fastapi import APIRouter, UploadFile, File, Depends, HTTPException, Query, Request
from fastapi.responses import FileResponse
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from sqlalchemy import delete as sql_delete
from app.api.schemas import (
    UploadResponse, StatusResponse, ChartResponse, NoteResponse,
    ShareRequest, ShareResponse, DifficultyEnum, SongListItem, DeleteResponse,
)
from app.storage.database import get_session, SongRecord, ChartRecord, SongStatus
from app.storage.file_manager import get_upload_path, get_wav_path, generate_song_id, cleanup_files
from app.analysis.pipeline import analyze_audio

router = APIRouter()
executor = ThreadPoolExecutor(max_workers=2)


@router.post("/upload", response_model=UploadResponse)
async def upload_song(
    file: UploadFile = File(...),
    session: AsyncSession = Depends(get_session),
):
    """Upload an audio file and start analysis."""
    allowed_extensions = {".mp3", ".wav", ".ogg", ".flac", ".m4a"}
    filename = file.filename or "unknown.mp3"
    ext = "." + filename.rsplit(".", 1)[-1].lower() if "." in filename else ""
    if ext not in allowed_extensions:
        raise HTTPException(400, f"Unsupported file format. Allowed: {allowed_extensions}")

    song_id = generate_song_id()
    upload_path = get_upload_path(song_id, filename)

    # Save uploaded file
    content = await file.read()
    with open(upload_path, "wb") as f:
        f.write(content)

    # Create database record
    song = SongRecord(id=song_id, filename=filename, status=SongStatus.PROCESSING.value)
    session.add(song)
    await session.commit()

    # Run analysis in background
    asyncio.get_event_loop().run_in_executor(
        executor,
        _run_analysis_sync,
        song_id, filename, upload_path,
    )

    return UploadResponse(
        song_id=song_id,
        status="processing",
        message="File uploaded. Analysis started.",
    )


def _run_analysis_sync(song_id: str, filename: str, upload_path: str):
    """Run analysis synchronously (called from thread pool)."""
    import asyncio

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    try:
        loop.run_until_complete(_run_analysis(song_id, filename, upload_path))
    finally:
        loop.close()


async def _run_analysis(song_id: str, filename: str, upload_path: str):
    """Run audio analysis and store results."""
    from app.storage.database import async_session

    async with async_session() as session:
        try:
            wav_path = get_wav_path(song_id)
            result = analyze_audio(upload_path, wav_path, song_id, filename)

            # Update song record
            stmt = select(SongRecord).where(SongRecord.id == song_id)
            db_result = await session.execute(stmt)
            song = db_result.scalar_one()
            song.status = SongStatus.READY.value
            song.duration_ms = result["duration_ms"]
            song.bpm = result["bpm"]

            # Store charts for each difficulty
            for diff_name, chart_data in result["charts"].items():
                chart_record = ChartRecord(
                    id=str(uuid.uuid4()),
                    song_id=song_id,
                    difficulty=diff_name,
                    notes_json=json.dumps(chart_data),
                )
                session.add(chart_record)

            await session.commit()

        except Exception as e:
            stmt = select(SongRecord).where(SongRecord.id == song_id)
            db_result = await session.execute(stmt)
            song = db_result.scalar_one_or_none()
            if song:
                song.status = SongStatus.ERROR.value
                song.error_message = str(e)
                await session.commit()


@router.get("/status/{song_id}", response_model=StatusResponse)
async def get_status(
    song_id: str,
    session: AsyncSession = Depends(get_session),
):
    """Check analysis status for a song."""
    stmt = select(SongRecord).where(SongRecord.id == song_id)
    result = await session.execute(stmt)
    song = result.scalar_one_or_none()

    if not song:
        raise HTTPException(404, "Song not found")

    return StatusResponse(
        song_id=song.id,
        status=song.status,
        message=song.error_message,
    )


@router.get("/chart/{song_id}", response_model=ChartResponse)
async def get_chart(
    song_id: str,
    difficulty: DifficultyEnum = Query(DifficultyEnum.MEDIUM),
    session: AsyncSession = Depends(get_session),
):
    """Get the note chart for a song at a specific difficulty."""
    # Check song exists and is ready
    stmt = select(SongRecord).where(SongRecord.id == song_id)
    result = await session.execute(stmt)
    song = result.scalar_one_or_none()

    if not song:
        raise HTTPException(404, "Song not found")
    if song.status != SongStatus.READY.value:
        raise HTTPException(400, f"Song is not ready. Status: {song.status}")

    # Get chart
    stmt = select(ChartRecord).where(
        ChartRecord.song_id == song_id,
        ChartRecord.difficulty == difficulty.value,
    )
    result = await session.execute(stmt)
    chart_record = result.scalar_one_or_none()

    if not chart_record:
        raise HTTPException(404, f"Chart not found for difficulty: {difficulty.value}")

    chart_data = json.loads(chart_record.notes_json)
    return ChartResponse(
        song_id=chart_data["song_id"],
        title=chart_data["title"],
        duration_ms=chart_data["duration_ms"],
        bpm=chart_data["bpm"],
        difficulty=chart_data["difficulty"],
        notes=[NoteResponse(**n) for n in chart_data["notes"]],
    )


@router.post("/share", response_model=ShareResponse)
async def create_share_link(
    req: ShareRequest,
    request: Request,
    session: AsyncSession = Depends(get_session),
):
    """Create a shareable link for a song."""
    stmt = select(SongRecord).where(SongRecord.id == req.song_id)
    result = await session.execute(stmt)
    song = result.scalar_one_or_none()

    if not song:
        raise HTTPException(404, "Song not found")

    if not song.share_token:
        song.share_token = str(uuid.uuid4())[:8]
        await session.commit()

    base_url = str(request.base_url).rstrip("/")
    share_url = f"{base_url}/api/share/{song.share_token}"

    return ShareResponse(share_url=share_url, share_token=song.share_token)


@router.get("/share/{token}", response_model=ChartResponse)
async def get_shared_chart(
    token: str,
    difficulty: DifficultyEnum = Query(DifficultyEnum.MEDIUM),
    session: AsyncSession = Depends(get_session),
):
    """Access a shared chart via share token."""
    stmt = select(SongRecord).where(SongRecord.share_token == token)
    result = await session.execute(stmt)
    song = result.scalar_one_or_none()

    if not song:
        raise HTTPException(404, "Shared chart not found")

    return await get_chart(song.id, difficulty, session)


@router.get("/songs", response_model=list[SongListItem])
async def list_songs(session: AsyncSession = Depends(get_session)):
    """List all uploaded songs."""
    stmt = select(SongRecord).order_by(SongRecord.upload_date.desc())
    result = await session.execute(stmt)
    songs = result.scalars().all()

    return [
        SongListItem(
            song_id=s.id,
            filename=s.filename,
            status=s.status,
            bpm=s.bpm,
            duration_ms=s.duration_ms,
        )
        for s in songs
    ]


@router.get("/audio/{song_id}")
async def get_audio(
    song_id: str,
    session: AsyncSession = Depends(get_session),
):
    """Stream the original audio file for playback during gameplay."""
    stmt = select(SongRecord).where(SongRecord.id == song_id)
    result = await session.execute(stmt)
    song = result.scalar_one_or_none()

    if not song:
        raise HTTPException(404, "Song not found")

    # Find the uploaded file (could be any supported extension)
    from app.storage.file_manager import UPLOAD_DIR
    for ext in [".mp3", ".m4a", ".wav", ".ogg", ".flac", ".aac"]:
        file_path = os.path.join(UPLOAD_DIR, f"{song_id}{ext}")
        if os.path.exists(file_path):
            media_types = {
                ".mp3": "audio/mpeg",
                ".m4a": "audio/mp4",
                ".wav": "audio/wav",
                ".ogg": "audio/ogg",
                ".flac": "audio/flac",
                ".aac": "audio/aac",
            }
            return FileResponse(
                file_path,
                media_type=media_types.get(ext, "application/octet-stream"),
                filename=song.filename,
            )

    raise HTTPException(404, "Audio file not found")


@router.delete("/songs/{song_id}", response_model=DeleteResponse)
async def delete_song(
    song_id: str,
    session: AsyncSession = Depends(get_session),
):
    """Delete a song and all its associated data."""
    stmt = select(SongRecord).where(SongRecord.id == song_id)
    result = await session.execute(stmt)
    song = result.scalar_one_or_none()

    if not song:
        raise HTTPException(404, "Song not found")

    # Delete charts
    await session.execute(
        sql_delete(ChartRecord).where(ChartRecord.song_id == song_id)
    )

    # Delete song record
    await session.delete(song)
    await session.commit()

    # Cleanup files on disk
    cleanup_files(song_id)

    return DeleteResponse(message="Song deleted")
