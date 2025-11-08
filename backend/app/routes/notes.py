from fastapi import APIRouter, File, UploadFile, HTTPException, Query
from fastapi.responses import JSONResponse, FileResponse
from typing import Optional, List
from pydantic import BaseModel
import os
import json
from datetime import datetime
import shutil
import uuid

router = APIRouter(prefix="/notes", tags=["notes"])

# Base path for notes
NOTES_BASE_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "notes")
METADATA_FILE = os.path.join(NOTES_BASE_PATH, "metadata.json")


class NoteMetadata(BaseModel):
    id: str
    name: str
    subject: str
    year: Optional[str] = None
    uploaded_by: Optional[str] = None
    upload_date: str
    file_size: int  # in bytes
    file_type: str
    file_path: str
    description: Optional[str] = None
    tags: Optional[List[str]] = []


class NoteUploadRequest(BaseModel):
    name: str
    subject: str
    year: Optional[str] = None
    uploaded_by: Optional[str] = None
    description: Optional[str] = None
    tags: Optional[List[str]] = []


def load_metadata():
    """Load metadata from JSON file."""
    if not os.path.exists(METADATA_FILE):
        return {}
    try:
        with open(METADATA_FILE, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception as e:
        print(f"Error loading metadata: {e}")
        return {}


def save_metadata(metadata):
    """Save metadata to JSON file."""
    try:
        with open(METADATA_FILE, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)
    except Exception as e:
        print(f"Error saving metadata: {e}")
        raise HTTPException(status_code=500, detail=f"Error saving metadata: {str(e)}")


def scan_existing_notes():
    """Scan existing notes folder and create metadata for files without it."""
    metadata = load_metadata()
    
    # Subject folders
    subjects = ['ai', 'ivp', 'se']
    
    for subject in subjects:
        subject_path = os.path.join(NOTES_BASE_PATH, subject)
        if not os.path.exists(subject_path):
            continue
        
        for filename in os.listdir(subject_path):
            file_path = os.path.join(subject_path, filename)
            if not os.path.isfile(file_path):
                continue
            
            # Create a unique ID based on subject and filename
            file_id = f"{subject}_{filename.replace(' ', '_').replace('.', '_')}"
            
            # If this file is not in metadata, add it
            if file_id not in metadata:
                file_size = os.path.getsize(file_path)
                file_ext = os.path.splitext(filename)[1]
                
                metadata[file_id] = {
                    "id": file_id,
                    "name": filename,
                    "subject": subject.upper(),
                    "year": None,
                    "uploaded_by": "system",
                    "upload_date": datetime.fromtimestamp(os.path.getctime(file_path)).isoformat(),
                    "file_size": file_size,
                    "file_type": file_ext,
                    "file_path": f"{subject}/{filename}",
                    "description": None,
                    "tags": []
                }
    
    save_metadata(metadata)
    return metadata


@router.get("/")
async def get_all_notes(
    subject: Optional[str] = Query(None, description="Filter by subject (ai, ivp, se)"),
    year: Optional[str] = Query(None, description="Filter by year"),
    uploaded_by: Optional[str] = Query(None, description="Filter by uploader"),
    search: Optional[str] = Query(None, description="Search in name and description"),
    tags: Optional[str] = Query(None, description="Filter by tags (comma-separated)"),
):
    """Get all notes with optional filtering."""
    metadata = scan_existing_notes()
    
    notes = list(metadata.values())
    
    # Apply filters
    if subject:
        notes = [n for n in notes if n.get("subject", "").lower() == subject.lower()]
    
    if year:
        notes = [n for n in notes if n.get("year") == year]
    
    if uploaded_by:
        notes = [n for n in notes if n.get("uploaded_by", "").lower() == uploaded_by.lower()]
    
    if search:
        search_lower = search.lower()
        notes = [n for n in notes if 
                 search_lower in n.get("name", "").lower() or 
                 search_lower in n.get("description", "").lower()]
    
    if tags:
        tag_list = [t.strip().lower() for t in tags.split(",")]
        notes = [n for n in notes if 
                 any(tag in [t.lower() for t in n.get("tags", [])] for tag in tag_list)]
    
    # Sort by upload date (newest first)
    notes.sort(key=lambda x: x.get("upload_date", ""), reverse=True)
    
    return JSONResponse(content={
        "success": True,
        "count": len(notes),
        "notes": notes
    })


@router.get("/{note_id}")
async def get_note_by_id(note_id: str):
    """Get a specific note by ID."""
    metadata = load_metadata()
    
    if note_id not in metadata:
        raise HTTPException(status_code=404, detail="Note not found")
    
    return JSONResponse(content={
        "success": True,
        "note": metadata[note_id]
    })


@router.get("/view/{note_id}")
async def view_note(note_id: str):
    """View a note file directly in the browser."""
    metadata = load_metadata()
    
    if note_id not in metadata:
        raise HTTPException(status_code=404, detail="Note not found")
    
    note = metadata[note_id]
    file_path = os.path.join(NOTES_BASE_PATH, note["file_path"])
    
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File not found on server")
    
    # Determine the correct media type based on file extension
    file_ext = note["file_type"].lower()
    media_type_map = {
        ".pdf": "application/pdf",
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png": "image/png",
        ".gif": "image/gif",
        ".txt": "text/plain",
        ".md": "text/markdown",
    }
    media_type = media_type_map.get(file_ext, "application/octet-stream")
    
    return FileResponse(
        path=file_path,
        filename=note["name"],
        media_type=media_type,
        headers={"Content-Disposition": f"inline; filename=\"{note['name']}\""}
    )


@router.get("/download/{note_id}")
async def download_note(note_id: str):
    """Download a specific note file."""
    metadata = load_metadata()
    
    if note_id not in metadata:
        raise HTTPException(status_code=404, detail="Note not found")
    
    note = metadata[note_id]
    file_path = os.path.join(NOTES_BASE_PATH, note["file_path"])
    
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File not found on server")
    # Set media type according to file extension so browsers can open PDFs/images directly
    _, ext = os.path.splitext(file_path)
    ext = ext.lower()
    if ext == '.pdf':
        media_type = 'application/pdf'
    elif ext in ('.jpg', '.jpeg'):
        media_type = 'image/jpeg'
    elif ext == '.png':
        media_type = 'image/png'
    else:
        media_type = 'application/octet-stream'

    return FileResponse(
        path=file_path,
        filename=note["name"],
        media_type=media_type
    )


@router.post("/upload")
async def upload_note(
    file: UploadFile = File(...),
    name: Optional[str] = None,
    subject: str = Query(..., description="Subject (ai, ivp, se)"),
    year: Optional[str] = None,
    uploaded_by: Optional[str] = None,
    description: Optional[str] = None,
    tags: Optional[str] = Query(None, description="Comma-separated tags")
):
    """Upload a new note with metadata."""
    # Validate subject
    valid_subjects = ['ai', 'ivp', 'se']
    if subject.lower() not in valid_subjects:
        raise HTTPException(status_code=400, detail=f"Invalid subject. Must be one of: {', '.join(valid_subjects)}")
    
    # Validate file type (allow PDF primarily)
    allowed_types = ["application/pdf", "image/jpeg", "image/jpg", "image/png"]
    if file.content_type not in allowed_types:
        raise HTTPException(status_code=400, detail=f"Invalid file type. Allowed types: PDF, JPEG, PNG")
    
    try:
        # Use original filename if name not provided
        file_name = name if name else file.filename

        # Sanitize filename to avoid path traversal and keep basename only
        file_name = os.path.basename(file_name)

        # Ensure subject folder exists
        subject_path = os.path.join(NOTES_BASE_PATH, subject.lower())
        os.makedirs(subject_path, exist_ok=True)

        # Ensure file has an extension; keep original extension if user-provided name omitted it
        original_ext = os.path.splitext(file.filename)[1]
        provided_ext = os.path.splitext(file_name)[1]
        if not provided_ext and original_ext:
            file_name = f"{file_name}{original_ext}"

        # Avoid filename collisions by appending a short uuid if file exists
        file_path = os.path.join(subject_path, file_name)
        if os.path.exists(file_path):
            base, ext = os.path.splitext(file_name)
            file_name = f"{base}_{uuid.uuid4().hex[:8]}{ext}"
            file_path = os.path.join(subject_path, file_name)

        # Read full uploaded file bytes and write in binary mode
        contents = await file.read()
        with open(file_path, "wb") as buffer:
            buffer.write(contents)
        await file.close()

        # Get file info
        file_size = os.path.getsize(file_path)
        file_ext = os.path.splitext(file_name)[1]

        # Create metadata entry
        file_id = f"{subject.lower()}_{file_name.replace(' ', '_').replace('.', '_')}"

        metadata = load_metadata()
        tag_list = [t.strip() for t in tags.split(",")] if tags else []

        metadata[file_id] = {
            "id": file_id,
            "name": file_name,
            "subject": subject.upper(),
            "year": year,
            "uploaded_by": uploaded_by or "anonymous",
            "upload_date": datetime.now().isoformat(),
            "file_size": file_size,
            "file_type": file_ext,
            "file_path": f"{subject.lower()}/{file_name}",
            "description": description,
            "tags": tag_list
        }

        save_metadata(metadata)

        return JSONResponse(content={
            "success": True,
            "message": "Note uploaded successfully",
            "note": metadata[file_id]
        })

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error uploading note: {str(e)}")


@router.put("/{note_id}/metadata")
async def update_note_metadata(
    note_id: str,
    name: Optional[str] = None,
    year: Optional[str] = None,
    uploaded_by: Optional[str] = None,
    description: Optional[str] = None,
    tags: Optional[str] = Query(None, description="Comma-separated tags")
):
    """Update metadata for a note."""
    metadata = load_metadata()
    
    if note_id not in metadata:
        raise HTTPException(status_code=404, detail="Note not found")
    
    # Update fields if provided
    if name:
        metadata[note_id]["name"] = name
    if year:
        metadata[note_id]["year"] = year
    if uploaded_by:
        metadata[note_id]["uploaded_by"] = uploaded_by
    if description:
        metadata[note_id]["description"] = description
    if tags is not None:
        tag_list = [t.strip() for t in tags.split(",")] if tags else []
        metadata[note_id]["tags"] = tag_list
    
    save_metadata(metadata)
    
    return JSONResponse(content={
        "success": True,
        "message": "Metadata updated successfully",
        "note": metadata[note_id]
    })


@router.delete("/{note_id}")
async def delete_note(note_id: str):
    """Delete a note and its metadata."""
    metadata = load_metadata()
    
    if note_id not in metadata:
        raise HTTPException(status_code=404, detail="Note not found")
    
    note = metadata[note_id]
    file_path = os.path.join(NOTES_BASE_PATH, note["file_path"])
    
    try:
        # Delete file if it exists
        if os.path.exists(file_path):
            os.remove(file_path)
        
        # Remove from metadata
        del metadata[note_id]
        save_metadata(metadata)
        
        return JSONResponse(content={
            "success": True,
            "message": "Note deleted successfully"
        })
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting note: {str(e)}")


@router.get("/subjects/list")
async def get_subjects():
    """Get list of all subjects with note counts."""
    metadata = load_metadata()
    
    subjects = {}
    for note in metadata.values():
        subject = note.get("subject", "UNKNOWN")
        if subject not in subjects:
            subjects[subject] = 0
        subjects[subject] += 1
    
    return JSONResponse(content={
        "success": True,
        "subjects": [{"name": k, "count": v} for k, v in subjects.items()]
    })


@router.post("/scan")
async def scan_notes_folder():
    """Manually trigger a scan of the notes folder to update metadata."""
    metadata = scan_existing_notes()
    
    return JSONResponse(content={
        "success": True,
        "message": "Notes folder scanned successfully",
        "total_notes": len(metadata)
    })
