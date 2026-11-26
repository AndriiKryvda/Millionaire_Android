---
name: build-and-release
description: Build and publish release artifacts for "Як стати мільйонером" (KMP quiz app). Use when asked to build an APK or iOS build, bump the version, cut a release, or update the releases/ folder structure.
---

# Build & Release — "Як стати мільйонером"

## Release folder structure (committed artifacts)

```
releases/
├── Android/   # ONLY the latest release APK: millionaire-<version>.apk
└── iOS/       # ONLY the latest iOS build (produced on macOS; see releases/iOS/README.md)
```

Rules:
- Each platform folder holds **exactly one** artifact — the latest build. Delete/overwrite the old one when publishing a new build (the build script already does this for Android).
- `.gitignore` ignores `*.apk` globally but un-ignores `releases/**/*.apk`, so files under `releases/` are committed.
- `dist/` is a gitignored local drop folder (timestamped APKs + `index.html` download page) — never commit it.

## Android build flow (Windows)

Always build through the wrapper script — it auto-selects a JDK 17–23 (system `java` is too new for Gradle 8.14.3):

```powershell
.\scripts\build-apk.ps1                        # bump version + tests + debug APK
.\scripts\build-apk.ps1 -BumpVersion:$false    # rebuild WITHOUT bumping the version
.\scripts\build-apk.ps1 -Tasks assembleRelease # release APK (unsigned unless keystore.properties exists)
```

The script:
1. Bumps `appVersionName`/`appVersionCode` in `gradle.properties` (single source of truth) unless `-BumpVersion:$false`.
2. Runs Gradle (default tasks: `testDebugUnitTest` + `assembleDebug`).
3. Publishes timestamped APKs to `dist\` and regenerates `dist\index.html`.
4. Copies the newest APK to `releases\Android\millionaire-<version>.apk` and deletes older APKs there.

Version rules:
- `gradle.properties` (`appVersionName` like `0.1.6`, `appVersionCode` integer) is the only version source.
- Android refuses to install an APK whose `versionCode` is not higher than the installed one — bump before shipping a new release, unless intentionally rebuilding the same version that has not shipped yet.

## iOS build flow (macOS only)

iOS builds cannot be produced on this Windows host. On macOS, build the iOS app that wraps the Compose Multiplatform `:shared` module, then:
1. Place the artifact (`.ipa`/`.zip`) in `releases/iOS/`, keeping only the newest file.
2. Update the download links in the root `README.md`.

## After every release

1. Verify the artifact: `releases/Android/millionaire-<version>.apk` exists, has a recent timestamp and a sane size (~13 MB debug build).
2. Update the download link at the top of `README.md` (`**Завантажити APK:** [millionaire-<version>.apk](releases/Android/millionaire-<version>.apk)`).
3. Commit the new artifact, the deleted old one, and the README/`gradle.properties` changes.

## Gotchas

- Gradle wrapper must stay on `gradle-8.14.3-bin.zip` (Gradle 9.x breaks AGP 8.13.2).
- `assembleRelease` is unsigned without `keystore.properties` — ship debug APKs unless signing is configured.
- Never edit `androidApp/src/main/assets/questions/*.json` by hand — regenerate with `tools/build_questions.py`.