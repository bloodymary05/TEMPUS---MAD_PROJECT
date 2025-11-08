"""Routes package for the FastAPI app."""

from .ocr import router as ocr_router
from .crowd import router as crowd_router
from .notes import router as notes_router

__all__ = ["ocr_router", "crowd_router", "notes_router"]
