# Reproductor de video para el celular (VideoCelu)

App Android liviana para reproducir los videos guardados en el teléfono, hecha en Kotlin con **AndroidX Media3 / ExoPlayer**.

## Qué hace

- Escanea las carpetas del celular y muestra los videos con miniaturas.
- Reproductor a pantalla completa con servicio en primer plano (sigue sonando con la pantalla apagada y tiene controles en la notificación).
- Pantalla de ajustes y una pantalla de **códecs** para ver qué decodificadores de hardware/software ofrece el equipo.

## Requisitos

- Android 11 o superior (minSdk 30, targetSdk 35).
- JDK 17 y Android SDK para compilar.

## Compilar

```bash
# Si tenés el JDK y el SDK en .tools/ (o en ../app-mando-facil/.tools), cargá el entorno:
source tools/env.sh
./gradlew assembleDebug
# El APK queda en app/build/outputs/apk/debug/
```

También podés abrir la carpeta con Android Studio. Creá un `local.properties` con `sdk.dir=<ruta a tu Android SDK>` si no usás `tools/env.sh` (ese archivo no se sube al repo).
