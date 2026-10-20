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
  mismo applicationId y la migración probada (`MigracionTest`, instrumentado).
- `MigracionTest` en el teléfono **sin** `connectedAndroidTest`: respaldo de `.debug`
  primero, después `adb install -r` del APK de app y del de androidTest y
  `adb shell am instrument -w -e class com.blair.cumpleanos.datos.MigracionTest
  com.blair.cumpleanos.debug.test/androidx.test.runner.AndroidJUnitRunner`. Así no se
  desinstala nada. No hay emulador configurado.
- Prueba de upgrade real: con la versión anterior instalada y datos cargados,
  `adb install -r` encima y comparar las tablas antes/después (`run-as ... tar`).
- Journal en TRUNCATE, no WAL: el backup copia `cumpleanos.db` sin el `-wal`.
  Las reglas de backup (`res/xml/`) incluyen la base y `ajustes.xml`.

Estado actual: `@Database version = 3`, schemas `1.json`, `2.json` y `3.json`.
Migraciones registradas: `.addMigrations(MIGRACION_1_2, MIGRACION_2_3)`.
- `MIGRACION_1_2` (v1.0.0 → 1.1.x): `regaloPlan`, `regaloReal`, `fechaFestejoDia`.
- `MIGRACION_2_3` (1.1.1 → 1.2.0): `asistio`, `asistenciaPreguntadaDia`. Primer cambio
  de schema con testers sobre la v2.

## Releases (versionCodes usados en Play)

Play rechaza un versionCode ya subido en CUALQUIER pista (interna, cerrada,
producción, o un borrador descartado). Antes de generar un AAB: subir el versionCode
por encima del MAYOR de esta tabla y agregar la fila nueva.

| versionCode | versionName | schema | Commit / tag | Estado |
|-------------|-------------|--------|--------------|--------|
| 1 | 1.0.0 | v1 | `v1.0.0` (9334fde) | publicado |
| 2 | 1.1.x | v2 | — | subido |
| 3 | 1.1.1 | v2 | 06a57ec | publicado |
| 4 | 1.2.0 | v2 | b3c2859 (solo buscador) | **usado en Play**: no reutilizar |
| 5 | 1.2.0 | v3 | `v1.2.0-vc5` | en closed testing; solicitud de acceso a Producción enviada 2026-10-20 14:08 (respuesta por mail, ≤7 días) |

**Próxima tanda: versionCode 6.** Mientras la solicitud de Producción está en
revisión **no se sube nada al track de closed testing** (un release nuevo puede
reiniciar o complicar la evaluación): lo que se termine queda commiteado y
pusheado a git, sin AAB subido.

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
`fechaFestejoDia?` (null = sin festejo aparte), `asistio?` (fue al festejo de ESTA
ocurrencia; null = sin responder), `asistenciaPreguntadaDia?` (epochDay en que se
mostró la pregunta; distingue "no pregunté" de "pregunté y no contestó"). Mover el
festejo borra la respuesta.

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
- Asistencia al festejo (`dominio/Asistencia.kt`): se pregunta desde el día SIGUIENTE
  al festejo y hasta 30 días después, si `asistio` es null. Notificación "¿Fuiste al
  festejo de X?" una sola vez por ocurrencia (solo si `asistenciaPreguntadaDia` es
  null), una por tick, la del festejo más reciente. Tocarla abre la app con la
  tarjeta arriba (extra `asistencia` = cumpleId). En la app, UNA tarjeta, la más
  reciente; mostrarla anota el día y ya no se notifica.
- Espacios de ids de notificación: cumple = personaId, festejo = `1 shl 30` +
  personaId, asistencia = `3 shl 29` + personaId, resumen = `Int.MAX_VALUE`. Todo en
  el canal `cumple_v1` salvo el resumen.

## Diseño

