# BEE J 1.3

Aplicación Android offline para practicar spelling bee en inglés estadounidense.

## Cambios principales de la versión 1.3

- Rediseño visual general con estilo oscuro más limpio.
- Ícono del launcher reducido para evitar que llegue a los bordes.
- Splash con branding actualizado de DigitalMinds.
- Pantalla principal reorganizada en 6 módulos:
  - Aprender
  - Entrenar
  - Concurso
  - Repasar errores
  - Alfabeto
  - Método
- Aprender muestra directamente las 30 palabras y un modo limpio para escuchar palabra y deletreo.
- Entrenar incluye dos modos:
  - Voz
  - Escritura
- Concurso usa el formato: palabra, deletreo, palabra.
- Opción para dobles letras:
  - letra por letra
  - DOUBLE + letra
- Velocidad independiente para pronunciación y deletreo.
- Método profesional de 14 días basado en repetición espaciada, práctica de recuperación y simulación.
- Audio regenerado para letras y palabras en inglés estadounidense.
- Reconocimiento de voz configurado con preferencia offline en `en-US`.

## Notas técnicas importantes

- El audio de la app está empaquetado dentro del APK y funciona sin internet.
- El reconocimiento de voz solicita modo offline, pero Android depende del motor de reconocimiento instalado en el dispositivo.
- Si el dispositivo no tiene modelo offline de voz en inglés, Android puede limitar esa función.

## Build

- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 31`
- `versionCode = 4`
- `versionName = 1.3`

## Archivos clave

- `MainActivity.kt`
- `BeeViewModel.kt`
- `Words.kt`
- `SpellingEvaluator.kt`

