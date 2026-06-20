import os
import shutil

src_static = r"D:\AntiGravitySoftware\GitWorkspace\Vortragsgenerator\src\main\resources\static"
target_static = r"D:\AntiGravitySoftware\GitWorkspace\Vortragsgenerator\target\classes\static"

def copy_recursive(src, dst):
    if not os.path.exists(src):
        return
    if os.path.isdir(src):
        if not os.path.exists(dst):
            os.makedirs(dst)
        for item in os.listdir(src):
            s = os.path.join(src, item)
            d = os.path.join(dst, item)
            copy_recursive(s, d)
    else:
        shutil.copy2(src, dst)
        print(f"Synced {src} -> {dst}")

copy_recursive(src_static, target_static)
print("Synchronization completed successfully!")
