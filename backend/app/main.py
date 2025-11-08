from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from app.routes.ocr import router as ocr_router
from app.routes.crowd import router as crowd_router
from app.routes.notes import router as notes_router
from app.routes.floor import router as floor_router


app = FastAPI(
    title="OCR Timetable Extraction API",
    description="Extract timetable data from images and PDFs using Mistral AI OCR",
    version="1.0.0",
)

# Configure CORS to allow all requests
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allow all origins
    allow_credentials=True,
    allow_methods=["*"],  # Allow all methods (GET, POST, PUT, DELETE, etc.)
    allow_headers=["*"],  # Allow all headers
)


@app.get("/")
async def root():
    return {"status": "online", "message": "OCR Timetable Extraction API", "version": "1.0.0"}


# Include routers
app.include_router(ocr_router)
app.include_router(crowd_router)
app.include_router(notes_router)
app.include_router(floor_router)


if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)