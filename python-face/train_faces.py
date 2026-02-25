import warnings
warnings.filterwarnings("ignore")
import face_recognition
import cv2
import numpy as np
import os
import sys

# =====================
# PATH SETUP
# =====================
if getattr(sys, 'frozen', False):
    BASE_DIR = os.path.dirname(sys.executable)
else:
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))

print(f"BASE_DIR: {BASE_DIR}")

# =====================
# INPUT ARGUMENTS
# =====================
if len(sys.argv) < 3:
    print("Usage: python script.py <type> <id>")
    sys.exit()

person_type = sys.argv[1]
person_id = sys.argv[2]

TARGET_DIR = os.path.join(BASE_DIR, "encodings", person_type.lower() + "s")
PHOTO_DIR = os.path.join(BASE_DIR, "photos", person_type.lower() + "s")

print(f"Saving photo to: {PHOTO_DIR}")
print(f"Saving encoding to: {TARGET_DIR}")

os.makedirs(TARGET_DIR, exist_ok=True)
os.makedirs(PHOTO_DIR, exist_ok=True)

# =====================
# HIGH QUALITY CAMERA SETUP
# =====================
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1920)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 1080)
cap.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc(*'MJPG'))

encodings = []

print(f"--- Training Mode: {person_id} ---")
print("Press 'SPACE' to capture | 'q' to quit")

while True:
    ret, frame = cap.read()
    if not ret: continue

    display_frame = frame.copy()
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

    small_rgb = cv2.resize(rgb, (0, 0), fx=0.5, fy=0.5)
    locations = face_recognition.face_locations(small_rgb)

    for (top, right, bottom, left) in locations:
        top, right, bottom, left = top*2, right*2, bottom*2, left*2
        cv2.rectangle(display_frame, (left, top), (right, bottom), (0, 255, 0), 2)

    cv2.putText(display_frame, f"ID: {person_id} | SPACE TO CAPTURE", (20, 40),
                cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)

    cv2.imshow("Capture Profile", cv2.resize(display_frame, (1280, 720)))
    cv2.setWindowProperty("Capture Profile", cv2.WND_PROP_TOPMOST, 1)  # Force focus

    key = cv2.waitKey(1) & 0xFF

    if key == ord(' '):
        full_locations = face_recognition.face_locations(rgb)

        if len(full_locations) == 1:
            face_encs = face_recognition.face_encodings(rgb, full_locations)
            if face_encs:
                photo_path = os.path.join(PHOTO_DIR, f"{person_id}.jpg")
                cv2.imwrite(photo_path, frame, [int(cv2.IMWRITE_JPEG_QUALITY), 100])

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
    npy_path = os.path.join(TARGET_DIR, f"{person_id}.npy")
    np.save(npy_path, final_encoding)
    print(f"Encoding saved: {npy_path}")
    print("OK|TRAINED|0")