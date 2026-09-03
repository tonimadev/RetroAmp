# RetroAmp

A nostalgic Winamp-inspired MP3 player for Android.

## Key Features

- **Winamp 2.x Aesthetic**: Custom Jetpack Compose components including nostalgic buttons and segmented displays.
- **Psychedelic Visualizer**: High-performance GPU rendering using AGSL (Android Graphics Shading Language) reacting to real-time audio amplitude.
- **Modern Media Engine**: Powered by Media3 (ExoPlayer) with full background playback support.
- **Playlist Persistence**: Automatically saves and restores your playlist using Room Database.
- **Clean Architecture**: Strictly follows the MVI (Model-View-Intent) pattern for predictable state management.
- **Immersive Experience**: Full edge-to-edge UI support for a modern look on Android devices.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Graphics**: AGSL (Android Graphics Shading Language)
- **Media**: Media3 (ExoPlayer)
- **Database**: Room
- **Concurrency**: Coroutines & Flow
- **Design System**: Material 3

## Architecture

RetroAmp is built using the **MVI (Model-View-Intent)** architectural pattern to ensure a robust and testable codebase:

- **UiState**: A single, immutable object representing the current state of the UI.
- **Intents**: User actions (like clicking "Play") or system events that are dispatched to the ViewModel.
- **Effects**: Side effects (like showing a Toast or navigating) that happen once and aren't part of the persistent UI state.
- **ViewModel**: The central hub that processes Intents, updates the UiState, and manages communication with the Media3 controller and Room database.

## How to Use

1. **Add Music**: Tap the "Eject" or "Add" button to open the Storage Access Framework (SAF) and select your MP3 files.
2. **Play**: Hit the classic Play button to start your music.
3. **Control**: Use the transport controls to skip tracks, pause, or stop playback.
4. **Volume & Progress**: Interact with the sliders to adjust the volume and seek through your tracks.
5. **Enjoy the Visuals**: The psychedelic visualizer will automatically sync with the audio frequency and amplitude.

## Screenshots

*(Place screenshots of the Winamp-style UI and AGSL visualizer here)*

## License & Disclaimer

This project is created for **educational and study purposes** only. It is an homage to classic media players and is not affiliated with the original Winamp developers.
