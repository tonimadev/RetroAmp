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

## AGSL Shaders & Visualizers

RetroAmp leverages the power of modern Android graphics to create a highly responsive and visually stunning experience.

- **Language**: The project uses **Android Graphics Shading Language (AGSL)** for high-performance GPU rendering, allowing for complex per-pixel calculations.
- **Shader Management**: Shaders are stored as string constants in `Shaders.kt` for easy access and organization.
- **Implementation**: It uses Compose's `RuntimeShader` and `ShaderBrush` integrated via `drawBehind` or `Canvas` to apply the graphics directly to the UI layer.
- **Reactivity**: Shaders receive real-time uniforms to stay in sync with the audio:
    - `uTime`: For continuous animation and movement within the shader.
    - `uAmplitude`: Mapped to audio intensity (bass-boosted and smoothed) to drive distortion, frequency shifts, and color transitions.
- **Interactions**:
    - **Single Click**: Toggle between different visualizer modes (e.g., Psychedelic Waves, Audio Tunnel).
    - **Long Click**: Enter/Exit Full-Screen visualizer mode for an immersive experience.
- **Performance**: Rendering happens entirely on the GPU, ensuring a smooth 60/120 FPS experience without taxing the UI thread or blocking main thread execution.

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
