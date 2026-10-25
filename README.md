# BEE J 1.0 | DIGITALMINDS

App Android nativa, offline, para las 30 palabras del concurso de 3rd, 4th and 5th grade.

## Incluye

- 30 palabras exactas, con CAREFUL corregida.
- Diccionario de estudio y 26 letras con audio offline.
- Sonido de la palabra y deletreo completo para cada palabra.
- Entrenamiento de escritura, modo concurso eliminatorio y repaso de errores.
- Progreso persistente, lecciones sugeridas durante 14 dias.
- Colores DIGITALMINDS, iconos y sello a partir de imagenes originales.
- Pruebas unitarias de inventario y normalizacion.
- Workflow: test, debug y release con R8, artefactos y publicacion al subir tags `v*`.

## Advertencias importantes

1. **Audio v1.0:** Los 86 archivos `.ogg` se generaron localmente mediante eSpeak (variante `en-us`). Funcionan sin internet. La inteligibilidad y naturalidad no equivalen a audios de locutores reales; validar audio con estudiantes antes de un concurso. CMUdict, por si solo, NO contiene grabaciones. Los archivos se pueden sustituir manteniendo los mismos nombres.
2. **Reconocimiento oral:** no se incluye evaluacion de micrófono, ya que seria incorrecto prometer reconocimiento fiable, completamente offline y ultraligero para todos los equipos. El aprendizaje auditivo y el deletreo por escritura funcionan offline.
3. **Fuentes:** no se adjuntaron fuentes Space Grotesk, Michroma ni Atkinson Hyperlegible. Esta version usa la tipografia base del sistema hasta que se obtengan legalmente los archivos `.ttf`.
4. **Firma:** `app/beej.keystore` está incluida y se usa en CI. La contrasena de ejemplo en Gradle es pública. Para una publicación comercial es indispensable cambiar a credenciales seguras guardadas en GitHub Secrets, y conservar una copia de la keystore para futuras actualizaciones. La contraseña actual es `BeeJDemo2026!`.
5. **Compilación:** en el entorno de generación no se disponía de Android SDK ni de las dependencias Gradle, por lo que NO fue posible compilar ni ejecutar el APK. La validacion final debe hacerse desde Actions, y si falla se necesita el log del paso rojo.
6. El plan de 14 días organiza grupos de palabras y seguimiento de progreso, pero no garantiza el dominio de todos los estudiantes en ese plazo.
7. No necesita permiso de INTERNET. Todos los audios están integrados.

## Requisitos

- VS Code, Git.
- Repositorio vacío en GitHub.
- GitHub Actions habilitado.
- Android SDK con plataforma 37 en CI.

Los scripts `gradlew` y `gradlew.bat` son lanzadores de conveniencia que usan un Gradle instalado o descargan la distribucion 8.13. GitHub Actions instala Gradle con `gradle/actions/setup-gradle` directamente. No se incluye el jar oficial del Wrapper. Una vez instalado Gradle puede regenerarse el wrapper con `gradle wrapper --gradle-version 8.13` si se desea la estructura oficial.

## Primera publicación

Cambiar `USUARIO` por la cuenta de GitHub y `REPOSITORIO` por el nombre real.

```powershell
git init
git branch -M main
git remote add origin https://github.com/USUARIO/REPOSITORIO.git
git add -A
git commit -m "BEE J 1.0: primera version"
git push -u origin main
git tag v1.0
git push origin v1.0
```

En GitHub, ir a `Actions`, revisar los pasos, y descargar `BEE-J-v1.0.apk` desde `Releases`. Si falla, copiar el log del paso rojo.

## Estructura

- `app/src/main/java/com/digitalminds/beej/`: interfaz, palabras, logica y estado.
- `app/src/main/res/raw/`: 86 audios offline.
- `app/src/main/res/drawable-nodpi/`: icono, logo y sello de marca.
- `app/src/test/`: pruebas unitarias.
- `.github/workflows/build.yml`: automatizacion.

## Dispositivos

`minSdk 31`, `compileSdk 37`, `targetSdk 37`. La aplicación es Kotlin + Compose y no incluye binarios `.so` ligados a una arquitectura; no hay limitación de ABI solo arm64. El tamaño final del APK y su instalación en un Samsung Galaxy A03 Core permanecen sin verificar.


## Versión 1.1
Corrección en GitHub Actions: setup-android recibe solo `platform-tools` y deja de solicitar el paquete obsoleto `tools`. Se mantiene la instalación de Android 37 y las tareas de pruebas y compilación.
