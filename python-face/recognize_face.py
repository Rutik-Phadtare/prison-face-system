import warnings
warnings.filterwarnings("ignore")
import face_recognition
import cv2
import numpy as np
import os
import datetime
import tkinter as tk
import threading
import time

# =====================
# PATH SETUP
# =====================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ENCODINGS_DIR = os.path.join(BASE_DIR, "encodings")
PHOTO_BASE    = os.path.join(BASE_DIR, "photos")

# =====================
# RECOGNITION TUNING
# =====================
TOLERANCE      = 0.45
CONFIRM_FRAMES = 5
AI_SCAN_INTERVAL = 0.08   # seconds between AI scans — gives display thread breathing room
                           # 0.08s = ~12 scans/sec in background, preview stays silky smooth

def load_encodings():
    data = []
    for cat in ["guards", "prisoners"]:
        path = os.path.join(ENCODINGS_DIR, cat)
        if not os.path.exists(path): continue
        for file in os.listdir(path):
            if file.endswith(".npy"):
                enc = np.load(os.path.join(path, file))
                data.append((cat[:-1].upper(), file.replace(".npy", ""), enc, cat))
    return data

known_faces = load_encodings()

# =====================
# WINDOW SETUP
# =====================
MAIN_WIN = "Security Monitor (Live)"
root = tk.Tk()
screen_w, screen_h = root.winfo_screenwidth(), root.winfo_screenheight()
root.destroy()

WIDTH, HEIGHT = 1280, 720
cv2.namedWindow(MAIN_WIN, cv2.WINDOW_NORMAL)
cv2.resizeWindow(MAIN_WIN, WIDTH, HEIGHT)
cv2.moveWindow(MAIN_WIN, (screen_w - WIDTH) // 2, (screen_h - HEIGHT) // 2)

cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_FRAME_WIDTH,  WIDTH)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, HEIGHT)
cap.set(cv2.CAP_PROP_FPS,          60)
cap.set(cv2.CAP_PROP_BUFFERSIZE,   1)

stop_event = threading.Event()

# =====================
# LOCK-FREE SHARED STATE
# Single variable assignment is atomic in CPython — no locks on hot path
# =====================
latest_frame        = None   # written by capture thread, read by display + AI
ai_face_locations   = []
ai_face_names       = []
ai_last_match_photo = None
ai_current_header   = ""
ai_final_label      = "UNKNOWN"
ai_final_id         = "0"

# =====================
# THREAD 1 — CAPTURE
# Keeps latest_frame always fresh — never blocks the display loop
# =====================
def capture_worker():
    global latest_frame
    while not stop_event.is_set():
        ret, frame = cap.read()
        if ret:
            latest_frame = frame

capture_thread = threading.Thread(target=capture_worker, daemon=True)
capture_thread.start()

