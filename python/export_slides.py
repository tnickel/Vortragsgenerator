import os
import sys
import argparse
import re
import win32com.client

def export_slides_to_images(pptx_path, output_dir):
    """Uses PowerPoint COM interface to export slides as PNG files."""
    if not os.path.exists(pptx_path):
        raise FileNotFoundError(f"PowerPoint-Datei nicht gefunden: {pptx_path}")
        
    abs_pptx_path = os.path.abspath(pptx_path)
    abs_output_dir = os.path.abspath(output_dir)
    
    if not os.path.exists(abs_output_dir):
        os.makedirs(abs_output_dir)
        
    print(f"Starte PowerPoint COM und exportiere Folien nach: {abs_output_dir}...")
    
    powerpoint = None
    presentation = None
    try:
        # Initialize COM
        powerpoint = win32com.client.Dispatch("PowerPoint.Application")
        # Open presentation in windowless mode
        presentation = powerpoint.Presentations.Open(abs_pptx_path, WithWindow=False)
        # Export all slides to PNG
        presentation.Export(abs_output_dir, "PNG")
        print("PowerPoint-Export erfolgreich abgeschlossen.")
        
        # Rename output files to have consistent structure: slide_1.png, slide_2.png, etc.
        # PowerPoint exports files as Folie1.PNG, Folie2.PNG, or Slide1.PNG, Slide2.PNG based on language.
        files = os.listdir(abs_output_dir)
        for f in files:
            match = re.search(r'(?:Folie|Slide)\D*(\d+)\.(?:png|jpg|jpeg)$', f, re.IGNORECASE)
            if match:
                slide_num = int(match.group(1))
                old_path = os.path.join(abs_output_dir, f)
                new_path = os.path.join(abs_output_dir, f"slide_{slide_num}.png")
                # Avoid overwriting conflicts, replace if exists
                if os.path.exists(new_path) and old_path != new_path:
                    os.remove(new_path)
                os.rename(old_path, new_path)
                print(f"Datei umbenannt: {f} -> slide_{slide_num}.png")
                
    except Exception as e:
        print(f"Fehler beim Exportieren der PowerPoint-Folien: {e}")
        sys.exit(1)
    finally:
        if presentation:
            presentation.Close()
        if powerpoint:
            powerpoint.Quit()

def main():
    parser = argparse.ArgumentParser(description="PowerPoint COM Folien-Export")
    parser.add_argument("--pptx-path", required=True, help="Pfad zur PPTX-Datei")
    parser.add_argument("--output-dir", required=True, help="Zielordner für die exportierten PNG-Bilder")
    args = parser.parse_args()
    
    export_slides_to_images(args.pptx_path, args.output_dir)

if __name__ == "__main__":
    main()
