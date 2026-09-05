# ✨ WisdomPulse 2.0 - Legends & Kavya Manch

[![Download APK](https://img.shields.io/badge/Download-WisdomPulse_v2.0.apk-2ECC71?style=for-the-badge&logo=android)](https://github.com/ashirvadraj/WisdomPulse/raw/main/WisdomPulse.apk)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)](https://kotlinlang.org)
[![Material Design](https://img.shields.io/badge/Material-3-purple.svg)](https://m3.material.io)

> **"हार नहीं मानूँगा, रार नहीं ठानूँगा, काल के कपाल पर लिखता मिटाता हूँ, गीत नया गाता हूँ।"**  
> — *Atal Bihari Vajpayee*

WisdomPulse 2.0 is a modern, offline-first Android application dedicated to the timeless quotes, philosophy, and poetry of world legends. Now upgraded with the **complete iconic poetic works of Atal Bihari Vajpayee**, **artistic author portraits for all legends**, and a **9:16 Wallpaper Studio 2.0 with author portrait badges**.

---

## 📲 Direct APK Download

Download and install the updated APK directly on your phone:
👉 **[Download WisdomPulse.apk (v2.0 - 6.0 MB)](https://github.com/ashirvadraj/WisdomPulse/raw/main/WisdomPulse.apk)**

---

## 🌟 What's New in Version 2.0

### 1. 📜 Complete Atal Bihari Vajpayee Poetry (काव्य मंच)
Featuring full multi-stanza texts, Hindi Devnagari script, and poetic English translations:
- **गीत नया गाता हूँ** (Geet Naya Gata Hoon)
- **क़दम मिलाकर चलना होगा** (Kadam Milakar Chalna Hoga)
- **हार नहीं मानूँगा, रार नहीं ठानूँगा** (Haar Nahi Manunga)
- **मौत से ठन गई!** (Maut Se Than Gayi)
- **आओ फिर से दिया जलाएँ** (Aao Phir Se Diya Jalayein)
- **दूध में दरार पड़ गई** (Doodh Mein Darar Pad Gayi)
- **गीत नहीं गाता हूँ** (Geet Nahi Gata Hoon)
- **पंद्रह अगस्त का दिन कहता** (Pandraha August Ka Din Kehta)
- **कौरव कौन, कौन पांडव** (Kaurav Kaun, Kaun Pandav)
- **हिरोशिमा की पीड़ा** (Hiroshima Ki Peeda)
- **मनाली मत जइyo** (Manali Mat Jaiyo)
- **अपने ही मन से बात करें** (Apne Hi Man Se Baat Karein)
- **जीवन की ढलती साँझ में** (Jeevan Ki Dhalti Saanjh Mein)

### 2. 🎨 Artistic Author Portraits for All Icons
Custom circular vector portraits with subtle golden glowing borders:
- **Dr. A.P.J. Abdul Kalam** (Silver hair wings & missile visionary badge)
- **Atal Bihari Vajpayee** (Statesman & poet quill badge)
- **Swami Vivekananda** (Iconic saffron turban)
- **Steve Jobs** (Minimalist glasses & turtleneck)
- **Albert Einstein** (Contemplative scientific icon)
- **Mahatma Gandhi** (Wire spectacles & shawl)
- **Bhagat Singh** (Iconic revolutionary hat)
- **Dr. B.R. Ambedkar** (Blue scholar aura)
- **Nelson Mandela** (Dignified freedom fighter)
- **Rabindranath Tagore** (Flowing beard of the Nobel poet)

### 3. 🖼️ 9:16 Wallpaper Studio 2.0 (Phone & WhatsApp Wallpaper)
- **Author Portrait Badge on Wallpaper**: The 9:16 wallpaper now includes the author's portrait badge in the center!
- **Portrait Toggle**: Switch author portrait ON or OFF with a single tap.
- **8 Themes**: Midnight OLED, Royal Gold, Crimson Dusk, Forest Zen, Deep Indigo, Sunset Ember, and Vintage Parchment.
- **1-Tap System Integrations**:
  - **Set as Phone Wallpaper**: Uses native `WallpaperManager` to apply to Home & Lock screen directly.
  - **Save for WhatsApp Wallpaper**: Renders high-res 1080x1920 PNG to gallery (`Pictures/WisdomPulse/`) and opens WhatsApp's wallpaper picker.

### 4. ⏰ Smart Offline Daily Notifications
- Daily 7:00 AM inspirational reminder with quick action to open Wallpaper Studio.
- "Inspire Me Now" button on the home bar for instant testing.

### 5. 🔊 Text-to-Speech (TTS) Voiceover
- Native `android.speech.tts.TextToSpeech` reads English quotes and Hindi poems aloud.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 1.9.24
- **Architecture:** Android Jetpack & ViewBinding
- **UI Components:** Google Material Components 3 (`Theme.Material3.Dark.NoActionBar`)
- **JSON Parsing:** Google Gson 2.10.1
- **Notifications:** Android `NotificationManager` + `AlarmManager` + `BroadcastReceiver`
- **Wallpaper API:** Android `WallpaperManager` + `MediaStore`

---

## 📦 Building from Source

```bash
git clone https://github.com/ashirvadraj/WisdomPulse.git
cd WisdomPulse
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
MIT License. Free to use, share, and modify!
