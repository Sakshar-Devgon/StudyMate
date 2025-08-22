# StudyMate - AI-Powered Learning Application

StudyMate is an Android application that transforms study materials into interactive flashcards and quizzes using Google Gemini AI API. Built with Kotlin, Jetpack Compose, and Firebase.

## Features

- 🤖 **AI-Powered Content Generation**: Convert study materials into flashcards using Google Gemini AI
- 📚 **Dual Study Modes**: Flashcard review and interactive quiz testing
- 🔄 **Real-time Sync**: Firebase integration for cloud synchronization
- 📱 **Modern UI**: Material Design 3 with dark/light theme support
- 📊 **Progress Tracking**: Detailed analytics and performance metrics
- 🔒 **Secure Authentication**: Firebase Authentication for user management
- 💾 **Offline Support**: Room Database for offline functionality

## Screenshots

[Add your app screenshots here]

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository Pattern
- **Database**: Room Database (local), Firebase Firestore (cloud)
- **Authentication**: Firebase Authentication
- **AI Integration**: Google Gemini AI API
- **HTTP Client**: OkHttp3
- **Navigation**: Navigation Component

## Setup Instructions

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24 or higher
- Google account for Firebase setup
- Google AI Studio account for Gemini API

### 1. Clone the Repository

```bash
git clone https://github.com/Sakshar-Devgon/StudyMate.git
cd StudyMate
```

### 2. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Add an Android app with package name: `com.beast.studymate`
4. Download `google-services.json` file
5. Replace the template `app/google-services.json` with your downloaded file
6. Enable Authentication and Firestore in Firebase Console

### 3. API Keys Configuration

1. Get your Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Open `app/src/main/res/values/api_keys.xml`
3. Replace the placeholder with your actual API key:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="gemini_api_key">YOUR_ACTUAL_API_KEY_HERE</string>
</resources>
```

**Note**: The current files contain placeholder values that will work for building but won't connect to actual services.

### 4. Build and Run

1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Build and run the application

## Project Structure

```
app/
├── src/main/java/com/beast/studymate/
│   ├── auth/                 # Authentication logic
│   ├── database/             # Room database entities and DAOs
│   ├── repository/           # Repository pattern implementation
│   ├── ui/
│   │   └── Screens/          # Compose UI screens
│   └── MainActivity.kt       # Main activity
├── src/main/res/
│   ├── values/
│   │   ├── api_keys.xml      # API keys (not in repo)
│   │   └── api_keys_template.xml  # Template for API keys
│   └── ...
└── google-services.json     # Firebase config (not in repo)
```

## Key Components

### FlashcardScreen
- Main screen for flashcard generation and display
- Integrates with Gemini AI API for content generation
- Supports custom instructions for flashcard creation

### AuthViewModel
- Handles user authentication with Firebase
- Manages user sessions and profile data

### StudyMateDatabase
- Room database for offline storage
- Stores quiz history and user progress

## Security

This project follows security best practices:

- API keys are stored in separate configuration files
- Sensitive files are excluded from version control
- Firebase configuration is externalized

### Automatic Security

The `.gitignore` file is configured to automatically exclude sensitive files:
- `app/google-services.json` (your Firebase config)
- `app/src/main/res/values/api_keys.xml` (your API keys)

This means you can push to GitHub normally without worrying about exposing your credentials!

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

**Sakshar Devgon**
- Email: Sakshardevgon08@gmail.com
- GitHub: [@Sakshar-Devgon](https://github.com/Sakshar-Devgon)
- LinkedIn: [sakshar-devgon](https://www.linkedin.com/in/sakshar-devgon-029568250/)

## Acknowledgments

- Google Gemini AI for content generation
- Firebase for backend services
- Material Design team for UI components
- Android Jetpack Compose team

---

⭐ If you found this project helpful, please give it a star!