# =====================
# THREAD 2 — AI SCANNING
# KEY FIX: time.sleep(AI_SCAN_INTERVAL) after each scan deliberately yields
# the GIL back to the display thread so it can render frames uninterrupted.
# Without this sleep, face_recognition monopolises the CPU and causes freezes.
# =====================
def ai_worker():
    global ai_face_locations, ai_face_names
    global ai_last_match_photo, ai_current_header
    global ai_final_label, ai_final_id

    confirm_buffer     = []
    confirmed_identity = None

    while not stop_event.is_set():
        frame = latest_frame
        if frame is None:
            time.sleep(0.01)
            continue

        # Work on a snapshot so latest_frame can keep updating freely
        snap  = frame.copy()
        small = cv2.resize(snap, (0, 0), fx=0.25, fy=0.25)
        rgb   = cv2.cvtColor(small, cv2.COLOR_BGR2RGB)

        locs = face_recognition.face_locations(rgb)
        encs = face_recognition.face_encodings(rgb, locs)

        names       = []
        raw_results = []

        for enc in encs:
            best_dist  = None
            match_data = None
            for label, person_id, known_enc, cat in known_faces:
                d = face_recognition.face_distance([known_enc], enc)[0]
                if best_dist is None or d < best_dist:
                    best_dist  = d
                    match_data = (label, person_id, cat)

            if best_dist is not None and best_dist <= TOLERANCE:
                label, person_id, cat = match_data
                names.append((label, person_id))
                raw_results.append((label, person_id, cat))
            else:
                names.append(("UNKNOWN", "0"))
                raw_results.append(None)

        # Confirmation buffer
        primary = raw_results[0] if raw_results else None
        confirm_buffer.append(primary)
        if len(confirm_buffer) > CONFIRM_FRAMES:
            confirm_buffer.pop(0)

        if len(confirm_buffer) == CONFIRM_FRAMES:
            valid = [r for r in confirm_buffer if r is not None]
            if len(valid) == CONFIRM_FRAMES and len(set(r[1] for r in valid)) == 1:
                confirmed_identity = valid[0]
            else:
                confirmed_identity = None

        # Resolve dossier
        if confirmed_identity:
            label, person_id, cat = confirmed_identity
            new_label, new_id = label, person_id
            new_header = f"VERIFIED {label}"
            new_photo  = None
            photo_path = os.path.join(PHOTO_BASE, cat, f"{person_id}.jpg")
            if os.path.exists(photo_path):
                new_photo = cv2.resize(cv2.imread(photo_path), (380, 320))
        else:
            new_label, new_id = "UNKNOWN", "0"
            new_header = ""
            new_photo  = None

        # Publish results atomically
        ai_face_locations   = locs
        ai_face_names       = names
        ai_last_match_photo = new_photo
        ai_current_header   = new_header
        ai_final_label      = new_label
        ai_final_id         = new_id

        # ← THIS IS THE KEY LINE
        # Yield CPU to the display thread between scans.
        # face_recognition holds the GIL while running — this sleep releases it
        # so the display loop can render frames at full speed uninterrupted.
        time.sleep(AI_SCAN_INTERVAL)

ai_thread = threading.Thread(target=ai_worker, daemon=True)
ai_thread.start()

# =====================
# MAIN LOOP — DISPLAY ONLY
# Pure rendering — no blocking calls, no AI, no camera I/O.
# Runs at maximum possible FPS at all times, even while AI is scanning.
# =====================
final_label = "UNKNOWN"
final_id    = "0"

while True:
    frame = latest_frame
    if frame is None:
        if cv2.waitKey(1) & 0xFF in (ord(' '), ord('q')):
            break
        continue

    # Read AI state — instant, lock-free
    face_locations   = ai_face_locations
    face_names       = ai_face_names
    last_match_photo = ai_last_match_photo
    current_header   = ai_current_header
    final_label      = ai_final_label
    final_id         = ai_final_id

    canvas = frame.copy()

    # Status bar
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cv2.putText(canvas, f"STATUS: ACTIVE | {now}", (20, 40),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

    # Face boxes
    for (top, right, bottom, left), (label, pid) in zip(face_locations, face_names):
        top *= 4; right *= 4; bottom *= 4; left *= 4
        color = (0, 255, 0) if label != "UNKNOWN" else (0, 0, 255)
        cv2.rectangle(canvas, (left, top), (right, bottom), color, 2)
        cv2.putText(canvas, f"{label}: {pid}", (left, top - 10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

    # Dossier overlay
    if last_match_photo is not None:
        h, w, _ = last_match_photo.shape
        xo, yo = WIDTH - w - 30, 60
        canvas[yo:yo+h, xo:xo+w] = last_match_photo
        cv2.rectangle(canvas, (xo, yo),    (xo+w, yo+h),  (0, 255, 0), 1)
        cv2.rectangle(canvas, (xo, yo-30), (xo+w, yo),    (0, 255, 0), -1)
        cv2.putText(canvas, current_header, (xo+3, yo-8),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 0), 2)

    cv2.imshow(MAIN_WIN, canvas)

    key = cv2.waitKey(1) & 0xFF
    if key == ord(' '):
        break
    elif key == ord('q'):
        final_label, final_id = "UNKNOWN", "0"
        break

stop_event.set()
cap.release()
cv2.destroyAllWindows()
print(f"OK|{final_label}|{final_id}")