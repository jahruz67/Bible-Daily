# Biblia Diaria

Android app en español para leer la Palabra del día desde Vatican News.

La app calcula la fecha del dispositivo y carga esta ruta:

```text
https://www.vaticannews.va/es/evangelio-de-hoy/YYYY/MM/DD.html
```

La pantalla principal muestra primero la Palabra de Dios, después la lectura del día y las palabras de los Papas. El botón `Extras` abre una sección de oraciones especiales, empezando con `Ven, Espíritu Creador`. El selector de fecha en la parte superior abre un calendario mensual para cambiar de día, mes o año; al abrir la app siempre vuelve a la fecha actual del dispositivo. El botón flotante `Aa` abre un control para ajustar el tamaño de letra.

## Abrir el proyecto

Abre esta carpeta en Android Studio y sincroniza el proyecto Gradle. Este entorno no incluye Java, Gradle ni Android SDK, así que la compilación debe hacerse desde Android Studio o desde una terminal que ya tenga esas herramientas instaladas.
