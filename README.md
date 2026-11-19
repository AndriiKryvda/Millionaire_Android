# Як стати мільйонером

Офлайн-вікторина для Android у стилі телевізійної гри: 15 питань, сходинка призів від 1 до
1 000 гривень і три підказки. Застосунок не потребує інтернету, не має жодних дозволів і
зберігає всі дані лише на пристрої.

Повний опис вимог — у [REQUIREMENTS.md](REQUIREMENTS.md), архітектурні рішення —
у [TECHNICAL_DESIGN.md](TECHNICAL_DESIGN.md).

---

## 1. Можливості

| Можливість | Деталі |
| --- | --- |
| Ігровий процес | 15 питань, 4 варіанти відповіді, спочатку вибір → потім «Підтвердити» → потім вирок |
| Сходинки призів | 1, 2, 3, 5, 10, 15, 25, 50, 75, 100, 150, 250, 400, 600, 1 000 ₴ |
| Гарантовані суми | 5-те (10 ₴) і 10-те (100 ₴) питання — не згорають після помилки |
| Підказки | `50:50`, «Думка зали», «Дзвінок другу» — по одному разу за гру |
| Питання | 1 000 питань, 4 вікові групи (10–12, 13–14, 15–18, 19–60), 5 рівнів складності |
| Без повторів | гра бере питання, яких гравець ще не бачив; історію можна скинути |
| Гравці | кілька профілів на пристрої, ім'я 2–16 символів, унікальне |
| Рекорди | топ-100 результатів, фільтр за віковою групою |
| Налаштування | звук, вібрація, скидання історії питань |
| Офлайн | жодного дозволу, жодного мережевого запиту, `allowBackup=false` |
| Мова й орієнтація | тільки українська, тільки портрет |

## 2. Екрани

| Екран | Призначення |
| --- | --- |
| Splash | завантаження бази питань, авто-вхід останнього гравця |
| Гравці | список профілів, створення / редагування / видалення |
| Створення гравця | ім'я + вікова група з валідацією |
| Меню | вітання, доступні питання, статистика, перехід до гри |
| Гра | питання, 4 варіанти, сходинка призів, підказки, «Забрати» |
| Результат | виграш, статистика раунду, правильна відповідь на помилковому питанні |
| Рекорди | топ результатів |
| Правила | стислий опис правил |
| Налаштування | звук, вібрація, історія, «Про застосунок» |

## 3. Технології

| Компонент | Версія |
| --- | --- |
| Мова | Kotlin 2.2.21 |
| UI | Jetpack Compose (BOM 2025.09.01) + Material 3, власна тема |
| Архітектура | MVVM + UDF, чисте ядро гри без Android-API |
| DI | Hilt 2.58 (KSP) |
| Навігація | Navigation Compose 2.9.8 (type-safe routes) |
| БД | Room 2.8.5 (`millionaire.db`) |
| Налаштування | DataStore Preferences 1.1.7 |
| JSON | kotlinx-serialization-json 1.9.0 |
| Збірка | Gradle 8.14.3, AGP 8.13.2, KSP 2.2.21-2.0.5 |
| SDK | compileSdk/targetSdk 36, minSdk 26 (Android 8.0+) |

## 4. Структура проєкту

