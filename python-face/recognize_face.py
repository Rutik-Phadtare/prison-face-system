import warnings
warnings.filterwarnings("ignore")
import face_recognition
import cv2
import numpy as np
import os
import datetime
import tkinter as tk

# =====================
# PATH SETUP
# =====================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ENCODINGS_DIR = os.path.join(BASE_DIR, "encodings")
PHOTO_BASE = os.path.join(BASE_DIR, "photos")
TOLERANCE = 0.6

def load_encodings():
    data = []
    for cat in ["guards", "prisoners"]:
        path = os.path.join(ENCODINGS_DIR, cat)
        if not os.path.exists(path): continue
        for file in os.listdir(path):
            if file.endswith(".npy"):
                encoding = np.load(os.path.join(path, file))
                data.append((cat[:-1].upper(), file.replace(".npy", ""), encoding, cat))
    return data

known_faces = load_encodings()

# =====================
# SCREEN & WINDOW SETUP
# =====================
MAIN_WIN = "Security Monitor (Live)"

root = tk.Tk()
screen_width = root.winfo_screenwidth()
screen_height = root.winfo_screenheight()
root.destroy()

WIDTH, HEIGHT = 1280, 720
cv2.namedWindow(MAIN_WIN, cv2.WINDOW_NORMAL)
cv2.resizeWindow(MAIN_WIN, WIDTH, HEIGHT)

x_pos = int((screen_width / 2) - (WIDTH / 2))
y_pos = int((screen_height / 2) - (HEIGHT / 2))
cv2.moveWindow(MAIN_WIN, x_pos, y_pos)

cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, WIDTH)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, HEIGHT)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1) # Reduce lag by minimizing buffer

# --- High FPS Optimization Variables ---
final_label = "UNKNOWN"
final_id = "0"
last_match_photo = None
current_header = ""
frame_count = 0 
face_locations = []
face_names = []

while True:
    ret, frame = cap.read()
    if not ret: break

    # 1. OPTIMIZATION: Only process every 3rd frame for AI
    # This keeps the video preview running at full camera speed
    if frame_count % 3 == 0:
        # Resize to 1/4 size for AI processing (320x180) - MASSIVE speed boost
        small_frame = cv2.resize(frame, (0, 0), fx=0.25, fy=0.25)
        rgb_small = cv2.cvtColor(small_frame, cv2.COLOR_BGR2RGB)
        
        face_locations = face_recognition.face_locations(rgb_small)
        face_encodings = face_recognition.face_encodings(rgb_small, face_locations)
        
        face_names = []
        for face_encoding in face_encodings:
            best_distance = None
            match_data = None

            for label, person_id, known_encoding, cat in known_faces:
                dist = face_recognition.face_distance([known_encoding], face_encoding)[0]
                if best_distance is None or dist < best_distance:
                    best_distance, match_data = dist, (label, person_id, cat)

            if best_distance is not None and best_distance <= TOLERANCE:
                label, person_id, cat = match_data
                face_names.append((label, person_id))
                
                # Update Dossier variables
                final_label, final_id = label, person_id
                current_header = f"VERIFIED {label}"
                
                photo_path = os.path.join(PHOTO_BASE, cat, f"{person_id}.jpg")
                if os.path.exists(photo_path):
                    stored_img = cv2.imread(photo_path)
                    last_match_photo = cv2.resize(stored_img, (380, 320))
            else:
                face_names.append(("UNKNOWN", "0"))

    frame_count += 1
    display_frame = frame.copy()
    
    # 2. DRAW LIVE DATA
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cv2.putText(display_frame, f"STATUS: ACTIVE (60FPS PREVIEW) | {now}", (20, 40), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
    
    # Draw boxes using locations from the last AI-processed frame
    for (top, right, bottom, left), (label, person_id) in zip(face_locations, face_names):
        # Scale coordinates back up to HD (x4)
        top *= 4; right *= 4; bottom *= 4; left *= 4
        
        color = (0, 255, 0) if label != "UNKNOWN" else (0, 0, 255)
        cv2.rectangle(display_frame, (left, top), (right, bottom), color, 2)
        cv2.putText(display_frame, f"{label}: {person_id}", (left, top - 10), 
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

    # 3. OVERLAP LOGIC (Dossier)
    if last_match_photo is not None:
        h, w, _ = last_match_photo.shape
        margin = 30
        y_offset, x_offset = 60, WIDTH - w - margin
        
        # Inject stored photo
        display_frame[y_offset:y_offset+h, x_offset:x_offset+w] = last_match_photo
        
        border_color = (0, 255, 0)
        cv2.rectangle(display_frame, (x_offset, y_offset), (x_offset+w, y_offset+h), border_color, 1)
        cv2.rectangle(display_frame, (x_offset, y_offset - 30), (x_offset + w, y_offset), border_color, -1)
        cv2.putText(display_frame, current_header, (x_offset + 3, y_offset - 8), 
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 0), 2)

    cv2.imshow(MAIN_WIN, display_frame)

    key = cv2.waitKey(1) & 0xFF
    if key == ord(' '): break
    elif key == ord('q'):
        final_label, final_id = "UNKNOWN", "0"
        break

cap.release()
cv2.destroyAllWindows()
print(f"OK|{final_label}|{final_id}")