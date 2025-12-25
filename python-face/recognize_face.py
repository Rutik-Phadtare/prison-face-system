import warnings
warnings.filterwarnings("ignore")

import face_recognition
import cv2
import numpy as np
import os
import time

# Resolve paths relative to this file
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ENCODINGS_DIR = os.path.join(BASE_DIR, "encodings")
GUARDS_DIR = os.path.join(ENCODINGS_DIR, "guards")
PRISONERS_DIR = os.path.join(ENCODINGS_DIR, "prisoners")


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


# Load all known faces
known_faces = []
known_faces += load_encodings(GUARDS_DIR, "GUARD")
known_faces += load_encodings(PRISONERS_DIR, "PRISONER")

if not known_faces:
    print("OK|UNKNOWN|0")
    exit()


# Open camera and wait for stable face
cap = cv2.VideoCapture(0)

face_encoding = None
start_time = time.time()

while time.time() - start_time < 5:
    ret, frame = cap.read()
    if not ret:
        continue

    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    faces = face_recognition.face_locations(rgb)

    if len(faces) >= 1:
        face_encoding = face_recognition.face_encodings(rgb, faces)[0]
        break

cap.release()

if face_encoding is None:
    print("ERROR|NO_FACE|0")
    exit()


# Compare with known encodings
for label, person_id, known_encoding in known_faces:
    distance = face_recognition.face_distance(
        [known_encoding],
        face_encoding
    )[0]

    # Final, realistic threshold for webcam-based academic projects
    if distance < 0.8:
        print(f"OK|{label}|{person_id}")
        exit()

print("OK|UNKNOWN|0")
