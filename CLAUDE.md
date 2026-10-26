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

Estado actual: `@Database version = 4`, schemas `1.json` a `4.json`.
Migraciones registradas: `.addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4)`.
- `MIGRACION_1_2` (v1.0.0 → 1.1.x): `regaloPlan`, `regaloReal`, `fechaFestejoDia`.
- `MIGRACION_2_3` (1.1.1 → 1.2.0): `asistio`, `asistenciaPreguntadaDia`. Primer cambio
  de schema con testers sobre la v2.
- `MIGRACION_3_4` (1.2.x → 1.3.0): `omitidoDia`, `motivoOmitido`, `motivoNoAsistio`.
  Upgrade real probado en el teléfono 2026-10-26 (25/25 filas idénticas). Editar la
  fecha del saludo y eliminar un saludo no tocan el esquema: `saludadoDia` no está
  en el índice único.

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
| 6 | 1.2.1 | v3 | ver git log | AAB generado 2026-10-20, sin subir (nativos mini en alta y entre secciones; buscador de personas; línea bajo los títulos; imePadding del alta; halo del FAB fuera del banner) |
| 7 | 1.2.2 | v3 | ver git log | AAB generado 2026-10-21 (nativos: media topeado y apaisado, compacto de alto fijo, regla de densidad, no se recargan al girar) |
| 8 | 1.3.0 | v4 | ver git log | AAB generado 2026-10-26 (marca en todos los idiomas; Velita con globo sin cumpleaños; contadores ESTE MES / ESTE AÑO; No saludé y excusa del festejo (schema v4, MigracionTest OK 11/11); editar fecha y eliminar un saludo; avisos 00:05 y del día siguiente con botones; nativos mini en ventanas y mediano al final; sin permiso de batería) |

**Próxima tanda: versionCode 9.** Mientras la solicitud de Producción está en
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
festejo borra la respuesta. `omitidoDia?` ("No saludé": se cierra como un saludo,
`saludado = 1` y `saludadoDia` = ese día; sale de contadores y avisos y queda en el
historial como "No saludado"; no cambia `ultimoSaludoDia`), `motivoOmitido?`,
`motivoNoAsistio?` (la excusa del festejo).

**Contadores ESTE MES / ESTE AÑO** (cabecera, siempre los dos, amarillos):
`pendientesEntre` cuenta ocurrencias del rango sin saludar, o saludadas con un
festejo que todavía no pasó. Un "No saludé" no cuenta.

Fechas como epochDay `Int` en todo el dominio.

## Notificaciones

- `notif/Madrugada.kt`: `setAndAllowWhileIdle` a las 00:05 (sin permiso de alarma
  exacta, corre en reposo) que publica los avisos del día: ninguna app se entera del
  desbloqueo, así que el aviso ya tiene que estar al desbloquear. Antes de las 8
  (`HORA_SONORA`) sale mudo. Se reprograma al disparar, al arrancar, en
  `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` y cambios de hora. Debug:
  `--ez madrugada true`.
- `TickWorker`: WorkManager periódico de 24 h a las 9:00 (±1 h), de respaldo. Los dos
  corren `correrTick` y cada aviso se marca una vez por día: no duplican.
- Cumpleaños: D-0 siempre (no configurable) y los preavisos de la persona (default 7 y
  1 días antes). Una vez por día por ocurrencia (`avisoUltimoDia`). Un cumple pasado no
  reavisa a diario.
- Festejo: **independiente** del cumple. Avisa el día anterior y el día mismo, aunque el
  cumple ya esté saludado. Si cae el mismo día del cumple no avisa (gana el del cumple).
  El "ya avisé" del festejo vive en **prefs** (`festejo_aviso_<cumpleId>`), no en la
  tabla.
- Resumen del mes: el día 1 (o el primer tick del mes), silencio si no hay cumpleaños.
- Día siguiente al cumple sin marcar (`preguntaSaludo`): "¿Saludaste a X ayer?" una
  vez, con botones YA SALUDÉ / NO SALUDÉ (abren la hoja del regalo o la de la excusa).
  La de asistencia lleva FUI / NO FUI (NO FUI abre la excusa). Los botones abren
  `MainActivity` con `EXTRA_ACCION` y `EXTRA_CUMPLE`.
- Asistencia al festejo (`dominio/Asistencia.kt`): se pregunta desde el día SIGUIENTE
  al festejo y hasta 30 días después, si `asistio` es null. Notificación "¿Fuiste al
  festejo de X?" una sola vez por ocurrencia (solo si `asistenciaPreguntadaDia` es
  null), una por tick, la del festejo más reciente. Tocarla abre la app con la
  tarjeta arriba (extra `asistencia` = cumpleId). En la app, UNA tarjeta, la más
  reciente; mostrarla anota el día y ya no se notifica.
- Espacios de ids de notificación: cumple = personaId, festejo = `1 shl 30` +
  personaId, asistencia = `3 shl 29` + personaId, pregunta del día siguiente =
  `1 shl 29` + personaId, resumen = `Int.MAX_VALUE`. Todo en
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

