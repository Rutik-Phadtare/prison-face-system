import warnings
warnings.filterwarnings("ignore")

import face_recognition
import cv2
import numpy as np
import os
import time

# =====================
# PATH SETUP
# =====================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ENCODINGS_DIR = os.path.join(BASE_DIR, "encodings")
GUARDS_DIR = os.path.join(ENCODINGS_DIR, "guards")
PRISONERS_DIR = os.path.join(ENCODINGS_DIR, "prisoners")

TOLERANCE = 0.6  # realistic & safe


def load_encodings(folder, label):
    data = []
    if not os.path.exists(folder):
        return data

    for file in os.listdir(folder):
        if file.endswith(".npy"):
            path = os.path.join(folder, file)
            encoding = np.load(path)
            person_id = file.replace(".npy", "")
            data.append((label, person_id, encoding))

    return data


# =====================
# LOAD ALL KNOWN FACES
# =====================
known_faces = []
known_faces += load_encodings(GUARDS_DIR, "GUARD")
known_faces += load_encodings(PRISONERS_DIR, "PRISONER")

if not known_faces:
    print("OK|UNKNOWN|0")
    exit()


# =====================
# CAPTURE FACE
# =====================
cap = cv2.VideoCapture(0)

face_encoding = None
start_time = time.time()

while time.time() - start_time < 5:
    ret, frame = cap.read()
    if not ret:
        continue

    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    locations = face_recognition.face_locations(rgb)

    if locations:
        encodings = face_recognition.face_encodings(rgb, locations)
        if encodings:
            face_encoding = encodings[0]
            break

cap.release()

if face_encoding is None:
    print("ERROR|NO_FACE|0")
    exit()


# =====================
# BEST-MATCH LOGIC (FIX)
# =====================
best_distance = None
best_label = None
best_person_id = None

for label, person_id, known_encoding in known_faces:
    distance = face_recognition.face_distance(
        [known_encoding],
        face_encoding
    )[0]

    if best_distance is None or distance < best_distance:
        best_distance = distance
        best_label = label
        best_person_id = person_id


# =====================
# FINAL DECISION
# =====================
if best_distance is not None and best_distance <= TOLERANCE:
    print(f"OK|{best_label}|{best_person_id}")
else:
    print("OK|UNKNOWN|0")
