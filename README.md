# Anatomy PDF

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="100" height="100" alt="Anatomy PDF App Icon"/>
</p>

Anatomy PDF is an Android application designed for medical students, doctors, and healthcare learners. It combines a fast PDF reader with an interactive anatomical atlas and an integrated AI study assistant.

Users can open lecture notes or medical textbooks, tap on anatomical structures directly on the page, see definitions and real medical diagrams, and ask questions to an AI assistant.

---

## Features

- PDF Reader: Smooth continuous scrolling, pinch to zoom up to 4.5x, and pan across document pages.
- Tap to Learn: Tap or double-tap words on the PDF to view immediate anatomical details, boundaries, relations, and clinical notes.
- In-App Medical Diagrams: View high-resolution anatomical drawings, diagrams, and cross-sections directly inside the app without leaving to a browser.
- AI Study Assistant: Chat with Gemini AI to ask medical questions, explain difficult concepts, or generate flashcards and quizzes.
- Markdown Support: Clean formatting for AI responses, including bold text, bullet points, headers, and tables.
- Document Library: Open built-in sample anatomy lectures or import your own PDF documents from device storage.
- Custom Bookmarks and Notes: Save important pages and highlight key terms for quick revision.

---

## Libraries Used

The project uses the following open-source libraries:

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose | BOM 2024.09.00 | Modern UI toolkit for building native Android interfaces |
| Material 3 | AndroidX Compose | Design system components, colors, typography, and theming |
| PDFBox Android | 2.0.27.0 | Extracts text and computes word coordinates for direct page tap selection |
| Android PdfRenderer | Native Android API | High-speed, hardware-accelerated PDF page rendering |
| Google AI Client SDK | 0.9.0 | Connects to the Gemini AI API for medical questions and explanations |
| Markwon | 4.6.2 | Formats Markdown in AI chat messages (bold, tables, lists, links) |
| Coil | 2.7.0 | Loads and caches anatomical diagrams and illustrations efficiently |
| Room Database | 2.6.1 | Local database storage for user notes, bookmarks, and document metadata |
| Kotlin Coroutines & Flow | 1.8.1 | Manages background tasks, image loading, and reactive UI state |
| Retrofit & OkHttp | 2.11.0 / 4.12.0 | Handles network calls and image search requests |

---

## Architecture Overview

The app follows standard Android Modern App Architecture (MVVM):

- UI Layer: Built entirely in Jetpack Compose. Screen composables observe state from ViewModels.
- State Management: Uses Kotlin StateFlow and Jetpack ViewModel to handle screen state during configuration changes.
- Data Layer:
  - `PdfDocumentManager`: Handles PDF file loading, rendering page bitmaps, and extracting text bounding boxes.
  - `AnatomyRepository`: Built-in local dictionary containing anatomical definitions, clinical notes, relations, and branches.
  - `AnatomyImageSearchService`: Fetches medical diagrams and Google search results.
  - `GeminiService`: Handles AI chat prompts, system instructions, and response streaming.

---

## Developer Guide

If you want to build, test, or contribute to this project, follow the steps below.

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Android SDK with API Level 34 (Android 14) installed
- A physical Android device or Android Emulator running Android 7.0 (API Level 24) or higher

### Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/anatomy-pdf.git
   cd anatomy-pdf
   ```

2. Open the project in Android Studio:
   - Select "Open an Existing Project".
   - Navigate to the cloned folder and select it.
   - Allow Gradle to sync dependencies.

3. Set up the Gemini API Key:
   - Obtain a free API key from Google AI Studio.
   - Add your API key to your local configuration or enter it directly in the app settings under "API Key".

4. Build and run the app:
   - Connect your Android device via USB debugging or start an emulator.
   - Click the "Run" button (green play icon) in Android Studio or run:
     ```bash
     gradle assembleDebug
     ```

### Project Directory Structure

```
anatomy-pdf/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/          # Data sources, PDF manager, repositories
│   │   │   │   ├── ui/            # Compose screens, components, dialogs, drawers
│   │   │   │   ├── viewmodel/     # Jetpack ViewModels and UI state
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/               # Vector drawables, icons, strings, colors
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml         # Version catalog for dependencies
├── LICENSE
└── README.md
```

### Contribution Guidelines

1. Fork the repository.
2. Create a new branch for your feature or bug fix:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Make your changes and test them thoroughly.
4. Commit your changes with clear commit messages:
   ```bash
   git commit -m "Add new anatomical flashcard quiz mode"
   ```
5. Push to your branch and open a Pull Request.

---

## ToDo

Here are the features and improvements planned for future releases:

- Offline Atlas Mode: Bundle high-resolution vector diagrams so all diagrams work without internet access.
- Stylus & Pen Drawing: Allow users to draw annotations, highlight text with a digital pen, and write handwritten notes directly on PDF pages.
- Audio Pronunciation: Add text-to-speech audio pronunciation for complex Latin and Greek anatomical names.
- 3D Anatomy Model Viewer: Embed lightweight interactive 3D anatomical models alongside 2D diagrams.
- Export Summaries: Export AI-generated summaries, key anatomical terms, and study notes as a new PDF or Markdown file.
- Multi-Language Translation: Translate anatomical structures and medical definitions into multiple languages.
- Flashcard Quiz Mode: Automatically turn document key terms into interactive flashcards with spaced repetition.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
