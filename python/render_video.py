import os
import sys
import re
import argparse

def create_video(images_dir, audio_dir, output_video_path, slide_count, single_slide=None):
    """Combines images and audio files using moviepy to build the final video."""
    print("Lade moviepy und erstelle Video...")
    from moviepy import ImageClip, AudioFileClip, concatenate_videoclips
    
    clips = []
    
    # Determine slides to process
    slides_to_process = [single_slide] if single_slide else list(range(1, slide_count + 1))
    
    for slide_num in slides_to_process:
        image_path = os.path.join(images_dir, f"slide_{slide_num}.png")
        audio_path = os.path.join(audio_dir, f"slide_{slide_num}.mp3")
        
        # Verify both files exist
        if not os.path.exists(image_path):
            print(f"Warnung: Bilddatei fehlt für Folie {slide_num}: {image_path}")
            continue
            
        if not os.path.exists(audio_path):
            print(f"Warnung: Audiodateien fehlt für Folie {slide_num}: {audio_path}")
            continue
            
        try:
            print(f"Verarbeite Folie {slide_num} (Bild: {os.path.basename(image_path)}, Audio: {os.path.basename(audio_path)})")
            
            audio_clip = AudioFileClip(audio_path)
            # Use MoviePy 2.x functional API (with_duration instead of set_duration)
            image_clip = ImageClip(image_path).with_duration(audio_clip.duration)
            video_clip = image_clip.with_audio(audio_clip)
            
            clips.append(video_clip)
        except Exception as e:
            print(f"Fehler bei Folie {slide_num}: {e}")
            raise e
            
    if not clips:
        print("Keine Clips zum Zusammenfügen vorhanden!")
        sys.exit(1)
        
    print(f"Rendere finalen Clip nach: {output_video_path}...")
    final_clip = concatenate_videoclips(clips, method="compose")
    
    # Render final video using standard parameters
    final_clip.write_videofile(
        output_video_path,
        fps=24,
        codec="libx264",
        audio_codec="aac",
        temp_audiofile=os.path.join(audio_dir, f"temp-audio-render.m4a"),
        remove_temp=True
    )
    
    # Close clips to free file locks on Windows
    for clip in clips:
        clip.close()
    final_clip.close()
    print("Video-Rendering erfolgreich abgeschlossen!")

def main():
    parser = argparse.ArgumentParser(description="MoviePy Video-Rendering für Präsentationen")
    parser.add_argument("--images-dir", required=True, help="Pfad zum Verzeichnis mit exported PNGs")
    parser.add_argument("--audio-dir", required=True, help="Pfad zum Verzeichnis mit MP3s")
    parser.add_argument("--output-video", required=True, help="Pfad für die zu erstellende MP4-Datei")
    parser.add_argument("--slide-count", type=int, required=True, help="Anzahl der Folien im Projekt")
    parser.add_argument("--slide", type=int, default=None, help="Rendert nur eine bestimmte Foliennummer")
    args = parser.parse_args()
    
    create_video(args.images_dir, args.audio_dir, args.output_video, args.slide_count, args.slide)

if __name__ == "__main__":
    main()