```
app/src/main/java/com/vibecoding/millionaire/
├── MainActivity.kt              # єдина активність, Compose + NavHost
├── MillionaireApp.kt            # @HiltAndroidApp
├── di/AppModule.kt              # application-scoped CoroutineScope
├── domain/
│   ├── model/                   # Question, Player, GameResult, QuestionBank
│   ├── game/                    # PrizeLadder, QuestionSelector, GameEngine,
│   │                            # LifelineEngine, NicknameValidator, Friends
│   ├── repository/              # інтерфейси репозиторіїв
│   └── usecase/                 # StartGame, CheckGameAvailability, SaveGameResult, ...
├── data/
│   ├── question/                # DTO, QuestionBankParser, QuestionAssetLoader
│   ├── local/                   # Room: entities, DAO, AppDatabase
│   └── repository/              # реалізації + Hilt-модуль
└── ui/
    ├── theme/                   # кольори, форми (гексагон відповідей), типіка
    ├── common/                  # ScreenScaffold, кнопки, діалоги
    ├── navigation/              # маршрути + AppNavHost
    ├── feedback/                # SoundPool + Vibrator
    └── splash|players|player|menu|game|result|records|rules|settings/

app/src/main/assets/questions/   # questions.json + answers.json (згенеровані)
app/src/main/res/raw/            # звукові ефекти (згенеровані)
tools/                           # генератори питань і звуків (Python)
scripts/build-apk.ps1            # збірка APK одним кроком
```

## 5. Вимоги до середовища

1. **JDK 17–21** (рекомендовано 21). Gradle 8.14.3 не запускається на JDK 24+, тому
   системний `java` у 27 версії для збірки не підходить.
2. **Android SDK** з `platforms/android-36`. AGP сам завантажить потрібні компоненти,
   якщо прийняті ліцензії (`sdkmanager --licenses`).
3. **Android Studio не обов'язковий** — достатньо SDK і JDK.
4. **Python 3.10+** — лише якщо хочете перегенерувати базу питань або звуки.

Шлях до SDK читається з `local.properties` (`sdk.dir=...`). Файл не потрапляє в git —
створіть його після клонування:

```powershell
# створіть local.properties, якщо його ще немає
echo "sdk.dir=$($env:LOCALAPPDATA -replace '\\','\\' -replace '^([A-Za-z]):','$1\:')\Android\Sdk" > local.properties
```

## 6. Збірка APK

### 6.1 Один крок (рекомендовано)

```powershell
.\scripts\build-apk.ps1                                  # тести + debug APK
.\scripts\build-apk.ps1 -Tasks assembleRelease           # лише релізний APK
.\scripts\build-apk.ps1 -Tasks testDebugUnitTest
.\scripts\build-apk.ps1 -JdkHome 'C:\Program Files\Java\jdk-21'
```

Скрипт сам шукає підходящий JDK 17–21 (`JAVA_HOME` → Android Studio `jbr` →
`C:\Program Files\Android\openjdk` → `C:\Program Files\Java` → `~\.gradle\jdks`), виставляє
`JAVA_HOME`, `ANDROID_HOME` і запускає Gradle wrapper.

Після успішної збірки скрипт копіює свіжі APK у `dist\` з версією в назві
(`millionaire-debug-0.1.1-20261120-0113.apk`) і переписує `dist\index.html` — сторінку з
переліком усіх APK у `dist`, найновіший зверху.

#### Версія збірки

Версія живе у `gradle.properties` (`appVersionName` / `appVersionCode`) — це єдине джерело
правди, звідки її бере `app/build.gradle.kts`, а далі `BuildConfig.VERSION_NAME` (видно в
меню гри та в «Налаштування → Про застосунок»). Перед кожною збіркою скрипт піднімає
патч на один (`0.1.0` → `0.1.1`) і `appVersionCode` на один. Зібрати без зміни версії:

```powershell
.\scripts\build-apk.ps1 -BumpVersion:$false
```

### 6.2 Вручну

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'   # будь-який JDK 17-21
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

.\gradlew.bat clean
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat assembleRelease
```

### 6.3 Де лежать APK

| Збірка | Файл | Розмір | Призначення |
| --- | --- | --- | --- |
| Debug | `app\build\outputs\apk\debug\app-debug.apk` | ~12,6 МБ | перевірка на своєму пристрої |
| Release без підпису | `app\build\outputs\apk\release\app-release-unsigned.apk` | ~1,6 МБ | потребує підпису |
| Release підписаний | `app\build\outputs\apk\release\app-release.apk` | ~1,6 МБ | для поширення |

