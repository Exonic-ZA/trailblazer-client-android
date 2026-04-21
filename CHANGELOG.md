# Changelog

## 2026-04-21

- Removed Sentry Gradle plugin configuration from `app/build.gradle`.
- Removed Sentry DSN and tracing metadata from `app/src/main/AndroidManifest.xml`.
- Removed Sentry event/exception reporting from:
  - `app/src/main/java/org/traccar/client/trailblazer/network/RequestManager.kt`
  - `app/src/main/java/org/traccar/client/trailblazer/ui/Trailblazer.kt`
- Cleaned `.gitignore` by removing `sentry.properties` ignore entry.
- Bumped Android app `versionCode` from `95` to `96`.
