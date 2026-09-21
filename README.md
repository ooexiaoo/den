# Den

A private, local-only task and note manager for Android.

- **Tasks & subtasks** with due dates, priorities, colors, and notification reminders
- **Notes** that link to each other (`[[title]]`) and show backlinks
- **Labels & tags** for both tasks and notes
- **Completion flow** — rate any task, add a reflection, attach a photo or video
- **Encrypted on device** — the database is encrypted with SQLCipher; media stays in private app storage
- **Encrypted backups** — automatic on a schedule or manual export/import, encrypted with your device key or a passphrase
- **Passcode lock** to guard the app

Everything lives locally. No account, no cloud, no tracking.

## Tech

- Kotlin + Jetpack Compose (Material 3)
- Room + SQLCipher via the Android Keystore-wrapped database key
- WorkManager (auto-backups, reminder rescheduling after reboot), AlarmManager (exact reminders)
- minSdk 26, targetSdk 36, R8 on release builds

## Build

```bash
./gradlew :app:assembleDebug
```

Release builds are signed automatically in CI via GitHub secrets
(`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). See
`.github/workflows/build-apk.yml`.

## License

MIT — see [LICENSE](LICENSE).