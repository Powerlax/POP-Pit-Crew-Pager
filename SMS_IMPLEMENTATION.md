# SMS Alarm Implementation

## Overview
This app monitors incoming SMS messages and can play sound alarms in response. The implementation includes two main components:

## Components

### 1. SmsBroadcastReceiver
**File:** `SmsBroadcastReceiver.kt`

A BroadcastReceiver that intercepts incoming SMS messages.

**Features:**
- Automatically receives all incoming SMS messages
- Extracts sender address and message body
- Provides a callback mechanism (`onMessageReceived`) for handling messages

**Usage:**
```kotlin
SmsBroadcastReceiver.onMessageReceived = { sender, messageBody ->
    // Your custom logic here
    // Filter by sender, check message content, etc.
}
```

### 2. SoundAlarmManager
**File:** `SoundAlarmManager.kt`

Manages sound playback for alarms using MediaPlayer.

**Public Methods:**

#### `playSound(soundUri: Uri? = null, volume: Float = 1.0f)`
Plays a single sound alarm.
- `soundUri`: URI of sound to play (null = default notification sound)
- `volume`: 0.0 to 1.0

#### `playSoundSequence(soundUri: Uri? = null, count: Int = 3, intervalMs: Long = 500, volume: Float = 1.0f)`
Plays a sequence of sounds with intervals.
- `soundUri`: URI of sound to play (null = default notification sound)
- `count`: Number of times to play the sound
- `intervalMs`: Interval between sounds in milliseconds
- `volume`: 0.0 to 1.0

#### `playCustomSound(resourceId: Int, volume: Float = 1.0f)`
Plays a custom sound from app resources (res/raw folder).
- `resourceId`: Resource ID from R.raw
- `volume`: 0.0 to 1.0

#### `stopSound()`
Stops any currently playing sound.

#### `isPlaying(): Boolean`
Returns true if a sound is currently playing.

**Usage Examples:**
```kotlin
val soundManager = SoundAlarmManager.getInstance(context)

// Play default notification sound once
soundManager.playSound()

// Play a sequence of 5 sounds with 1 second interval
soundManager.playSoundSequence(count = 5, intervalMs = 1000)

// Play custom sound from res/raw/alarm.mp3
soundManager.playCustomSound(R.raw.alarm, volume = 0.8f)

// Stop all sounds
soundManager.stopSound()
```

## Current Implementation in MainActivity

The app currently:
1. Requests SMS permissions on startup
2. Sets up a callback that triggers when ANY SMS is received
3. Plays a 3-sound sequence (500ms intervals) when SMS arrives
4. Shows a Snackbar with the sender's information
5. The FAB button plays a test sound

## Next Steps (For You to Implement)

### 1. Filter by Group Chat
Add logic to filter messages from specific group chats:

```kotlin
SmsBroadcastReceiver.onMessageReceived = { sender, messageBody ->
    // Example: Filter by sender address pattern
    if (sender.contains("your-group-identifier") || messageBody.contains("keyword")) {
        soundAlarmManager.playSoundSequence(count = 3, intervalMs = 500)
    }
}
```

### 2. Add Custom Sound Files
1. Create a `raw` folder: `app/src/main/res/raw/`
2. Add your sound files (e.g., `alarm.mp3`, `alert.ogg`)
3. Use them: `soundManager.playCustomSound(R.raw.alarm)`

### 3. Add UI Controls
Consider adding UI elements to:
- View SMS permission status
- Configure which senders/groups trigger alarms
- Test different sound patterns
- Adjust volume settings
- Enable/disable alarm functionality

### 4. Persistent Settings
Use SharedPreferences to save:
- Approved group chat identifiers
- Sound preferences
- Volume settings
- Enable/disable state

## Permissions

The app requires these permissions (already added to AndroidManifest.xml):
- `RECEIVE_SMS`: To receive incoming SMS messages
- `READ_SMS`: To read SMS message content

These are dangerous permissions requiring runtime approval (API 23+).

## Testing

1. **Permission Testing**: Launch app and grant SMS permissions
2. **Sound Testing**: Tap the FAB button to play a test sound
3. **SMS Testing**: Send an SMS to the device and verify:
   - Log messages in Logcat (tag: "SmsBroadcastReceiver")
   - Sound alarm plays
   - Snackbar notification appears

## Notes

- The `SoundAlarmManager` is a singleton to prevent multiple instances
- Sounds are cleaned up automatically on completion
- The receiver has high priority (2147483647) to intercept messages early
- All sounds stop when MainActivity is destroyed

