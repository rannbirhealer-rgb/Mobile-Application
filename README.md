# AlarmBoss 🚨

An advanced Android alarm application built using Jetpack Compose, Kotlin, and Material Design 3. AlarmBoss features strict challenges (Math, Lyrics, Barcodes, Reading, Pattern Lock, Word Scramble and Step Counter tasks) to ensure you wake up on time.

---

## 🎮 Progression

Every alarm you actually wake up to earns XP, so mornings become a game you can level up in.

- **XP and levels:** Easy, Medium and Strict alarms pay different XP, with bonuses for your streak and for finishing fast. Levels follow a square-root curve (level 2 at 50 XP, level 3 at 200 XP).
- **Wake-up streak:** consecutive days with a completed alarm. Miss a day and it restarts at 1.
- **Boss fights:** on every 7th day of a streak, Medium and Strict alarms swap their task for a three-stage boss fight (Math, then Sequence, then Maze) worth a flat 100 XP. Easy alarms stay one tap.
- **Adaptive difficulty:** Math and Maze tasks get harder as your level and streak grow (difficulty 1-10).
- **Stats tab:** level progress, streak, bosses defeated, records and a four-week wake-up calendar.
- **Avatars:** new ranks unlock at levels 2, 5 and 10 and can be equipped from the Stats tab.
- **Morning window:** a wake-up only advances the streak when it is completed between 5:00 and 10:00; outside that it still earns XP.
- **Morning quest:** after a real wake-up, optional checklist items (water, bed, stretch) earn bonus XP.

---

## ⏰ Scheduling

- **Repeating alarms keep repeating.** After one rings, the next occurrence is queued straight away; a one-time alarm switches itself off instead of sitting in the list looking active.
- **Clock and time-zone changes are handled.** Alarms are re-registered after a reboot, a time-zone change, a manual clock change, and whenever the app opens, so 7:00 stays 7:00 local.
- **Presets and dates.** One-tap Weekdays, Weekends and Every day, or pin an alarm to a single date. A pinned date and a repeat pattern are mutually exclusive, and a moment that has already passed can't be saved.
- **24-hour clock.** Times follow the system 12/24-hour setting everywhere they appear.
- **Two-hour lock.** An enabled alarm can't be edited, switched off or deleted once its next real ring is within two hours, so a weekday alarm isn't locked on a Saturday evening.

## 🔊 Sound

- **Loudness** of 25%, 50%, 75% or 100% on the alarm stream, with an optional **gradual fade-in**.
- **Raise a turned-down phone:** optionally lift the alarm volume to the chosen level for the ring, then put it back afterwards.

## 🛟 Safety nets

- **Emergency exits.** A locked alarm can still be switched off with a long press followed by a held button, and a ringing challenge has a deliberately slow Emergency stop, so the lock and the challenges can never trap someone who is ill or has made a mistake.
- **Wake-up check.** After a Medium or Strict alarm is completed, it rings again after 5, 10 or 15 minutes (your choice in Settings) with a quick challenge, because the commonest failure is passing the task and going back to sleep. It is always on for Medium and Strict alarms, can be switched on for an Easy alarm individually, and can be applied to every non-Easy alarm in Settings.
- **Backup alarm (optional).** If an alarm isn't completed, it rings again after 5, 10 or 15 minutes. Completing, snoozing or stopping the alarm cancels the backup.

## 🎨 Appearance

The original purple colour scheme, in **System default**, **Light** or **Dark**. Pick one under Settings, Appearance; the choice applies at once to every screen, including the ringing screen, and the status and navigation bar icons follow it.

## ♿ Accessibility

- **Puzzle instead of a workout.** A Strict alarm set to a physical exercise offers a mental puzzle in its place for the same reward.
- **Silent mode.** Vibrates and flashes the camera light instead of playing sound, with an optional flash on top of normal ringing.

## 👨‍👩‍👧 Parent Mode

For ages 9 to 12. A parent sets a four-digit PIN and a school-morning alarm, and the strict lock then guards the alarms.

- **One PIN dialog for everything guarded:** changing, switching off, deleting or adding an alarm, applying a template, skipping a holiday, ending a ringing alarm with the emergency stop, turning the lock off and leaving Parent Mode.
- **The PIN is stored as a salted hash**, never in the clear. After five wrong guesses entry is refused for 30 seconds, doubling up to 15 minutes, on a clock that changing the date cannot move.
- **Forgotten PIN:** there is no recovery by design, since anything a child could use would defeat the lock. Clearing the app's storage in Android Settings removes Parent Mode, and the alarms with it.

## 🌙 Sleep counselor

A chat tab with sleep advice that works entirely on the device. Optionally add your own Google Gemini API key in Settings for fuller answers and AI-written reading passages. Only then are the messages you send, and your passage requests, sent to Google, using your key. No key is built into the app.

## 🏖️ Holidays

Turn on **Holidays** in Settings and the alarm list asks, a week ahead, whether repeating alarms should stay quiet on the next holiday.

- **Nothing is skipped without a yes.** "Skip that day" skips it; "Keep ringing", or closing the dialog, changes nothing. One-time and pinned-date alarms are never skipped, and each holiday is asked about once.
- **Holidays are computed offline.** Holidays for Worldwide, India, the US and the UK are worked out on the device from fixed dates and weekday rules (including Easter), with no network request.
- **Your own days off.** Add festivals that depend on the lunar calendar, such as Diwali or Eid, which no rule can predict.
- **Can't be used to dodge the lock.** A skip is only offered while the alarm is more than two hours away, and an agreed skip can be cancelled from Settings at any time.

---

## 🚀 Automated Release Pipeline

This project uses **GitHub Actions** to automatically build and compile the application into a production-ready Android APK whenever a new version tag is published. 

### How to Trigger a New Production Build

Because the automation script is configured to look for version tags (e.g., `v1.0.0`), standard code pushes to the `main` branch will **not** trigger an APK build. 

When you are ready to publish a new version of the app, open your Command Prompt (CMD) and execute the following two commands:

#### 1. Create a Local Version Tag
Create a signed version tag on your current local branch:
```bash
git tag -a v1.0.0 -m "First automated release"
```
*(Feel free to update `v1.0.0` to your current version number, such as `v1.0.1` or `v2.0.0`)*

#### 2. Push the Tag to GitHub
Push the new tag to your remote GitHub repository to wake up the build runner:
```bash
git push origin v1.0.0
```

---

### 🔏 Signing the release

An unsigned APK cannot be installed, so the release build is always signed. To have every release signed with the same key, which lets users update in place, add these repository secrets (Settings, Secrets and variables, Actions): `KEYSTORE_BASE64` (the keystore file, base64-encoded), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. Without them the workflow signs with the debug key: the APK installs, but a build signed with a different key can only be installed after uninstalling the old one. The workflow runs the unit tests and lint before building.

### ⏱️ What Happens Next?

1. **Automation Trigger:** GitHub will immediately detect the new `v*` tag and spin up an isolated virtual environment to compile your code.
2. **Track the Progress:** Navigate to your repository page on the GitHub website and click the **Actions** tab at the top. You will see a live workflow log showing your Gradle environment compiling the APK.
3. **Download the App:** Once the build successfully completes, a brand new production **Release** will instantly appear on your GitHub dashboard. Your compiled `app-release.apk` file will be attached and ready for download onto any Android device!

