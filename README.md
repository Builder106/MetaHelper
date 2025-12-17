# MetaHelper

An AI-powered companion for Meta Ray-Ban glasses that provides audio descriptions and answers via Gemini Vision.

## Project Structure

- `backend/`: FastAPI server for image processing, LLM integration, and TTS.
- `android/`: Android application using Meta Wearables SDK.

## Setup (Backend)

1. `cd backend`
2. `pip install -r requirements.txt`
3. Create a `.env` file with `GOOGLE_API_KEY`.
4. `uvicorn app.main:app --reload`

## Self-Hosting

The backend is dockerized. Run:
```bash
docker build -t metahelper-backend ./backend
docker run -p 8000:8000 metahelper-backend
```

