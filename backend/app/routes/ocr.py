from fastapi import APIRouter, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from typing import Optional
from app.utils.mistral_client import get_client
from app.utils.ocr_utils import encode_document, get_mime_type, process_ocr_response, save_upload_to_temp
import os

router = APIRouter(prefix="/ocr", tags=["ocr"])


@router.post("/extract-timetable")
async def extract_timetable(file: UploadFile = File(...), page_number: Optional[int] = None):
    allowed_types = ["image/jpeg", "image/jpg", "image/png", "application/pdf"]
    if file.content_type not in allowed_types:
        raise HTTPException(status_code=400, detail=f"Invalid file type. Allowed types: {', '.join(allowed_types)}")

    tmp_path = None
    try:
        suffix = ".pdf" if file.content_type == "application/pdf" else ".jpg"
        # save to temp
        tmp_path = save_upload_to_temp(file, suffix=suffix)

        base64_document = encode_document(tmp_path)
        mime_type = get_mime_type(file.content_type)

        client = get_client()
        ocr_response = client.ocr.process(
            model="mistral-ocr-latest",
            document={"type": "document_url", "document_url": f"data:{mime_type};base64,{base64_document}"},
            include_image_base64=True,
        )

        if not getattr(ocr_response, "pages", None):
            raise HTTPException(status_code=500, detail="No pages found in OCR response")

        total_pages = len(ocr_response.pages)
        if page_number is not None:
            if page_number < 1 or page_number > total_pages:
                raise HTTPException(status_code=400, detail=f"Invalid page number. Document has {total_pages} pages.")
            markdown_output = ocr_response.pages[page_number - 1].markdown
            timetable_json = process_ocr_response(markdown_output)
            return JSONResponse(content={"success": True, "total_pages": total_pages, "processed_page": page_number, "data": timetable_json, "raw_markdown": markdown_output})

        all_results = []
        for i, page in enumerate(ocr_response.pages):
            timetable_json = process_ocr_response(page.markdown)
            all_results.append({"page_number": i + 1, "data": timetable_json, "raw_markdown": page.markdown})

        return JSONResponse(content={"success": True, "total_pages": total_pages, "results": all_results})

    finally:
        if tmp_path and os.path.exists(tmp_path):
            os.unlink(tmp_path)


@router.post("/extract-raw")
async def extract_raw(file: UploadFile = File(...), page_number: Optional[int] = None):
    allowed_types = ["image/jpeg", "image/jpg", "image/png", "application/pdf"]
    if file.content_type not in allowed_types:
        raise HTTPException(status_code=400, detail=f"Invalid file type. Allowed types: {', '.join(allowed_types)}")

    tmp_path = None
    try:
        suffix = ".pdf" if file.content_type == "application/pdf" else ".jpg"
        tmp_path = save_upload_to_temp(file, suffix=suffix)

        base64_document = encode_document(tmp_path)
        mime_type = get_mime_type(file.content_type)

        client = get_client()
        ocr_response = client.ocr.process(
            model="mistral-ocr-latest",
            document={"type": "document_url", "document_url": f"data:{mime_type};base64,{base64_document}"},
            include_image_base64=True,
        )

        total_pages = len(ocr_response.pages)
        if page_number is not None:
            if page_number < 1 or page_number > total_pages:
                raise HTTPException(status_code=400, detail=f"Invalid page number. Document has {total_pages} pages.")
            return JSONResponse(content={"success": True, "total_pages": total_pages, "page_number": page_number, "markdown": ocr_response.pages[page_number - 1].markdown})

        pages_output = "\n\n".join([f"Page {i+1}\n{ocr_response.pages[i].markdown}" for i in range(len(ocr_response.pages))])
        return JSONResponse(content={"success": True, "total_pages": total_pages, "markdown": pages_output})

    finally:
        if tmp_path and os.path.exists(tmp_path):
            os.unlink(tmp_path)


@router.post("/extract-url")
async def extract_from_url(url: str, page_number: Optional[int] = None):
    try:
        client = get_client()
        ocr_response = client.ocr.process(
            model="mistral-ocr-latest",
            document={"type": "document_url", "document_url": url},
            include_image_base64=True,
        )

        if not getattr(ocr_response, "pages", None):
            raise HTTPException(status_code=500, detail="No pages found in OCR response")

        total_pages = len(ocr_response.pages)
        if page_number is not None:
            if page_number < 1 or page_number > total_pages:
                raise HTTPException(status_code=400, detail=f"Invalid page number. Document has {total_pages} pages.")
            markdown_output = ocr_response.pages[page_number - 1].markdown
            timetable_json = process_ocr_response(markdown_output)
            return JSONResponse(content={"success": True, "total_pages": total_pages, "processed_page": page_number, "data": timetable_json, "raw_markdown": markdown_output})

        all_results = []
        for i, page in enumerate(ocr_response.pages):
            timetable_json = process_ocr_response(page.markdown)
            all_results.append({"page_number": i + 1, "data": timetable_json, "raw_markdown": page.markdown})

        return JSONResponse(content={"success": True, "total_pages": total_pages, "url": url, "results": all_results})

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing URL: {str(e)}")
