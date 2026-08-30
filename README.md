# UltimateTracker

Небольшой персональный трекер фильмов, сериалов и аниме на Kotlin и Jetpack Compose.

## Запуск

1. Откройте эту папку в актуальной Android Studio.
2. Дождитесь Gradle Sync и установки Android SDK 36 по предложению Android Studio.
3. Создайте эмулятор или подключите Android-устройство с Android 6.0 (API 23) или новее.
4. Нажмите **Run app**.

Сборка из терминала Android Studio:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

APK после успешной сборки находится в `app/build/outputs/apk/debug/app-debug.apk`.

## Архитектура

Один модуль `app`, упрощённый MVVM: Compose-экраны отправляют действия во ViewModel, ViewModel публикует `StateFlow`, Repository работает с Room DAO, а Room хранит коллекцию в локальной SQLite-базе.