Релізний APK менший у ~8 разів, бо R8 мініфікує код і стискає ресурси.
База зі 1000 питань (550 КБ JSON) і звуки лежать усередині APK, тому гра працює без мережі.

### 6.4 Підпис релізного APK

1. Створіть ключ:

   ```powershell
   keytool -genkeypair -v -keystore millionaire.keystore -alias millionaire `
     -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Створіть `keystore.properties` у корені проєкту (файл доданий у `.gitignore`):

   ```properties
   RELEASE_STORE_FILE=../millionaire.keystore
   RELEASE_STORE_PASSWORD=ваш_пароль
   RELEASE_KEY_ALIAS=millionaire
   RELEASE_KEY_PASSWORD=ваш_пароль
   ```

   Можна передати й прапорцями: `.\gradlew.bat assembleRelease -PRELEASE_STORE_FILE=...`.

3. `.\gradlew.bat assembleRelease` — тепер APK буде підписаним. Без цих властивостей
   `assembleRelease` чесно створює **непідписаний** APK, який Android відмовиться встановлювати.

### 6.5 Встановлення на пристрій

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 7. Тести

```powershell
.\gradlew.bat testDebugUnitTest
```

У поточній збірці **47 тестів, 0 падінь**. JVM-тести не потребують пристрою і
покривають усю ігрову логіку:

| Тест | Що перевіряє |
| --- | --- |
| `PrizeLadderTest` | сходи призів, гарантовані суми, «забрати» та «помилка» |
| `GameEngineTest` | вибір → підтвердження → вирок, 50:50, зал, друг, перемога на 15-му питанні |
| `QuestionSelectorTest` | 3 питання на рівень складності, пріоритет unseen, fallback, «немає питань» |
| `LifelineEngineTest` | 50:50 лишає правильну, голоси зали = 100 %, друг не вигадує варіантів |
| `NicknameValidatorTest` | 2–16 символів, українські/латинські літери, апострофи, заборонені символи |
| `QuestionBankTest` | індекс `вік × складність`, підрахунок доступних питань |
| `QuestionBankIntegrityTest` | читає реальні `assets/questions/*.json`: 1 000 питань без пропущених, ≥ 50 на кожну комірку, рівномірний розподіл правильних літер |

Не покрито: Compose UI та instrumented тести — вони потребують емулятора або фізичного
пристрою (`.\gradlew.bat connectedDebugAndroidTest`). У середовищі збірки емулятора не було,
тому візуальну частину перевіряйте на телефоні.

## 8. База питань

Джерела питань — текстові файли `tools/questions_src/part_XX.txt` разом із `tools/answers.txt`.
Формат і правила описані в докстрінгу `tools/build_questions.py`.

```powershell
python tools\build_questions.py
```

Скрипт:

1. збирає всі частини,
2. валідує (унікальність id, 4 варіанти, довжина тексту, оцінки `вік → складність`,
   наявність правильної відповіді, покриття кожної комірки `4 віки × 5 складностей ≥ 50`,
   розподіл правильних літер),
3. зберігає `app/src/main/assets/questions/questions.json` і `answers.json`.

Якщо валідація падає — файли не перезаписуються. Під час запуску застосунку
`QuestionBankParser` повторно перевіряє кожне питання та пропускає псутий запис із
попередженням у Logcat замість краху гри.

## 9. Звукові ефекти

```powershell
python tools\build_sounds.py
```

Генерує шість коротких WAV-файлів (`click`, `select`, `correct`, `wrong`, `win`, `walkaway`)
без зовнішніх бібліотек і завантажень — усе синтезується локально.

## 10. Іконка застосунку

```powershell
python tools\build_icon.py
```

