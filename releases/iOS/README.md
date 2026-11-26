# iOS-збірки

У цій папці зберігається **лише найновіша** збірка для iOS
(`millionaire-<version>-unsigned.ipa`).

iOS-білди не можна зібрати на Windows. Офіційний шлях — GitHub Actions workflow
**iOS Release Build** (`.github/workflows/ios-release.yml`): запускається вручну з вкладки
Actions (`workflow_dispatch`), збирає застосунок на macOS-раннері (XcodeGen + `xcodebuild`,
без підпису) і сам викладає новий `.ipa` сюди, видаляючи попередній.

## Як встановити unsigned .ipa

Файл не підписаний сертифікатом розробника Apple, тому встановлюється через sideload-інструменти:

- **AltStore / Sideloadly** — підписує безкоштовним Apple ID (потрібно періодично оновлювати).
- Власний Apple Developer сертифікат — тоді варто додати підпис у сам workflow.

## Ручна збірка на macOS

```bash
brew install xcodegen
cd iosApp && xcodegen generate
open iosApp.xcodeproj   # схема iosApp
```

`.xcodeproj` згенерований — його ніколи не комітять (див. `.gitignore`).
