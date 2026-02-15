import warnings
warnings.filterwarnings("ignore")
import face_recognition
import cv2
import numpy as np
import os
import sys

# =====================
# INPUT ARGUMENTS
# =====================
if len(sys.argv) < 3:
    print("Usage: python script.py <type> <id>")
    sys.exit()

person_type = sys.argv[1]   
person_id = sys.argv[2]

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
TARGET_DIR = os.path.join(BASE_DIR, "encodings", person_type.lower() + "s")
PHOTO_DIR = os.path.join(BASE_DIR, "photos", person_type.lower() + "s")

os.makedirs(TARGET_DIR, exist_ok=True)
os.makedirs(PHOTO_DIR, exist_ok=True)

# =====================
# HIGH QUALITY CAMERA SETUP
# =====================
cap = cv2.VideoCapture(0)

# Request HD Resolution
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1920)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 1080)
# Use MJPG for better high-res throughput
cap.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc(*'MJPG'))

encodings = []

print(f"--- Training Mode: {person_id} ---")
print("Press 'SPACE' to capture | 'q' to quit")

while True:
    ret, frame = cap.read()
    if not ret: continue

    display_frame = frame.copy()
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    
    # Process small for UI smoothness, save big for quality
    small_rgb = cv2.resize(rgb, (0, 0), fx=0.5, fy=0.5)
    locations = face_recognition.face_locations(small_rgb)

    # Draw preview UI (re-scaling locations for display)
    for (top, right, bottom, left) in locations:
        top, right, bottom, left = top*2, right*2, bottom*2, left*2
        cv2.rectangle(display_frame, (left, top), (right, bottom), (0, 255, 0), 2)
    
    cv2.putText(display_frame, f"ID: {person_id} | SPACE TO CAPTURE", (20, 40), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)
    
    # Scale display window so it fits on screen but captures full HD
    cv2.imshow("Capture Profile", cv2.resize(display_frame, (1280, 720)))
    
    key = cv2.waitKey(1) & 0xFF
    
    if key == ord(' '):
        # Detect on full-size image for precision before saving
        full_locations = face_recognition.face_locations(rgb)
        
        if len(full_locations) == 1:
            face_encs = face_recognition.face_encodings(rgb, full_locations)
            if face_encs:
                photo_path = os.path.join(PHOTO_DIR, f"{person_id}.jpg")
                
                # --- SAVE WITH ZERO COMPRESSION (MAX QUALITY) ---
                # Quality 100 ensures no JPEG artifacts
                cv2.imwrite(photo_path, frame, [int(cv2.IMWRITE_JPEG_QUALITY), 100])
                
                # Visual Flash
                white_flash = np.full(frame.shape, 255, dtype=np.uint8)
                cv2.imshow("Capture Profile", cv2.resize(white_flash, (1280, 720)))
                cv2.waitKey(100)
                
                encodings = [face_encs[0]] 
                print(f"Max quality image saved: {photo_path}")
                break 
        else:
            print(f"Found {len(full_locations)} faces. Need exactly 1!")

    elif key == ord('q'):
        cap.release()
        cv2.destroyAllWindows()
        exit()

cap.release()
cv2.destroyAllWindows()

if encodings:
    final_encoding = encodings[0]
    np.save(os.path.join(TARGET_DIR, f"{person_id}.npy"), final_encoding)
    print("OK|TRAINED|0")