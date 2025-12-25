import warnings
warnings.filterwarnings("ignore")

import face_recognition
import cv2
import numpy as np
import os
import sys
import time

# Arguments: GUARD/PRISONER and DB ID
person_type = sys.argv[1]   # GUARD or PRISONER
person_id = sys.argv[2]

# Resolve paths relative to this file
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ENCODINGS_DIR = os.path.join(BASE_DIR, "encodings")
TARGET_DIR = os.path.join(
    ENCODINGS_DIR,
    person_type.lower() + "s"
)

os.makedirs(TARGET_DIR, exist_ok=True)

cap = cv2.VideoCapture(0)

encoding = None
start_time = time.time()

# Try for up to 5 seconds to get a stable face
while time.time() - start_time < 5:
    ret, frame = cap.read()
    if not ret:
        continue

    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    faces = face_recognition.face_locations(rgb)

    if len(faces) == 1:
        encoding = face_recognition.face_encodings(rgb, faces)[0]
        break

cap.release()

if encoding is None:
    print("ERROR|FACE_NOT_DETECTED|0")
    exit()

file_path = os.path.join(TARGET_DIR, f"{person_id}.npy")
np.save(file_path, encoding)

print("OK|TRAINED|0")
