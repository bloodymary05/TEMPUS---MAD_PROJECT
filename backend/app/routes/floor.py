from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import JSONResponse, FileResponse
from typing import Optional, List
from pydantic import BaseModel
import os
import json
from datetime import datetime

router = APIRouter(prefix="/floor", tags=["floor"])

# Base path for floor maps
FLOOR_BASE_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "floor")
METADATA_FILE = os.path.join(FLOOR_BASE_PATH, "metadata.json")


class FloorRoomMetadata(BaseModel):
    id: str
    floor: str
    room_number: str
    room_type: str  # cr (classroom), tr (tutorial room), cc (computer lab), cl (conference/lab)
    image_path: str
    image_url: str
    file_size: int
    last_updated: str


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
        os.makedirs(FLOOR_BASE_PATH, exist_ok=True)
        with open(METADATA_FILE, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)
    except Exception as e:
        print(f"Error saving metadata: {e}")
        raise HTTPException(status_code=500, detail=f"Error saving metadata: {str(e)}")


def get_room_type_full_name(room_type: str) -> str:
    """Get full name for room type abbreviation."""
    room_types = {
        'cr': 'Classroom',
        'tr': 'Tutorial Room',
        'cc': 'Computer Lab',
        'cl': 'Conference/Lab'
    }
    return room_types.get(room_type.lower(), 'Unknown')


def get_image_url(floor_num: str, filename: str, request_base_url: str = "http://localhost:8000") -> str:
    """Generate the image URL for a floor plan."""
    return f"{request_base_url}/floor/image/{floor_num}/{filename}"


def scan_floor_images():
    """Scan existing floor folders and create metadata for all images."""
    metadata = {}
    
    # Check all subdirectories in floor folder
    if not os.path.exists(FLOOR_BASE_PATH):
        return metadata
    
    for floor_num in os.listdir(FLOOR_BASE_PATH):
        floor_path = os.path.join(FLOOR_BASE_PATH, floor_num)
        
        # Skip if not a directory or if it's the metadata file
        if not os.path.isdir(floor_path):
            continue
        
        # Scan all PNG files in this floor
        for filename in os.listdir(floor_path):
            if not filename.endswith('.png'):
                continue
            
            file_path = os.path.join(floor_path, filename)
            
            # Parse room information from filename (e.g., cr301.png, tr302.png)
            room_name = os.path.splitext(filename)[0]  # Remove .png
            
            # For ground floor (g) and lower ground (lg), extract only 'cr' as room type
            if floor_num in ['g', 'lg']:
                room_type = 'cr'  # These are all classrooms
                # For g floor, prefix room numbers with 'g'
                if floor_num == 'g':
                    room_number = 'g' + room_name[3:]  # Skip 'crg' prefix to get number
                else:  # lg floor
                    room_number = 'lg' + room_name[4:]  # Skip 'crlg' prefix to get number
            else:
                # For other floors, extract room type and number normally
                room_type = ''.join([c for c in room_name if not c.isdigit()])  # cr, tr, cc, cl
                room_number = ''.join([c for c in room_name if c.isdigit()])  # 301, 302, etc.
            
            # Create unique ID
            room_id = f"floor{floor_num}_{room_name}"
            
            # Get file size
            file_size = os.path.getsize(file_path)
            
            metadata[room_id] = {
                "id": room_id,
                "floor": floor_num,
                "room_number": room_number,
                "room_type": room_type,
                "room_type_full": get_room_type_full_name(room_type),
                "image_path": f"{floor_num}/{filename}",
                "image_url": get_image_url(floor_num, filename),
                "file_size": file_size,
                "last_updated": datetime.fromtimestamp(os.path.getmtime(file_path)).isoformat()
            }
    
    save_metadata(metadata)
    return metadata


@router.get("/")
async def get_all_floors():
    """Get all available floors."""
    metadata = scan_floor_images()
    
    # Group by floor
    floors = {}
    for room in metadata.values():
        floor_num = room["floor"]
        if floor_num not in floors:
            floors[floor_num] = []
        floors[floor_num].append(room)
    
    # Sort rooms within each floor
    for floor_num in floors:
        floors[floor_num].sort(key=lambda x: (x["room_type"], x["room_number"]))
    
    return JSONResponse(content={
        "success": True,
        "floors": floors,
        "total_rooms": len(metadata)
    })


@router.get("/list")
async def list_floors():
    """Get list of available floor numbers with room counts."""
    metadata = load_metadata()
    
    floor_counts = {}
    for room in metadata.values():
        floor_num = room["floor"]
        if floor_num not in floor_counts:
            floor_counts[floor_num] = 0
        floor_counts[floor_num] += 1
    
    return JSONResponse(content={
        "success": True,
        "floors": [{"floor": k, "room_count": v} for k, v in sorted(floor_counts.items())]
    })


