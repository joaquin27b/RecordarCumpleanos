# Recordar Cumpleaños

App Android (Kotlin + Compose + Room + WorkManager + AdMob) para no olvidarse de saludar.

## 🔴 Antes de nada: ¿es la carpeta correcta?

- Carpeta: `~/AndroidStudioProjects/RecordarCumpleanos2`
- `applicationId` y `namespace`: `com.blair.cumpleanos`
- Remote: `https://github.com/joaquin27b/RecordarCumpleanos.git`
- Rama de trabajo: `rediseno`. Principal: `main`.

`~/AndroidStudioProjects/RecordarCumpleanos` (sin el 2) es el **respaldo original en v1.0.0**:
no tiene nada de la 1.1.x. No se trabaja ahí. Si el applicationId o el remote no
coinciden, frenar y avisar: es otra app (ya pasó con Por Vencer).

## Builds

| Build      | applicationId                     | Para qué |
|------------|-----------------------------------|----------|
| debug      | `com.blair.cumpleanos.debug`      | desarrollo, con sembrador de datos de prueba |
| medicion   | `com.blair.cumpleanos.medicion`   | release minificada con firma debug, sembrador, sin pedir anuncios |
| release    | `com.blair.cumpleanos`            | Play. Firma con `keystore.properties` (fuera de git) |

- 🔴 **Nunca se instala el APK release en el teléfono de desarrollo.**
- 🔴 **`connectedAndroidTest` desinstala `.debug` al terminar** y se lleva sus datos. No
  correrlo en el teléfono de desarrollo sin avisar antes.
- El sembrador (`src/sembrador/`) existe solo en debug y medicion, nunca en release.
- El AAB se genera solo cuando el usuario lo pide.

## 🔴 Datos del usuario: no se pierden nunca

Hay testers con datos reales y la v1.0.0 está publicada. Toda versión que sale tiene
que abrir la base que el usuario ya tiene y conservarla entera:

- Base: `cumpleanos.db` (Room, `BaseDatos`). Prefs: `ajustes`. No se renombran.
- **Prohibido `fallbackToDestructiveMigration`** en cualquiera de sus variantes.
- Toda columna nueva va por `ALTER TABLE ... ADD COLUMN` dentro de una `Migration`
  versionada (sube `version` en `@Database`, se exporta el `.json` nuevo en
  `app/schemas/`). Nullable o con DEFAULT, sin tocar filas existentes. Chequear
  `PRAGMA table_info` antes del ALTER, como en `MIGRACION_1_2`, para que repetirla no falle.
- Nunca se edita una Migration ya publicada; se agrega la siguiente.
- Antes de un AAB: `versionCode` mayor que el publicado, misma firma de release,
  mismo applicationId y la migración probada (`MigracionTest`, en un emulador: es
  instrumentado y `connectedAndroidTest` desinstala `.debug`).
- Journal en TRUNCATE, no WAL: el backup copia `cumpleanos.db` sin el `-wal`.
  Las reglas de backup (`res/xml/`) incluyen la base y `ajustes.xml`.

Estado actual: `@Database version = 2`, schemas `1.json` y `2.json`, única migración
`MIGRACION_1_2` (agrega `regaloPlan`, `regaloReal`, `fechaFestejoDia`).

## Modelo de datos

**Persona**: `id`, `nombre`, `diaHabitual` (día de nacimiento crudo: un 29/02 guarda 29),
`anioNacimiento?` (null = no se sabe, sin edad), `ancla` (epochDay de la última
ocurrencia generada; el mes vive acá), `ultimoSaludoDia?`, `activo` (false = eliminada;
la fila queda para siempre porque el historial la nombra), `preavisos?` (CSV de días;
null = default, "" = ninguno).

**Cumple** (una fila por ocurrencia anual): `id`, `personaId` (FK RESTRICT), `fechaDia`
(epochDay, única fuente de verdad temporal), `avisoUltimoDia?`, `saludado`,
`saludadoDia?`, `anulado` (marcado por error: vuelve a pendiente pero sigue tachado en
el historial), `regaloPlan?`, `regaloReal?` (se confirma al saludar),
`fechaFestejoDia?` (null = sin festejo aparte).

Fechas como epochDay `Int` en todo el dominio.

## Notificaciones

- `TickWorker`: WorkManager periódico de 24 h, primera corrida a las 9:00 (±1 h). No
  AlarmManager: sobrevive al reboot y no pide `USE_EXACT_ALARM`.
- Cumpleaños: D-0 siempre (no configurable) y los preavisos de la persona (default 7 y
  1 días antes). Una vez por día por ocurrencia (`avisoUltimoDia`). Un cumple pasado no
  reavisa a diario.
- Festejo: **independiente** del cumple. Avisa el día anterior y el día mismo, aunque el
  cumple ya esté saludado. Si cae el mismo día del cumple no avisa (gana el del cumple).
  El "ya avisé" del festejo vive en **prefs** (`festejo_aviso_<cumpleId>`), no en la
  tabla.
- Resumen del mes: el día 1 (o el primer tick del mes), silencio si no hay cumpleaños.

## Diseño

Tema único **RECITAL**, oscuro: `darkColorScheme` fijo en `ui/theme/Theme.kt`. **Nada
de `dynamicColor`** ni tema claro. Paleta en `ui/theme/Color.kt`: noche violeta
(`NocheBase` #100038), escenario (`Escenario1` #4018B0), neón cian (`CianNeon` #30F0F8),
dorado (`Dorado` #F8D818), rojo de acción (`RojoCta` #E03840), blanco de escena
(`BlancoEscena`). Fuente Outfit empaquetada. Campos: `CampoTexto` (violeta translúcido,
borde cian al enfocar). Textos en `values/` (inglés) y `values-es/`.

## Errores que ya cometimos

- **`imePadding` duplicado**: el `Scaffold` de `MainActivity` ya suma `WindowInsets.ime`.
  Sumar `imePadding()` en una pantalla dentro de él sube el contenido el doble del
  teclado. Solo las `ModalBottomSheet` (fuera del Scaffold) lo llevan.
- **Banner**: `navigationBarsPadding()` va ANTES de `height()`; al revés, el padding se
  descuenta del alto del AdView.
- **`getQuantityString`**: la cantidad va dos veces, una para elegir el plural y otra
  como argumento del formato (`getQuantityString(id, n, n)`). Con una sola, el `%d` queda
  sin valor.
- **`connectedAndroidTest`** desinstala `.debug` (ver arriba).

## Comprobaciones

```
./gradlew :app:testDebugUnitTest :app:assembleDebug
ls app/schemas/com.blair.cumpleanos.datos.BaseDatos/
grep -rn 'fallbackToDestructive' app/src   # tiene que dar vacío
```

Commits en español, estilo `tipo(área): descripción`.
