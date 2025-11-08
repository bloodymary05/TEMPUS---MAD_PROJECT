# Floor Plans API

This API provides access to floor plan images for different rooms across various floors.

## Room Types

- **cr** - Classroom
- **tr** - Tutorial Room
- **cc** - Computer Lab
- **cl** - Conference/Lab

## Available Endpoints

### Get All Floors

```
GET /floor/
```

Returns all floors with their rooms grouped by floor number.

**Response:**

```json
{
  "success": true,
  "floors": {
    "3": [
      {
        "id": "floor3_cr301",
        "floor": "3",
        "room_number": "301",
        "room_type": "cr",
        "room_type_full": "Classroom",
        "image_path": "3/cr301.png",
        "file_size": 12345,
        "last_updated": "2025-11-05T00:00:00"
      }
    ]
  },
  "total_rooms": 12
}
```

### List Floors

```
GET /floor/list
```

Returns a simple list of available floors with room counts.

### Get Floor Rooms

```
GET /floor/{floor_number}?room_type=cr
```

Get all rooms on a specific floor, optionally filtered by room type.

**Example:** `/floor/3?room_type=tr` - Get all tutorial rooms on floor 3

### Get Rooms by Type

```
GET /floor/{floor_number}/{room_type}
```

Get all rooms of a specific type on a floor.

**Example:** `/floor/3/cr` - Get all classrooms on floor 3

### Get Room by ID

```
GET /floor/room/{room_id}
```

Get details for a specific room.

**Example:** `/floor/room/floor3_cr301`

### View Floor Image

```
GET /floor/image/{floor_number}/{filename}
```

Serve a floor plan image (displays inline in browser).

**Example:** `/floor/image/3/cr301.png`

### Download Floor Image

```
GET /floor/download/{floor_number}/{filename}
```

Download a floor plan image.

**Example:** `/floor/download/3/tr302.png`

### Get Room Types

```
GET /floor/types/list
```

Get list of all room types with counts.

### Scan Floors

```
POST /floor/scan
```

Manually trigger a scan to update metadata from floor folders.

## Usage Examples

### Get all classrooms on floor 3:

```bash
curl http://localhost:8000/floor/3/cr
```

### View floor plan for classroom 301:

```bash
curl http://localhost:8000/floor/image/3/cr301.png
```

### Get all tutorial rooms:

```bash
curl http://localhost:8000/floor/3?room_type=tr
```

## Metadata Structure

The metadata is automatically generated from the files in the `floor/` directory and stored in `floor/metadata.json`. You can manually trigger a rescan by calling the `/floor/scan` endpoint.