@router.get("/{floor_number}")
async def get_floor_rooms(
    floor_number: str,
    room_type: Optional[str] = Query(None, description="Filter by room type (cr, tr, cc, cl)")
):
    """Get all rooms on a specific floor."""
    metadata = scan_floor_images()
    
    # Filter by floor
    floor_rooms = [room for room in metadata.values() if room["floor"] == floor_number]
    
    if not floor_rooms:
        raise HTTPException(status_code=404, detail=f"Floor {floor_number} not found or has no rooms")
    
    # Apply room type filter if provided
    if room_type:
        floor_rooms = [room for room in floor_rooms if room["room_type"].lower() == room_type.lower()]
        if not floor_rooms:
            raise HTTPException(status_code=404, detail=f"No rooms of type '{room_type}' found on floor {floor_number}")
    
    # Sort by room type and room number
    floor_rooms.sort(key=lambda x: (x["room_type"], x["room_number"]))
    
    return JSONResponse(content={
        "success": True,
        "floor": floor_number,
        "room_count": len(floor_rooms),
        "rooms": floor_rooms
    })


@router.get("/{floor_number}/{room_type}")
async def get_rooms_by_type(
    floor_number: str,
    room_type: str
):
    """Get all rooms of a specific type on a floor (e.g., all classrooms on floor 3)."""
    valid_types = ['cr', 'tr', 'cc', 'cl']
    if room_type.lower() not in valid_types:
        raise HTTPException(status_code=400, detail=f"Invalid room type. Must be one of: {', '.join(valid_types)}")
    
    metadata = load_metadata()
    
    # Filter by floor and room type
    rooms = [
        room for room in metadata.values() 
        if room["floor"] == floor_number and room["room_type"].lower() == room_type.lower()
    ]
    
    if not rooms:
        raise HTTPException(
            status_code=404, 
            detail=f"No {get_room_type_full_name(room_type)}s found on floor {floor_number}"
        )
    
    # Sort by room number
    rooms.sort(key=lambda x: x["room_number"])
    
    return JSONResponse(content={
        "success": True,
        "floor": floor_number,
        "room_type": room_type,
        "room_type_full": get_room_type_full_name(room_type),
        "room_count": len(rooms),
        "rooms": rooms
    })


@router.get("/room/{room_id}")
async def get_room_by_id(room_id: str):
    """Get a specific room by ID."""
    metadata = load_metadata()
    
    if room_id not in metadata:
        raise HTTPException(status_code=404, detail="Room not found")
    
    return JSONResponse(content={
        "success": True,
        "room": metadata[room_id]
    })


@router.get("/find/{room_type}/{room_number}")
async def find_room_by_type_and_number(room_type: str, room_number: str):
    """Find a specific room by type and number (e.g., cc and 301, or cr and 601)."""
    valid_types = ['cr', 'tr', 'cc', 'cl']
    if room_type.lower() not in valid_types:
        raise HTTPException(status_code=400, detail=f"Invalid room type. Must be one of: {', '.join(valid_types)}")
    
    metadata = load_metadata()
    
    # Search for room with matching type and number across all floors
    for room in metadata.values():
        if room["room_type"].lower() == room_type.lower() and room["room_number"] == room_number:
            return JSONResponse(content={
                "success": True,
                "room": room
            })
    
    # Room not found
    raise HTTPException(
        status_code=404, 
        detail=f"Room not found: {get_room_type_full_name(room_type)} {room_number}"
    )


@router.get("/image/{floor_number}/{filename}")
async def get_floor_image(floor_number: str, filename: str):
    """Serve a floor plan image."""
    # Validate filename to prevent directory traversal
    if '..' in filename or '/' in filename or '\\' in filename:
        raise HTTPException(status_code=400, detail="Invalid filename")
    
    if not filename.endswith('.png'):
        raise HTTPException(status_code=400, detail="Only PNG files are supported")
    
    file_path = os.path.join(FLOOR_BASE_PATH, floor_number, filename)
    
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Image not found")
    
    return FileResponse(
        path=file_path,
        media_type="image/png",
        headers={"Content-Disposition": f"inline; filename=\"{filename}\""}
    )


@router.get("/download/{floor_number}/{filename}")
async def download_floor_image(floor_number: str, filename: str):
    """Download a floor plan image."""
    # Validate filename to prevent directory traversal
    if '..' in filename or '/' in filename or '\\' in filename:
        raise HTTPException(status_code=400, detail="Invalid filename")
    
    if not filename.endswith('.png'):
        raise HTTPException(status_code=400, detail="Only PNG files are supported")
    
    file_path = os.path.join(FLOOR_BASE_PATH, floor_number, filename)
    
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Image not found")
    
    return FileResponse(
        path=file_path,
        filename=filename,
        media_type="application/octet-stream"
    )


@router.post("/scan")
async def scan_floors():
    """Manually trigger a scan of the floor folders to update metadata."""
    metadata = scan_floor_images()
    
    return JSONResponse(content={
        "success": True,
        "message": "Floor folders scanned successfully",
        "total_rooms": len(metadata)
    })


@router.get("/types/list")
async def get_room_types():
    """Get list of all room types with counts."""
    metadata = load_metadata()
    
    room_types = {}
    for room in metadata.values():
        room_type = room["room_type"]
        if room_type not in room_types:
            room_types[room_type] = {
                "type": room_type,
                "type_full": room["room_type_full"],
                "count": 0
            }
        room_types[room_type]["count"] += 1
    
    return JSONResponse(content={
        "success": True,
        "room_types": list(room_types.values())
    })