| buildType | `AD_UNIT_BANNER` | `AD_UNIT_NATIVE` | `AD_UNIT_NATIVE_FINAL` |
|-----------|------------------|------------------|------------------------|
| debug     | prueba `…3940256099942544/9214589741` | prueba `…3940256099942544/2247696110` | prueba (la misma) |
| release   | `ca-app-pub-6760097434219921/3170801198` | `ca-app-pub-6760097434219921/8011490915` | `ca-app-pub-6760097434219921/6727036276` |
| medicion  | prueba, y `PEDIR_ANUNCIO=false`: no pide nada | prueba, no pide | prueba, no pide |

`AD_UNIT_NATIVE_FINAL` es solo para `LISTA_FINAL` (el mediano del final): unidad
propia para medir ese lugar aparte en el panel.

- **Banner**: adaptativo en el `bottomBar` del Scaffold, una instancia por sesión. No
  se recrea al navegar ni al abrir hojas. **Nunca** `loadAd()` por acción del usuario:
  es invalid traffic y suspende la cuenta entera. El refresco (60 s) se configura en
  el panel de AdMob, no en el código.
- **Nativos** (`ui/AnuncioNativo.kt`, `CoordinadorNativos` en `NativosViewModel`:
  sobrevive a la rotación, girar no pide otro):
  - **Tamaños.** MINI (hojas y ventana de borrar historial): una línea de 44dp,
    etiqueta, título y CTA, sin ícono. COMPACTO (lista): 64dp con ícono, título a 2
    líneas y anunciante. MEDIANO (final de la lista): con media topeado a 120dp.
    COMPLETO (alta): media topeado a 180dp. Se pide `NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE`.
  - **Aire de 4dp entre los assets y el borde de la NativeAdView** (`AIRE_ASSETS_DP`).
    Pegados al borde, el redondeo dp→px los dejaba un píxel afuera según el ancho y
    el validador marcaba "Advertiser assets outside native ad view" (en la lista sí,
    en el alta no). Mismo arreglo que en Ya La Tomé.
  - **CTA apagado**, violeta parecido al fondo de la tarjeta (`FONDO_CTA` #3B2A86,
    texto #CFC8EE): pedido del usuario, no compite con los botones de la app.
  - Tarjeta de fondo **sólido**: translúcida, el confeti se veía a través del anuncio.
  - **Alta, hojas y ventana**: uno solo, el de mayor prioridad (1 alta, 2 hoja, 3
    ventana), pedido al abrir, `destroy()` al cerrar. El del alta va siempre.
    Confirmaciones y calendarios NO llevan: quedaría pegado a Borrar/Cancelar y
    encima de otro anuncio.
  - **Lista — regla de densidad** (`ui/DensidadNativos.kt`, `ubicarNativos`):
    secciones pasados, este año, próximo año, personas, historial (este cuenta hasta
    `FILAS_MAX_HISTORIAL`) y después el final. Sin cumpleaños: ninguno (aunque haya
    historial). 1–5 filas: uno solo, tras PRÓXIMOS CUMPLEAÑOS. 6+: uno tras la
    primera sección con datos y los siguientes con 6 filas desde el anterior; si el
    final queda pegado al anterior, gana el final. Nunca dos en pantalla.
  - Los turnos LISTA_1..4 y LISTA_FINAL se anotan con datos y cada uno se pide la
    primera vez que su ancla queda en pantalla al detenerse el scroll.
  - Con una hoja, el alta o la ventana encima, los de la lista quedan invisibles en
    su lugar (`alpha 0`) y vuelven los mismos al cerrar.
  - Sin animación de entrada.
  Siempre en su propia tarjeta, con etiqueta "Anuncio". Van ENTRE secciones; nada
  dentro de la tarjeta de PERSONAS: es "ad mimicking app content".
  Sin anuncio no se dibuja la tarjeta: sin hueco reservado.

## Sin cumpleaños

Misma pantalla con sus secciones vacías. Velita (`ui/MascotaInvita.kt`) arriba del
botón, señalándolo, con halo dorado, llama que crece y globo "¡AGREGÁ ESE
CUMPLEAÑOS…!" (como Ya La Tomé y Por Vencer). La animación corre con
`repeatOnLifecycle(STARTED)`: en segundo plano no dibuja nada (medido: 0 cuadros).

## Permisos

`MainActivity`: después del splash, notificaciones directo con la ventana del sistema
(sin cartel previo). Una vez por día mientras no se permita (`permisos_pedidos_dia`
en prefs); permitido, no pregunta nunca. Si Android bloqueó la ventana, abre el
interruptor de la app. **Batería NO** (decisión del usuario 2026-10-26): Play revisa
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` y la alarma de las 00:05 no la necesita.

## Errores que ya cometimos

- **Anuncios dentro de un `LazyColumn`**: el item se destruye y se recrea al
  scrollear, y cada recreación es una request nueva (invalid traffic). El anuncio
  vive fuera del item (`nativoEn(...)` se llama fuera de la LazyColumn), el item solo
  lo dibuja, con key estable, y `destroy()` va en el `onDispose` del dueño.

- **`imePadding` duplicado**: sumarlo en padre e hijo sube el contenido el doble del
  teclado. Pero con `bottomBar` el `Scaffold` da de `innerPadding` el alto del banner,
  NO el del teclado: el final de AltaPantalla quedaba detrás del teclado sin poder
  scrollearse. AltaPantalla lleva `imePadding()` UNA vez y `MainActivity` le pasa
  `padding(innerPadding).consumeWindowInsets(innerPadding)`. Las `ModalBottomSheet`
  (fuera del Scaffold) llevan el suyo.
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
