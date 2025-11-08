# TEMPUS — MAD Project

This repository contains two main components:

- `backend/` — a FastAPI-based backend providing OCR, crowd-counting, floor-plan and notes management endpoints. It uses Mistral AI for OCR and Ultralytics YOLO for crowd analysis.
- `temp2/` — an Android app (Gradle) that integrates with the backend.

This README explains project structure, how to run the backend locally, and how to build/run the Android app on Windows (PowerShell).

## Repository structure (key parts)

- `backend/`
  - `app/` — FastAPI app code
    - `main.py` — FastAPI application entrypoint
    - `routes/` — API routers: `ocr.py`, `crowd.py`, `floor.py`, `notes.py`
    - `utils/` — helpers: `mistral_client.py`, `ocr_utils.py`, etc.
  - `requirements.txt` — Python requirements
  - `yolov8n.pt` — YOLO model used by crowd counting (small model shipped here)

- `temp2/` — Android project (Gradle). Open in Android Studio or build with Gradle wrapper.

## Backend (FastAPI)

### Overview

The backend exposes several grouped routes:

- `/` — health/status endpoint
- `/ocr/*` — image/PDF OCR and timetable extraction endpoints
  - `POST /ocr/extract-timetable` — upload image/PDF, returns parsed timetable JSON
  - `POST /ocr/extract-raw` — returns raw markdown from OCR
  - `POST /ocr/extract-url` — process document at a URL
- `/crowd/*` — crowd counting
  - `POST /crowd/count` — upload video (mp4/avi/mov), returns crowd counts and summary
  - Uses Ultralytics YOLO with `yolov8n.pt` by default (file present at `backend/yolov8n.pt`)
- `/floor/*` — floor plan image listing and serving
  - `GET /floor/` — list all floors/rooms (scans `backend/app/floor/`)
  - `GET /floor/{floor_number}` — list rooms on a floor
  - `GET /floor/image/{floor_number}/{filename}` — serve a floor PNG
  - `POST /floor/scan` — trigger rescan to rebuild metadata
- `/notes/*` — notes (lecture notes) management
  - listing, view, download, upload, metadata update, delete and scan endpoints

### Prerequisites

- Python 3.11+ recommended
- Git (to clone)
- On Windows: install the MSVC build tools if you need to build native wheels (optional)

### Install and run (Windows PowerShell)

1. Open PowerShell and create a virtual environment (recommended):

```powershell
cd \path\to\repository\root
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

2. Install dependencies:

```powershell
pip install --upgrade pip
pip install -r requirements.txt
```

3. Environment variables

- The Mistral client reads API key from the `MISTRAL_API_KEY` environment variable. The code provides a default key in `app/utils/mistral_client.py`, but you should set your own key in production:

```powershell
$env:MISTRAL_API_KEY = "your_real_mistral_api_key_here"
```

4. Run the server

Option A — recommended (uvicorn directly):

```powershell
# from backend/ directory
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Option B — run the script (main.py will call uvicorn):

```powershell
python app/main.py
```

The API will be available at http://localhost:8000. The autodoc UI is at http://localhost:8000/docs (Swagger) and http://localhost:8000/redoc (ReDoc).

### Notes about the YOLO model and video processing

- `backend/yolov8n.pt` is included for quick local testing. If you update the model or want a different size (e.g. yolov8s.pt), place it in the `backend/` folder and update the path in `app/routes/crowd.py` (currently `YOLO('yolov8n.pt')`).
- Video uploads can be large. The `crowd/count` endpoint saves uploads to a temporary file and deletes them after processing.
- Installing `ultralytics`, `torch`, and `opencv-python` is required (they are in `requirements.txt`). GPU support for PyTorch requires a GPU-enabled wheel and appropriate CUDA toolkit.

### Troubleshooting

- If `ultralytics` or `torch` import fails, ensure you installed the correct wheel for your Python and OS, and consider installing CPU-only or GPU versions following PyTorch's official guide.
- If CORS or connection issues occur when connecting from the Android app, ensure the backend host/port are reachable and CORS is allowed (FastAPI is configured with broad CORS in `main.py`).

## Android app (temp2)

This is a standard Android Gradle project located in `temp2/`.

### Open in Android Studio

- Open Android Studio and select "Open an existing project" then choose the `temp2/` folder.

### Build and run from PowerShell (Windows)

From the repository root (or `temp2`):

```powershell
cd temp2
# Assemble a debug APK
.\gradlew.bat assembleDebug

# Install and run on a connected device/emulator
.\gradlew.bat installDebug
```

Notes:
- Ensure `local.properties` contains `sdk.dir` pointing to your Android SDK, or set it via Android Studio.
- Use Android Studio to run on emulators or configure signing for release builds.

## Useful development tips

- API testing: use curl, Postman, or the built-in Swagger UI at `/docs`.
- When extending or debugging the backend, add log statements and run with `--reload` for fast iteration.
- Keep long-running models or heavy processing isolated (offload to background jobs) if you integrate with a production Android client.

## Security and secrets

- Do not commit API keys into source. Replace or remove any default key in `app/utils/mistral_client.py` and use env variables instead.

## Contributing

- Open issues or PRs in the repository. For backend changes, include tests for new behaviors where possible.

## Acknowledgements

- FastAPI, Uvicorn, Ultralytics YOLO, Mistral AI.

---
