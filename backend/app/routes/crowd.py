from fastapi import APIRouter, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from typing import Optional
import tempfile
import os

import numpy as np
from collections import deque

from app.utils.ocr_utils import save_upload_to_temp

router = APIRouter(prefix="/crowd", tags=["crowd"])


@router.post("/count")
async def count_crowd(file: UploadFile = File(...), green_max: int = 3, yellow_max: int = 7, conf_threshold: float = 0.4):
    """Count people in uploaded video and classify crowd levels."""
    # Validate file type
    allowed_types = ["video/mp4", "video/avi", "video/x-msvideo", "video/quicktime"]
    if file.content_type not in allowed_types:
        raise HTTPException(status_code=400, detail=f"Invalid file type. Allowed types: MP4, AVI, MOV")

    tmp_path = None
    try:
        try:
            from ultralytics import YOLO
        except ImportError:
            raise HTTPException(status_code=500, detail="YOLO not installed. Run: pip install ultralytics")

        # Save uploaded video to temp
        tmp_path = save_upload_to_temp(file, suffix=".mp4")

        model = YOLO('yolov8n.pt')

        import cv2

        cap = cv2.VideoCapture(tmp_path)
        if not cap.isOpened():
            raise HTTPException(status_code=500, detail="Could not open video file")

        fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        orig_w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        orig_h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

        ROI_POLY_NORM = [(0.15, 0.55), (0.85, 0.55), (0.95, 0.98), (0.05, 0.98)]
        roi_poly_abs = np.array([(int(x*orig_w), int(y*orig_h)) for (x, y) in ROI_POLY_NORM], dtype=np.int32)

        def in_polygon(point, polygon):
            import cv2 as _cv2
            return _cv2.pointPolygonTest(polygon.astype(np.float32), (float(point[0]), float(point[1])), False) >= 0

        def classify_level(count):
            if count <= green_max:
                return "GREEN"
            elif count <= yellow_max:
                return "YELLOW"
            else:
                return "RED"

        SMOOTH_N = 10
        counts_window = deque(maxlen=max(1, SMOOTH_N))
        frame_idx = 0
        all_counts = []
        level_distribution = {"GREEN": 0, "YELLOW": 0, "RED": 0}

        while True:
            ret, frame = cap.read()
            if not ret:
                break

            results = model.predict(source=frame, verbose=False, conf=conf_threshold)
            res = results[0]
            raw_count = 0

            if hasattr(res, "boxes") and res.boxes is not None:
                boxes = res.boxes
                xyxy = boxes.xyxy.cpu().numpy()
                cls = boxes.cls.cpu().numpy().astype(int)

                for i in range(len(xyxy)):
                    if cls[i] != 0:
                        continue
                    x1, y1, x2, y2 = xyxy[i].astype(int)
                    cx = (x1 + x2) // 2
                    cy = (y1 + y2) // 2
                    if in_polygon((cx, cy), roi_poly_abs):
                        raw_count += 1

            counts_window.append(raw_count)
            smoothed = int(np.mean(counts_window))
            level = classify_level(smoothed)
            time_s = frame_idx / fps

            all_counts.append({"frame": frame_idx, "time_s": round(time_s, 2), "raw_count": raw_count, "smoothed_count": smoothed, "level": level})
            level_distribution[level] += 1
            frame_idx += 1

        cap.release()

        smoothed_counts = [c["smoothed_count"] for c in all_counts]

        stats = {
            "video_info": {"duration_seconds": round(total_frames / fps, 2), "total_frames": total_frames, "fps": round(fps, 2), "resolution": f"{orig_w}x{orig_h}"},
            "crowd_statistics": {"average_count": round(np.mean(smoothed_counts), 2), "max_count": int(np.max(smoothed_counts)), "min_count": int(np.min(smoothed_counts)), "median_count": int(np.median(smoothed_counts))},
            "level_distribution": {
                "GREEN": {"frames": level_distribution["GREEN"], "percentage": round(level_distribution["GREEN"] / total_frames * 100, 2)},
                "YELLOW": {"frames": level_distribution["YELLOW"], "percentage": round(level_distribution["YELLOW"] / total_frames * 100, 2)},
                "RED": {"frames": level_distribution["RED"], "percentage": round(level_distribution["RED"] / total_frames * 100, 2)},
            },
            "overall_assessment": classify_level(int(np.mean(smoothed_counts))),
            "peak_crowd_time": all_counts[np.argmax(smoothed_counts)]["time_s"] if len(all_counts) else 0,
            "frame_data": all_counts[:100],
        }

        return JSONResponse(content={"success": True, "data": stats})

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing video: {str(e)}")
    finally:
        if tmp_path and os.path.exists(tmp_path):
            os.unlink(tmp_path)
