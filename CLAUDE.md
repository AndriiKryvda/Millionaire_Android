# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

"Як стати мільйонером" — a fully offline Ukrainian-language Android quiz game (Who Wants to Be a Millionaire style): 15 questions, prize ladder 1–1000 ₴, three lifelines. Kotlin + Jetpack Compose (Material 3), Hilt (KSP), Room, DataStore, kotlinx-serialization. minSdk 26, compileSdk/targetSdk 36. UI strings are Ukrainian only; portrait only; the app declares **zero permissions** — keep it that way (no network calls, no new permissions).

Full spec lives in `REQUIREMENTS.md` (FR-/NFR- references used in code comments) and `TECHNICAL_DESIGN.md`.

## Build & test

Always build through the wrapper script, not bare `gradlew` — it auto-selects a JDK 17–23 (Gradle 8.14.3 does **not** run on JDK 24+, and the system `java` here is too new):

```powershell
.\scripts\build-apk.ps1                        # tests + debug APK (default tasks)
.\scripts\build-apk.ps1 -Tasks testDebugUnitTest
.\scripts\build-apk.ps1 -Tasks assembleRelease
.\scripts\build-apk.ps1 -BumpVersion:$false    # build without bumping the version
```

The script bumps `appVersionName`/`appVersionCode` in `gradle.properties` before every build (single source of truth; `androidApp/build.gradle.kts` reads them into `BuildConfig`), copies APKs into `dist\` (gitignored) and regenerates `dist\index.html`, then publishes the newest APK as `releases\Android\millionaire-<version>.apk` and deletes older APKs there.

Committed build layout — each platform folder holds **only the latest** artifact:

```
releases/
├── Android/   # latest release APK: millionaire-<version>.apk
└── iOS/       # latest iOS build (produced on macOS; see releases/iOS/README.md)
```

After shipping a new APK, update the download link at the top of `README.md`.

Direct Gradle (after setting `JAVA_HOME` to a JDK 17–21):

```powershell
.\gradlew.bat testDebugUnitTest                # all JVM unit tests
.\gradlew.bat testDebugUnitTest --tests "com.vibecoding.millionaire.domain.game.GameEngineTest"   # single class
.\gradlew.bat assembleDebug
```

`local.properties` (gitignored) must contain `sdk.dir=...` pointing at the Android SDK. The Gradle wrapper must stay on `gradle-8.14.3-bin.zip` — an IDE may silently regenerate it for Gradle 9.x, which breaks AGP 8.13.2. Library versions are pinned in `gradle/libs.versions.toml`; do not bump them casually (newer libs may require AGP 9 / compileSdk 37).

Tests are plain JVM unit tests in `app/src/test` (no emulator). `QuestionBankIntegrityTest` reads the real `assets/questions/*.json` and enforces bank invariants (8000 questions, ≥50 per age×difficulty cell, balanced correct-letter distribution). Compose UI / instrumented tests are not set up.

## Question bank pipeline (Python)

`app/src/main/assets/questions/questions.json` + `answers.json` are **generated** — never edit them by hand. Sources: `tools/questions_src/part_XX.txt` (one question per line: `id|category|age:difficulty[,...]|text|A|B|C|D`) and `tools/answers.txt` (`id|letter`).

```powershell
python tools\build_questions.py                 # validate + regenerate assets
python tools\build_questions.py --assign-ids    # assign IDs to new ID-less lines
```

Critical rules:
- **Question IDs are permanent** — Room stores them to track seen questions per player. Never renumber or reuse an ID; when deleting a question, append its ID to `tools/retired_ids.txt`.
- New questions are written without an ID with the **correct answer first**; `--assign-ids` places options A–D balancing the correct letter across blocks of 4 IDs.
- Validation failures abort without overwriting the JSON. At runtime `QuestionBankParser` re-validates and skips corrupt entries with a log warning instead of crashing.
- Categories are a fixed set (see `CATEGORIES` in `tools/build_questions.py`); each cell of 4 age groups × 5 difficulties needs ≥50 questions.

Other generators (all stdlib-only Python):
- `python tools\build_sounds.py` — regenerates the six WAVs in `app/src/main/res/raw/`.
- `python tools\build_icon.py` — single source of truth for the adaptive launcher icon (writes the `ic_launcher_*.xml` vectors and PNG previews in `tools/icon/`); geometry/palette constants are at the top of the script.

## Architecture

MVVM + UDF with a clean, Android-free game core:

```
JSON assets → QuestionAssetLoader → QuestionBankParser → QuestionBank (age×difficulty index)
PlayerRepository (Room, seen-question history) → QuestionSelector → 15 questions
GameEngine (pure functions) → GameState → GameViewModel → GameScreen (Compose)
GameResultRepository (Room) → ResultScreen / RecordsScreen
```

- `domain/` must not depend on Compose/Room/DataStore (only `@StringRes` for age-group and fake-friend names). `GameEngine` is pure `(GameState, ...) -> GameState` — game rules changes go there and get JVM tests, not UI tests.
- Round phases are an explicit state machine: `AWAITING_ANSWER → CONFIRMING → REVEALING → ANSWER_CORRECT | ANSWER_WRONG → FINISHED` (select → confirm → verdict flow is deliberate; don't shortcut it).
- Per-screen packages under `ui/` each hold Screen + ViewModel; navigation is type-safe Navigation Compose routes in `ui/navigation/`.
- Static question data is cached in-process; settings in DataStore; players/results/seen-questions in Room (`millionaire.db`). A game in progress is not persisted — process death restarts the round (seen questions stay marked).

## Release signing

**Always increment the version before building a release APK** — Android refuses to install an APK whose `versionCode` is not higher than the installed one, so a release built at the same version as the last shipped APK cannot be installed over it. `scripts/build-apk.ps1` bumps `appVersionName`/`appVersionCode` automatically before each build; if you build via bare `gradlew.bat assembleRelease` (or pass `-BumpVersion:$false`), bump `gradle.properties` yourself first.

`assembleRelease` produces an **unsigned** APK unless `keystore.properties` (gitignored) exists in the repo root with `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (see README §6.4).

Every successful `scripts/build-apk.ps1` run also refreshes the committed Android artifact: the newest APK is copied to `releases\Android\millionaire-<version>.apk` and older APKs are deleted there, so the folder always holds exactly one file. iOS builds cannot be produced on this Windows host; when one is built on macOS, place it in `releases\iOS\` (keep only the newest) — see `releases\iOS\README.md`.
