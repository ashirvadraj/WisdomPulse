# ✨ WisdomPulse - Quotes of Legends & WhatsApp Wallpaper Studio

[![Download APK](https://img.shields.io/badge/Download-WisdomPulse.apk-2ECC71?style=for-the-badge&logo=android)](https://github.com/ashirvadraj/WisdomPulse/raw/main/WisdomPulse.apk)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)](https://kotlinlang.org)
[![Material Design](https://img.shields.io/badge/Material-3-purple.svg)](https://m3.material.io)

> **"Dream is not that which you see while sleeping, it is something that does not let you sleep."**  
> — *Dr. A.P.J. Abdul Kalam*

WisdomPulse is a modern, offline-first Android application featuring timeless quotes and poetry from iconic world leaders, philosophers, and visionaries, including **Dr. A.P.J. Abdul Kalam**, **Atal Bihari Vajpayee**, **Swami Vivekananda**, **Steve Jobs**, **Albert Einstein**, and more.

---

## 📲 Direct APK Download

You can download the pre-built APK directly and install it on your phone:
👉 **[Download WisdomPulse.apk (5.9 MB)](https://github.com/ashirvadraj/WisdomPulse/raw/main/WisdomPulse.apk)**

---

## 🚀 Key Features

### 1. 📖 Curated Legends & Poetry Database
- **Dr. A.P.J. Abdul Kalam**: Vision, youth empowerment, perseverance, and handling failure.
- **Atal Bihari Vajpayee**: Dedicated bilingual corner featuring iconic poems:
  - *गीत नया गाता हूँ* (Geet Naya Gata Hoon)
  - *कदम मिलाकर चलना होगा* (Kadam Milakar Chalna Hoga)
  - *हार नहीं मानूँगा, रार नहीं ठानूँगा* (Haar Nahi Manunga)
  - *मौत से ठन गई!* (Maut Se Than Gayi)
- **Swami Vivekananda**: Fearlessness, inner strength, and purpose.
- **Global Visionaries**: Albert Einstein, Steve Jobs, Nelson Mandela, Mahatma Gandhi, Dr. B.R. Ambedkar, Chanakya, Bhagat Singh, Marcus Aurelius.
- **Smart Filters & Instant Search**: Filter by author chip or search through quote text, category, or emotions in real-time.

### 2. 🎨 9:16 Wallpaper Studio (Phone & WhatsApp Wallpaper)
- **9:16 Aspect Ratio Canvas**: Perfectly proportioned for smartphone screens and WhatsApp chat backgrounds.
- **8 Dynamic Aesthetic Themes**:
  - `Midnight OLED` (Pure AMOLED black `#0B0C10` with golden typography)
  - `Royal Gold` (Warm luxury palette)
  - `Crimson Dusk` (Deep burgundy elegance)
  - `Forest Zen` (Calming deep emerald)
  - `Deep Indigo` (Midnight blue)
  - `Sunset Ember` (Warm radiant gradient)
  - `Vintage Parchment` (Classic reading paper aesthetic)
- **1-Tap System Integrations**:
  - **Set as Phone Wallpaper**: Uses native `WallpaperManager` to apply directly to device Home and Lock screen.
  - **Save for WhatsApp Wallpaper**: Renders high-res 1080x1920 PNG to gallery (`Pictures/WisdomPulse`) and opens WhatsApp's wallpaper picker.

### 3. ⏰ Offline Daily Notification Engine
- **Scheduled Morning Reminder**: Fires daily at 7:00 AM using Android `AlarmManager` without requiring any cloud server or internet.
- **"Inspire Me Now" Button**: Instant on-demand inspiration notification with 1-tap "Set Wallpaper" action.

### 4. 🔊 Text-to-Speech (TTS) Voiceover
- Native `android.speech.tts.TextToSpeech` integration.
- Reads English quotes and Hindi poems with accurate regional accent.

### 5. ❤️ Offline Bookmarks
- Tap the heart icon to save favorite quotes locally with zero network latency.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 1.9.24
- **Architecture:** Android Jetpack & ViewBinding
- **UI Components:** Google Material Components 3 (`Theme.Material3.Dark.NoActionBar`)
- **JSON Parsing:** Google Gson 2.10.1
- **Notifications:** Android `NotificationManager` + `AlarmManager` + `BroadcastReceiver`
- **Wallpaper API:** Android `WallpaperManager` + `MediaStore`

---

## 📦 Building the APK from Source

```bash
# Clone the repository
git clone https://github.com/ashirvadraj/WisdomPulse.git
cd WisdomPulse

# Build Debug APK
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
MIT License. Free to use, share, and modify!
