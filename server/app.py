"""Single-user JayMinder transcription gateway. Run one uvicorn worker."""
import asyncio
import hashlib
import hmac
import io
import os
import sqlite3
import time
import wave
from contextlib import asynccontextmanager
from pathlib import Path

import httpx
from fastapi import FastAPI, HTTPException, Request

MAX_BYTES = 24 * 1024 * 1024
MAX_SECONDS = 600


def validate_wav(data: bytes) -> None:
    try:
        with wave.open(io.BytesIO(data), 'rb') as wav:
            if wav.getcomptype() != 'NONE' or wav.getnchannels() not in (1, 2):
                raise ValueError()
            if wav.getsampwidth() != 2 or wav.getframerate() not in (16000, 22050, 24000, 44100, 48000):
                raise ValueError()
            if not 0 < wav.getnframes() / wav.getframerate() <= MAX_SECONDS:
                raise ValueError()
            expected = wav.getnframes() * wav.getnchannels() * wav.getsampwidth()
            if len(wav.readframes(wav.getnframes())) != expected:
                raise ValueError()
    except (wave.Error, EOFError, ValueError):
        raise HTTPException(400, 'Use a valid PCM16 WAV recording of at most 10 minutes.')


def create_app(*, api_key=None, app_token=None, db_path=None, transport=None):
    api_key = api_key if api_key is not None else os.getenv('OPENAI_API_KEY', '')
    app_token = app_token if app_token is not None else os.getenv('JAYMINDER_APP_TOKEN', '')
    model = os.getenv('OPENAI_TRANSCRIPTION_MODEL', 'gpt-transcribe')
    daily_limit = int(os.getenv('DAILY_REQUEST_LIMIT', '60'))
    db_path = db_path or os.getenv('CACHE_DB', '/data/transcriptions.sqlite3')
    lock = asyncio.Lock()

    @asynccontextmanager
    async def lifespan(app):
        if not api_key or len(app_token) < 32:
            raise RuntimeError('Set OPENAI_API_KEY and a random JAYMINDER_APP_TOKEN of at least 32 characters.')
        if daily_limit < 1:
            raise RuntimeError('DAILY_REQUEST_LIMIT must be positive')
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        db = sqlite3.connect(db_path)
        db.execute('CREATE TABLE IF NOT EXISTS cache (key TEXT PRIMARY KEY, text TEXT NOT NULL, created REAL NOT NULL)')
        db.execute('CREATE TABLE IF NOT EXISTS budget (day TEXT PRIMARY KEY, requests INTEGER NOT NULL)')
        db.commit()
        app.state.db = db
        async with httpx.AsyncClient(timeout=120, transport=transport) as client:
            app.state.client = client
            yield
        db.close()

    app = FastAPI(lifespan=lifespan, docs_url=None, redoc_url=None)

    @app.get('/health')
    async def health():
        return {'status': 'ok'}

    @app.post('/v1/transcribe')
    async def transcribe(request: Request):
        if not hmac.compare_digest(request.headers.get('authorization', '').encode(), ('Bearer ' + app_token).encode()):
            raise HTTPException(401, 'Unauthorized')
        if request.headers.get('content-type', '').split(';')[0].strip() != 'audio/wav':
            raise HTTPException(415, 'Expected audio/wav')
        # Bound streamed bodies even when Content-Length is absent or dishonest.
        body = bytearray()
        async for chunk in request.stream():
            if len(body) + len(chunk) > MAX_BYTES:
                raise HTTPException(413, 'Recording too large')
            body.extend(chunk)
        audio = bytes(body)
        validate_wav(audio)
        digest = hashlib.sha256(model.encode() + b'\0' + audio).hexdigest()
        # Single worker serializes duplicate requests and budget reservations.
        async with lock:
            db = app.state.db
            now = time.time()
            db.execute('DELETE FROM cache WHERE created < ?', (now - 30 * 86400,))
            row = db.execute('SELECT text FROM cache WHERE key = ?', (digest,)).fetchone()
            if row:
                db.commit()
                return {'text': row[0], 'cached': True}
            day = time.strftime('%Y-%m-%d', time.gmtime(now))
            db.execute('INSERT OR IGNORE INTO budget VALUES (?, 0)', (day,))
            used = db.execute('SELECT requests FROM budget WHERE day = ?', (day,)).fetchone()[0]
            if used >= daily_limit:
                db.commit()
                raise HTTPException(429, 'Daily transcription request limit reached')
            # Count attempts too: repeated upstream failures must not spend without bound.
            db.execute('UPDATE budget SET requests = requests + 1 WHERE day = ?', (day,))
            db.commit()
            try:
                response = await app.state.client.post(
                    'https://api.openai.com/v1/audio/transcriptions',
                    headers={'Authorization': 'Bearer ' + api_key},
                    data={'model': model, 'prompt': 'گفتار فارسی؛ نام‌ها، اعداد و تاریخ‌ها را دقیق ثبت کن. متن را خلاصه نکن.'},
                    files={'file': ('recording.wav', audio, 'audio/wav')},
                )
                response.raise_for_status()
                text = response.json().get('text', '')
                if not isinstance(text, str) or not text.strip():
                    raise ValueError('Empty transcript')
            except (httpx.HTTPError, ValueError):
                # Never return upstream bodies, credentials or recorded content in errors.
                raise HTTPException(502, 'Transcription provider unavailable') from None
            db.execute('INSERT OR REPLACE INTO cache VALUES (?, ?, ?)', (digest, text.strip(), now))
            db.commit()
            return {'text': text.strip(), 'cached': False}

    return app

app = create_app()
