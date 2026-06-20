# Vortragsgenerator - Dokumentation & GitHub Guide 📝

Willkommen im Dokumentationsverzeichnis des Vortragsgenerators!

Dieser Guide gibt Entwicklern, die das Projekt von GitHub klonen, einen detaillierten Überblick über die Funktionsweise und das Setup.

---

## 💡 Showcase-Charakter

> [!IMPORTANT]
> Bitte beachten Sie, dass dieses Projekt als Showcase konzipiert wurde. Es läuft lokal unter Windows (wegen PowerPoint win32com Automation) und dient als Machbarkeitsnachweis für das Zusammenspiel von **Spring Boot, H2, OpenRouter und ElevenLabs**. Für den produktiven Einsatz muss es entsprechend an eigene IT-Infrastrukturen und Sicherheitsstandards angepasst werden.

---

## 🏗️ Systemarchitektur

Die Architektur trennt das responsive Web-Frontend (Glassmorphism), das Java-Backend mit Spring Boot zur REST-Kommunikation und API-Steuerung, die relationale Datenbank H2 und die lokale Python-Rendering-Pipeline:

![System Architektur](./assets/vortrag_tech_architecture.png)

---

## 🗄️ Datenstruktur (H2 Database Schema)

Zur persistenten Speicherung der Projekte und Versionen wird folgendes relationale Datenbankschema in H2 genutzt:

![H2 Datenbank Schema](./assets/vortrag_tech_database.png)

---

## ⚙️ Funktionsweise der Synthese

1. **Prompt-Eingabe:** Der Benutzer gibt einen Wunsch-Vortrag im Web-Frontend ein.
2. **LLM-Generierung:** Das Backend sendet die Anfrage über OpenRouter an das LLM (z.B. Claude). Dieses liefert strukturierten Folientext und Rednernotizen zurück.
3. **Datenbank-Caching:** Die Folien und Sprechtexte werden persistiert.
4. **Stimmenklonung (TTS):** Das Skript wird abschnittsweise an ElevenLabs gesendet. Die MP3-Audiodateien werden lokal gespeichert.
5. **Video-Generierung:** Ein Python-Subprozess öffnet PowerPoint im Hintergrund, exportiert die Folien als PNG-Grafiken und fügt Bild und Ton über MoviePy 2.x zusammen. Das fertige Video wird ins Projektverzeichnis exportiert und kann direkt im Browser abgespielt werden.

---

## 🚀 Setup-Schritte
Siehe die Hauptanleitung im [Root README.md](../README.md).
