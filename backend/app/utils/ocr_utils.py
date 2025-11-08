import base64
import os
import re
import tempfile
from typing import Dict, Optional

def encode_document(file_path: str) -> str:
    """Encode the document (image/PDF) to base64."""
    with open(file_path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")

def get_mime_type(content_type: str) -> str:
    """Map content type to MIME type for base64 encoding."""
    mime_map = {
        "image/jpeg": "image/jpeg",
        "image/jpg": "image/jpeg",
        "image/png": "image/png",
        "application/pdf": "application/pdf",
    }
    return mime_map.get(content_type, "application/octet-stream")

def process_ocr_response(markdown_output: str) -> Dict:
    """Process OCR markdown output and extract structured data."""
    program_match = re.search(r'Program:(.*)', markdown_output)
    wef_match = re.search(r'W\.e\.f\.: (.*)', markdown_output)

    program_info = program_match.group(1).strip() if program_match else ""
    wef_date = wef_match.group(1).strip() if wef_match else ""

    # Extract table lines (lines that contain '|')
    table_lines = [line.strip() for line in markdown_output.split("\n") if "|" in line]

    if len(table_lines) < 3:
        return {"Program": program_info, "Effective_From": wef_date, "Timetable": []}

    headers = [h.strip() for h in table_lines[0].split("|")[1:-1]]

    data_rows = []
    for line in table_lines[2:]:
        cells = [c.strip() for c in line.split("|")[1:-1]]
        if len(cells) == len(headers):
            data_rows.append(dict(zip(headers, cells)))

    return {"Program": program_info, "Effective_From": wef_date, "Timetable": data_rows}

def save_upload_to_temp(uploaded_file, suffix: Optional[str] = None) -> str:
    """Save a starlette UploadFile to a temporary file and return its path.

    Caller is responsible for removing the file when done.
    """
    if suffix is None:
        suffix = ".pdf" if uploaded_file.content_type == "application/pdf" else ".jpg"

    tmp_path = None
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp_file:
        content = uploaded_file.file.read()
        tmp_file.write(content)
        tmp_path = tmp_file.name

    return tmp_path
