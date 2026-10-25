import io
import wave
import httpx
import pytest
from fastapi.testclient import TestClient
from server.app import create_app

TOKEN = 'a' * 40
HEADERS = {'Authorization': 'Bearer ' + TOKEN, 'Content-Type': 'audio/wav'}

def wav():
    out = io.BytesIO()
    with wave.open(out, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(16000)
        w.writeframes(b'\x00\x00' * 1600)
    return out.getvalue()


def make_client(tmp_path, handler):
    return TestClient(create_app(api_key='provider-secret', app_token=TOKEN,
        db_path=str(tmp_path / 'test.sqlite3'), transport=httpx.MockTransport(handler)))


def test_original_audio_and_cache(tmp_path):
    calls = []
    def upstream(request):
        calls.append(request)
        assert request.headers['Authorization'] == 'Bearer provider-secret'
        assert wav() in request.content
        assert b'name="model"' in request.content
        return httpx.Response(200, json={'text': 'فردا ساعت ده تماس بگیر'})
    with make_client(tmp_path, upstream) as client:
        a = client.post('/v1/transcribe', headers=HEADERS, content=wav())
        b = client.post('/v1/transcribe', headers=HEADERS, content=wav())
        assert a.status_code == 200 and a.json()['cached'] is False
        assert b.json()['cached'] is True
        assert a.json()['text'] == b.json()['text']
        assert len(calls) == 1


def test_auth_and_invalid_audio_never_call_provider(tmp_path):
    def upstream(request):
        pytest.fail('must not contact provider')
    with make_client(tmp_path, upstream) as client:
        assert client.post('/v1/transcribe', content=wav()).status_code == 401
        assert client.post('/v1/transcribe', headers=HEADERS, content=b'invalid').status_code == 400
        headers = dict(HEADERS, **{'Content-Type': 'text/plain'})
        assert client.post('/v1/transcribe', headers=headers, content=wav()).status_code == 415


def test_stream_size_limit(tmp_path, monkeypatch):
    monkeypatch.setattr('server.app.MAX_BYTES', 100)
    with make_client(tmp_path, lambda r: pytest.fail('must not contact provider')) as client:
        assert client.post('/v1/transcribe', headers=HEADERS, content=wav()).status_code == 413


def test_provider_error_is_redacted_and_budget_persists(tmp_path, monkeypatch):
    monkeypatch.setenv('DAILY_REQUEST_LIMIT', '1')
    with make_client(tmp_path, lambda r: httpx.Response(401, text='provider-secret')) as client:
        response = client.post('/v1/transcribe', headers=HEADERS, content=wav())
        assert response.status_code == 502
        assert 'provider-secret' not in response.text
    with make_client(tmp_path, lambda r: pytest.fail('budget exhausted')) as client:
        assert client.post('/v1/transcribe', headers=HEADERS, content=wav()).status_code == 429


def test_empty_transcript_not_cached(tmp_path):
    with make_client(tmp_path, lambda r: httpx.Response(200, json={'text': ' '})) as client:
        assert client.post('/v1/transcribe', headers=HEADERS, content=wav()).status_code == 502


def test_missing_credentials_fail_startup(tmp_path):
    with pytest.raises(RuntimeError):
        with TestClient(create_app(api_key='', app_token='', db_path=str(tmp_path / 'db'))):
            pass