Tema único **RECITAL**, oscuro: `darkColorScheme` fijo en `ui/theme/Theme.kt`. **Nada
de `dynamicColor`** ni tema claro. Paleta en `ui/theme/Color.kt`: noche violeta
(`NocheBase` #100038), escenario (`Escenario1` #4018B0), neón cian (`CianNeon` #30F0F8),
dorado (`Dorado` #F8D818), rojo de acción (`RojoCta` #E03840), blanco de escena
(`BlancoEscena`). Fuente Outfit empaquetada. Campos: `CampoTexto` (violeta translúcido,
borde cian al enfocar). Textos en `values/` (inglés) y `values-es/`.

## AdMob

App ID en el manifiesto por `manifestPlaceholders["adAppId"]` (release
`ca-app-pub-6760097434219921~8061744806`). No se duplica.

| buildType | `AD_UNIT_BANNER` | `AD_UNIT_NATIVE` |
|-----------|------------------|------------------|
| debug     | prueba `…3940256099942544/9214589741` | prueba `…3940256099942544/2247696110` |
| release   | `ca-app-pub-6760097434219921/3170801198` | `ca-app-pub-6760097434219921/8011490915` |
| medicion  | prueba, y `PEDIR_ANUNCIO=false`: no pide nada | prueba, no pide |

- **Banner**: adaptativo en el `bottomBar` del Scaffold, una instancia por sesión. No
  se recrea al navegar ni al abrir hojas. **Nunca** `loadAd()` por acción del usuario:
  es invalid traffic y suspende la cuenta entera. El refresco (60 s) se configura en
  el panel de AdMob, no en el código.
- **Nativos** (`ui/AnuncioNativo.kt`): **uno solo visible por pantalla**. Lo decide
  `CoordinadorNativos` (scope de pantalla, `PantallaConNativos`) por prioridad:
  1 tarjeta de asistencia, 2 hoja abierta, 3 AltaPantalla, 4 arriba del HISTORIAL.
  Solo se pide el activo; el tapado se pausa (conserva su anuncio) y vuelve sin pedir
  otro. Siempre en su propia tarjeta, con etiqueta "Anuncio" y CTA gris neutro (nunca
  cian, degradé, dorado ni rojo). Nada de nativos "chicos" como separador, ni
  intercalados cada N filas, ni en PERSONAS: es "ad mimicking app content".
  Sin anuncio no se dibuja la tarjeta: sin hueco reservado.

## Errores que ya cometimos

- **Anuncios dentro de un `LazyColumn`**: el item se destruye y se recrea al
  scrollear, y cada recreación es una request nueva (invalid traffic). El anuncio
  vive fuera del item (`nativoEn(...)` se llama fuera de la LazyColumn), el item solo
  lo dibuja, con key estable, y `destroy()` va en el `onDispose` del dueño.

- **`imePadding` duplicado**: el `Scaffold` de `MainActivity` ya suma `WindowInsets.ime`.
  Sumar `imePadding()` en una pantalla dentro de él sube el contenido el doble del
  teclado. Solo las `ModalBottomSheet` (fuera del Scaffold) lo llevan.
- **Banner**: `navigationBarsPadding()` va ANTES de `height()`; al revés, el padding se
  descuenta del alto del AdView.
- **`getQuantityString`**: la cantidad va dos veces, una para elegir el plural y otra
  como argumento del formato (`getQuantityString(id, n, n)`). Con una sola, el `%d` queda
  sin valor.
- **`connectedAndroidTest`** desinstala `.debug` (ver arriba).
- **LazyColumn dentro de LazyColumn** tira `IllegalStateException`. El historial va en
  UNA fila: `Column` con `heightIn(max)` + `verticalScroll` y barra dibujada (patrón
  de Por Vencer).
- **Abrir otra carpeta**: ya se entregó una vez el relevamiento de Por Vencer creyendo
  que era esta app. Verificar carpeta y applicationId antes de nada.

## Comprobaciones

```
./gradlew :app:testDebugUnitTest :app:assembleDebug
ls app/schemas/com.blair.cumpleanos.datos.BaseDatos/
grep -rn 'fallbackToDestructive' app/src   # tiene que dar vacío
```

Commits en español, estilo `tipo(área): descripción`.
