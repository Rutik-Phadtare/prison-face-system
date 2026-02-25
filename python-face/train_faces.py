import warnings
warnings.filterwarnings("ignore")
import face_recognition
import cv2
import numpy as np
import os
import sys
import tkinter as tk
from tkinter import messagebox, filedialog

# =====================
# PATH SETUP
# =====================
if getattr(sys, 'frozen', False):
    BASE_DIR = os.path.dirname(sys.executable)
else:
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))

print(f"BASE_DIR: {BASE_DIR}")

if len(sys.argv) < 3:
    print("Usage: python script.py <type> <id>")
    sys.exit()

person_type = sys.argv[1]
person_id   = sys.argv[2]

TARGET_DIR = os.path.join(BASE_DIR, "encodings", person_type.lower() + "s")
PHOTO_DIR  = os.path.join(BASE_DIR, "photos",    person_type.lower() + "s")
os.makedirs(TARGET_DIR, exist_ok=True)
os.makedirs(PHOTO_DIR,  exist_ok=True)

# ══════════════════════════════════════════════════════
#  POPUP — Match app theme
# ══════════════════════════════════════════════════════
def choose_option():
    root = tk.Tk()
    root.title("Face Training Setup")
    root.withdraw()  # Hide while building
    root.resizable(False, False)
    root.configure(bg="#ffffff")
    root.attributes('-topmost', True)

    choice = {"value": None}

    tk.Frame(root, bg="#c8a415", height=4).pack(fill="x")

    header = tk.Frame(root, bg="#1a2a4a")
    header.pack(fill="x")

    header_inner = tk.Frame(header, bg="#1a2a4a", padx=28, pady=18)
    header_inner.pack(fill="x")

    tk.Label(header_inner,
             text="MINISTRY OF JUSTICE  —  DEPARTMENT OF CORRECTIONS",
             bg="#1a2a4a", fg="#c8a415",
             font=("Georgia", 8, "bold"),
             anchor="w").pack(fill="x")

    tk.Label(header_inner,
             text="Biometric Face Training",
             bg="#1a2a4a", fg="#ffffff",
             font=("Georgia", 18, "bold"),
             anchor="w").pack(fill="x", pady=(4, 2))

    tk.Label(header_inner,
             text="Facial Recognition Enrollment System   |   Classification: RESTRICTED",
             bg="#1a2a4a", fg="#7a8faa",
             font=("Segoe UI", 9),
             anchor="w").pack(fill="x")

    tk.Frame(root, bg="#c8a415", height=2).pack(fill="x")

    body = tk.Frame(root, bg="#ffffff", padx=32, pady=24)
    body.pack(fill="both", expand=True)

    badge_bg = "#dbeafe" if person_type == "GUARD" else "#fef9e7"
    badge_fg = "#1d4ed8" if person_type == "GUARD" else "#b45309"
    icon     = "🛡️" if person_type == "GUARD" else "👤"

    info_frame = tk.Frame(body, bg=badge_bg)
    info_frame.pack(fill="x", pady=(0, 20))
    tk.Label(info_frame,
             text=f"{icon}   Enrolling:  {person_type}   •   ID: #{person_id}",
             bg=badge_bg, fg=badge_fg,
             font=("Segoe UI", 11, "bold"),
             anchor="w", padx=16, pady=10).pack(fill="x")

    tk.Label(body,
             text="Select the method to capture the biometric photo:",
             bg="#ffffff", fg="#374151",
             font=("Segoe UI", 10),
             anchor="center").pack(fill="x", pady=(0, 20))

    btn_frame = tk.Frame(body, bg="#ffffff")
    btn_frame.pack(fill="x")
    btn_frame.columnconfigure(0, weight=1)
    btn_frame.columnconfigure(1, weight=1)

    def pick_camera():
        choice["value"] = "camera"
        root.destroy()

    def pick_file():
        choice["value"] = "file"
        root.destroy()

    def on_cancel():
        choice["value"] = "cancel"
        root.destroy()

    cam_btn = tk.Button(btn_frame, text="📷   Use Live Camera",
                        bg="#1a2a4a", fg="white",
                        font=("Segoe UI", 11, "bold"),
                        relief="flat", cursor="hand2", height=2,
                        command=pick_camera,
                        activebackground="#243550", activeforeground="white")
    cam_btn.grid(row=0, column=0, padx=(0, 8), sticky="ew")
    cam_btn.bind("<Enter>", lambda e: cam_btn.configure(bg="#243550"))
    cam_btn.bind("<Leave>", lambda e: cam_btn.configure(bg="#1a2a4a"))

    file_btn = tk.Button(btn_frame, text="🖼️   Upload from PC",
                         bg="#c8a415", fg="#1a2a4a",
                         font=("Segoe UI", 11, "bold"),
                         relief="flat", cursor="hand2", height=2,
                         command=pick_file,
                         activebackground="#b8940f", activeforeground="#1a2a4a")
    file_btn.grid(row=0, column=1, padx=(8, 0), sticky="ew")
    file_btn.bind("<Enter>", lambda e: file_btn.configure(bg="#b8940f"))
    file_btn.bind("<Leave>", lambda e: file_btn.configure(bg="#c8a415"))

    tk.Label(btn_frame, text="Live webcam capture",
             bg="#ffffff", fg="#94a3b8",
             font=("Segoe UI", 8),
             anchor="center").grid(row=1, column=0, pady=(8, 0), sticky="ew")

    tk.Label(btn_frame, text="JPG, PNG, BMP, WEBP",
             bg="#ffffff", fg="#94a3b8",
             font=("Segoe UI", 8),
             anchor="center").grid(row=1, column=1, pady=(8, 0), sticky="ew")

    tk.Frame(root, bg="#e2e8f0", height=1).pack(fill="x")

    footer = tk.Frame(root, bg="#f8fafc", padx=28, pady=10)
    footer.pack(fill="x")

    tk.Label(footer,
             text="⚠   Authorised users only. All activity is monitored and logged.",
             bg="#f8fafc", fg="#94a3b8",
             font=("Segoe UI", 8), anchor="w").pack(side="left")

    tk.Button(footer, text="✕   Cancel",
              bg="#f8fafc", fg="#64748b",
              font=("Segoe UI", 9),
              relief="flat", cursor="hand2", padx=8,
              command=on_cancel,
              activebackground="#f1f5f9",
              activeforeground="#374151").pack(side="right")
    
    root.update_idletasks()
    w, h = 560, 420
    x = (root.winfo_screenwidth()  // 2) - (w // 2)
    y = (root.winfo_screenheight() // 2) - (h // 2)
    root.geometry(f"{w}x{h}+{x}+{y}")
    root.deiconify()  # Now show perfectly centered and fully rendered


    root.protocol("WM_DELETE_WINDOW", on_cancel)
    root.mainloop()
    return choice["value"]


# ══════════════════════════════════════════════════════
#  OPTION 1 — CAMERA (original logic, preserved exactly)
# ══════════════════════════════════════════════════════
def capture_from_camera():
    # ── Exact same setup as original working version ──
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

        # Process small for UI smoothness
        small_rgb = cv2.resize(rgb, (0, 0), fx=0.5, fy=0.5)
        locations = face_recognition.face_locations(small_rgb)

        # Draw boxes scaled back up
        for (top, right, bottom, left) in locations:
            top, right, bottom, left = top*2, right*2, bottom*2, left*2
            cv2.rectangle(display_frame, (left, top), (right, bottom), (26, 42, 74), 2)
            cv2.rectangle(display_frame, (left, top - 32), (right, top), (26, 42, 74), -1)
            cv2.putText(display_frame, "Face Detected",
                        (left + 6, top - 9),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)

        # Status bar
        count = len(locations)
        cv2.rectangle(display_frame, (0, 0), (1920, 56), (26, 42, 74), -1)
        cv2.rectangle(display_frame, (0, 0), (1920, 4), (200, 164, 21), -1)

        if count == 1:
            status = "READY  —  Press SPACE to capture"
            color  = (34, 200, 94)
        elif count == 0:
            status = "No face detected — position yourself in frame"
            color  = (200, 200, 200)
        else:
            status = f"{count} faces — need exactly 1"
            color  = (60, 60, 220)

        cv2.putText(display_frame,
                    f"ID: #{person_id}   |   {person_type}   |   {status}",
                    (20, 38),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.75, color, 2)

        # Corner brackets (gold)
        L = 50
        for (x, y, sx, sy) in [
            (0, 0, 1, 1), (1920, 0, -1, 1),
            (0, 1080, 1, -1), (1920, 1080, -1, -1)
        ]:
            cv2.line(display_frame, (x, y), (x + sx*L, y), (200, 164, 21), 4)
            cv2.line(display_frame, (x, y), (x, y + sy*L), (200, 164, 21), 4)

        # ── Same as original: resize to 1280x720 for display ──
        cv2.imshow("Capture Profile", cv2.resize(display_frame, (1280, 720)))
        cv2.setWindowProperty("Capture Profile", cv2.WND_PROP_TOPMOST, 1)

        key = cv2.waitKey(1) & 0xFF

        if key == ord(' '):
            # ── Exact same capture logic as original ──
            full_locations = face_recognition.face_locations(rgb)

            if len(full_locations) == 1:
                face_encs = face_recognition.face_encodings(rgb, full_locations)
                if face_encs:
                    photo_path = os.path.join(PHOTO_DIR, f"{person_id}.jpg")
                    cv2.imwrite(photo_path, frame, [int(cv2.IMWRITE_JPEG_QUALITY), 100])

                    # Flash
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
            print("ERROR|CANCELLED|0")
            return

    cap.release()
    cv2.destroyAllWindows()

    if encodings:
        npy_path = os.path.join(TARGET_DIR, f"{person_id}.npy")
        np.save(npy_path, encodings[0])
        print(f"Encoding saved: {npy_path}")
        print("OK|TRAINED|0")


# ══════════════════════════════════════════════════════
#  OPTION 2 — DEFAULT FILE EXPLORER
# ══════════════════════════════════════════════════════
def capture_from_file():
    root = tk.Tk()
    root.withdraw()
    root.attributes('-topmost', True)

    file_path = filedialog.askopenfilename(
        title="Select Face Image",
        filetypes=[
            ("Image Files", "*.jpg *.jpeg *.png *.bmp *.webp"),
            ("All Files", "*.*")
        ],
        initialdir=os.path.expanduser("~/Pictures")
    )
    root.destroy()

    if not file_path:
        print("ERROR|NO_FILE|0")
        return

    image     = face_recognition.load_image_file(file_path)
    locations = face_recognition.face_locations(image)

    def show_error(title, msg):
        r = tk.Tk()
        r.withdraw()
        r.attributes('-topmost', True)
        messagebox.showerror(title, msg)
        r.destroy()

    if len(locations) == 0:
        show_error("No Face Found",
                   "No face detected in the selected image.\nPlease choose a clearer photo.")
        print("ERROR|NO_FACE|0")
        return

    if len(locations) > 1:
        show_error("Multiple Faces",
                   f"Found {len(locations)} faces.\nPlease use an image with exactly 1 face.")
        print("ERROR|MULTI_FACE|0")
        return

    face_encs = face_recognition.face_encodings(image, locations)
    if not face_encs:
        print("ERROR|ENCODING_FAILED|0")
        return

    photo_path = os.path.join(PHOTO_DIR, f"{person_id}.jpg")
    frame_bgr  = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
    cv2.imwrite(photo_path, frame_bgr, [int(cv2.IMWRITE_JPEG_QUALITY), 100])

    npy_path = os.path.join(TARGET_DIR, f"{person_id}.npy")
    np.save(npy_path, face_encs[0])

    print(f"Photo saved:    {photo_path}")
    print(f"Encoding saved: {npy_path}")
    print("OK|TRAINED|0")


# ══════════════════════════════════════════════════════
#  MAIN
# ══════════════════════════════════════════════════════
option = choose_option()

if option == "camera":
    capture_from_camera()
elif option == "file":
    capture_from_file()
else:
    print("ERROR|CANCELLED|0")
