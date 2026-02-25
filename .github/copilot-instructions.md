# GitHub Copilot Instructions for Pager SMS Alarm App

## Project Overview
Pager is an Android application that receives SMS messages from group chats and triggers sound alarms based on specific messages.

## Current Architecture

### Core Components

1. **SoundAlarmManager** - Manages audio playback
   - `playSound()` - Plays a single beep
   - `playSoundSequence(count, intervalMs)` - Plays multiple beeps with intervals
   - `stopSound()` - Stops current audio playback
   - Uses MediaPlayer or similar audio API

2. **SmsBroadcastReceiver** - Receives SMS messages
   - Listens for incoming SMS broadcasts
   - Triggers `onMessageReceived` callback with sender and message body
   - Requires RECEIVE_SMS and READ_SMS permissions

3. **MainActivity** - Main activity
   - Initializes SoundAlarmManager
   - Sets up SMS receiver callback
   - Handles permission requests
   - Provides FAB for testing sound functionality

## Development Guidelines

### SMS Handling
- Always validate sender and message content before triggering alarms
- Log SMS events for debugging (use TAG: "MainActivity" or "SmsBroadcastReceiver")
- Handle permission requests gracefully for Android 6.0+ (API 23+)

### Sound Playback
- Expose methods like `playSound()` and `playSoundSequence()` from SoundAlarmManager
- Support customizable sound sequences (count, intervals)
- Properly release audio resources in cleanup
- Consider vibration patterns as alternative/complementary alerts

### UI/UX
- Show Snackbar notifications when SMS is received
- Display permission status and request dialogs
- Provide test buttons (like FAB) for testing sound

### Code Style
- Use Kotlin for all new code
- Follow Android Material Design guidelines
- Use ViewBinding for view references
- Use Coroutines for async operations where needed
- Proper logging with consistent TAG constants

### Testing Considerations
- Test with actual SMS or SMS emulator
- Verify permissions work on different Android versions
- Test sound playback on different devices
- Handle edge cases (no audio output, permissions denied, etc.)

## Future Features (To Be Implemented)
- Message filtering/group chat detection
- Custom alarm sound selection
- Alarm schedule/time-based rules
- Notification preferences
- User-configurable vibration patterns

## Important Files
- `MainActivity.kt` - Main entry point
- `SoundAlarmManager.kt` - Audio management
- `SmsBroadcastReceiver.kt` - SMS reception
- `AndroidManifest.xml` - App configuration and permissions
- `build.gradle.kts` - Dependencies and build config

## Key Dependencies
- androidx.appcompat (Material Design)
- androidx.navigation (Navigation framework)
- androidx.core (Permission handling, compatibility)
- com.google.android.material (Material components)

## Notes
- The app requires SMS permissions to function
- Audio playback may vary based on device audio settings
- Test thoroughly on different Android versions (especially 6.0+)

