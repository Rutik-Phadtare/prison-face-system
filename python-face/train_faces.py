import warnings
warnings.filterwarnings("ignore")

import face_recognition
import cv2
import numpy as np
import os
import sys
import time

# =====================
# INPUT ARGUMENTS
# =====================
person_type = sys.argv[1]   # GUARD or PRISONER
person_id = sys.argv[2]

# =====================
# PATH SETUP
# =====================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ENCODINGS_DIR = os.path.join(BASE_DIR, "encodings")
TARGET_DIR = os.path.join(
    ENCODINGS_DIR,
    person_type.lower() + "s"
)

os.makedirs(TARGET_DIR, exist_ok=True)

# =====================
# CAPTURE MULTIPLE FACES
# =====================
cap = cv2.VideoCapture(0)

encodings = []
start_time = time.time()

while time.time() - start_time < 7:  # slightly longer for stability
    ret, frame = cap.read()
    if not ret:
        continue

    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    locations = face_recognition.face_locations(rgb)

    if len(locations) == 1:
        face_encs = face_recognition.face_encodings(rgb, locations)
        if face_encs:
            encodings.append(face_encs[0])

cap.release()

# =====================
# VALIDATION
# =====================
if len(encodings) < 3:
    print("ERROR|FACE_NOT_STABLE|0")
    exit()

# =====================
# AVERAGE ENCODINGS
# =====================
final_encoding = np.mean(encodings, axis=0)

# =====================
# SAVE ENCODING
# =====================
file_path = os.path.join(TARGET_DIR, f"{person_id}.npy")
np.save(file_path, final_encoding)

print("OK|TRAINED|0")
