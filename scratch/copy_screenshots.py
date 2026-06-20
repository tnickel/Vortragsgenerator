import os
import shutil

src_dir = r"C:\Users\tnickel\AppData\Local\Temp\antigravity-ide\brain\cdcb7b1b-6e7a-4349-820d-b7c8401ded6c"
# Wait, let's verify if the path has AppData\Local\Temp or .gemini
# In our metadata, it says: App Data Directory is C:\Users\tnickel\.gemini\antigravity-ide
# Let's adjust src_dir to:
src_dir = r"C:\Users\tnickel\.gemini\antigravity-ide\brain\cdcb7b1b-6e7a-4349-820d-b7c8401ded6c"

dest_dirs = [
    r"D:\AntiGravitySoftware\GitWorkspace\Vortragsgenerator\tutorialbilder",
    r"D:\AntiGravitySoftware\GitWorkspace\Vortragsgenerator\src\main\resources\static\tutorialbilder"
]

mapping = {
    "media__1781956804432.png": "01_account_erstellen.png",
    "media__1781957011279.png": "02_elevenapi_auswaehlen.png",
    "media__1781957075011.png": "03_api_schluessel_menue.png",
    "media__1781957149793.png": "04_stimmen_menue.png",
    "media__1781957203914.png": "05_stimme_erstellen_button.png",
    "media__1781957248361.png": "06_sofortige_stimmenklonung.png",
    "media__1781957293672.png": "07_audio_aufnehmen_button.png",
    "media__1781957371897.png": "08_mikrofon_erlauben.png",
    "media__1781957427395.png": "09_mikrofon_auswaehlen.png",
    "media__1781957560066.png": "10_stimmen_id_kopieren.png"
}

for dest in dest_dirs:
    if not os.path.exists(dest):
        os.makedirs(dest, exist_ok=True)
        print(f"Created directory: {dest}")
    
    for src_name, dest_name in mapping.items():
        src_path = os.path.join(src_dir, src_name)
        dest_path = os.path.join(dest, dest_name)
        if os.path.exists(src_path):
            shutil.copy2(src_path, dest_path)
            print(f"Copied {src_name} to {dest_path}")
        else:
            # Maybe check in .tempmediaStorage too
            alt_src_path = os.path.join(src_dir, ".tempmediaStorage", src_name)
            if os.path.exists(alt_src_path):
                shutil.copy2(alt_src_path, dest_path)
                print(f"Copied {src_name} (from tempmedia) to {dest_path}")
            else:
                print(f"WARNING: Source file not found: {src_path}")

print("Copy completed successfully!")