Єдине джерело правди — `tools/build_icon.py`. З нього генеруються векторні шари
adaptive-іконки (`app/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml`
та `app/src/main/res/mipmap-anydpi-v26/ic_launcher{,_round}.xml`) і PNG-превью в
`tools/icon/`: разом, окремо шари, безпечна зона 66/72 dp та прикидки під круглою
і squircle-маскою. Знак ₴ побудований замкненими контурами, тому той самий код дає
і растер, і `pathData` у VectorDrawable. Геометрія та палітра — константи вгорі скрипту
(`HEX_R_OUT`, `GLYPH_SCALE`, `GLYPH_STOPS`, `RINGS`); після правки достатньо перезапустити
скрипт і перезібрати APK.

## 11. Архітектура

```
JSON (assets) ──> QuestionAssetLoader ──> QuestionBankParser ──> QuestionBank (індекс)
                                                                    │
PlayerRepository (Room) ── seen question ids ──> QuestionSelector ──┘
                                                                    │ 15 питань
                                                        GameEngine (чисті функції)
                                                                    │ GameState
                                                    GameViewModel ─┴─ GameScreen (Compose)
                                                                    │
                                            GameResultRepository (Room) ──> ResultScreen
```

- `domain/` не знає про Compose, Room чи DataStore; єдиний зв'язок з Android —
  `@StringRes` для назв вікових груп і вигаданих друзів.
- `GameEngine` — набір чистих функцій `(GameState, ...) -> GameState`, тому правила легко
  тестувати та важко зламати випадковим тапом.
- Фази раунду: `AWAITING_ANSWER → CONFIRMING → REVEALING → ANSWER_CORRECT | ANSWER_WRONG → FINISHED`.
- Статичні дані (питання) кешуються в пам'яті за-process; налаштування — DataStore;
  гравці, результати та «бачені» питання — Room.

## 12. Приватність і дозволи

- у вихідному `AndroidManifest.xml` немає жодного `<uses-permission>`, тому застосунок не має мережевого доступу;
- єдиний службовий запис у зібраному APK — дозвіл імені самого застосунку
  (`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, рівень signature), який автоматично додає
  AndroidX Core. Жодної системної permission (мережа, сховище, камера тощо) застосунок не запитує;
- `android:allowBackup="false"`, тому дані не копіюються в резервні копії Android;
- всі дані живуть у `millionaire.db` і `datastore` у приватній директорії застосунку;
  «Видалити дані» в системних налаштуваннях стирає гравців і рекорди;
- мова інтерфейсу — українська, орієнтація — лише портрет.

## 13. Відомі обмеження

- Раунд не відновлюється після вбивства процесу: гра почнеться заново (вже бачені питання
  при цьому залишаються відміченими).
- Рекорди зберігаються локально, між пристроями не синхронізуються.
- Релізний APK без `keystore.properties` лишається непідписаним.
- Compose UI-тести не запускалися через відсутність емулятора в середовищі збірки.
- Питання згенеровані автоматично — перед публікацією варто вибірково вичитати факти.

## 14. Типові проблеми

| Симптом | Причина і рішення |
| --- | --- |
| `Plugin 'com.android.internal.application' relies on InternalProblems ... removed in Gradle 9.6` | IDE перегенерувала wrapper на Gradle 9.x. У `gradle/wrapper/gradle-wrapper.properties` має бути `gradle-8.14.3-bin.zip` |
| Gradle не стартує, `Unsupported class file major version` | системний `java` — JDK 24+. Запустіть `.\scripts\build-apk.ps1` або вкажіть `JAVA_HOME` на JDK 21 |
| `SDK location not found` | немає `local.properties` — див. розділ 5 |
| `Dependency ... requires compileSdk 37 / AGP 9` | хтось оновив бібліотеки, застарілі для AGP 8.13.2. Тримайте версії з `gradle/libs.versions.toml` |
| APK не встановлюється (`INSTALL_PARSE_FAILED_NO_CERTIFICATES`) | це непідписаний release — підпишіть його або візьміть debug-збірку |
| `python` не знайдено | встановіть Python 3.10+; для збірки APK він не потрібен |
