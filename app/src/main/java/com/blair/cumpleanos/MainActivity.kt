package com.blair.cumpleanos

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.blair.cumpleanos.datos.CumpleUi
import com.blair.cumpleanos.notif.Accion
import com.blair.cumpleanos.notif.EXTRA_ACCION
import com.blair.cumpleanos.notif.EXTRA_ASISTENCIA
import com.blair.cumpleanos.notif.EXTRA_CUMPLE
import com.blair.cumpleanos.notif.EXTRA_NOTIF
import com.blair.cumpleanos.ui.FilaLista
import androidx.compose.runtime.snapshotFlow
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.blair.cumpleanos.notif.ACCION_MADRUGADA
import com.blair.cumpleanos.notif.Madrugada
import com.blair.cumpleanos.notif.TickWorker
import com.blair.cumpleanos.repo.FechaSaludo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import com.blair.cumpleanos.ui.AltaPantalla
import com.blair.cumpleanos.ui.GloboMascota
import com.blair.cumpleanos.ui.MascotaInvita
import com.blair.cumpleanos.ui.BannerAdaptativo
import com.blair.cumpleanos.ui.BotonPrincipal
import com.blair.cumpleanos.ui.Festejo
import com.blair.cumpleanos.ui.FilaCumple
import com.blair.cumpleanos.ui.FranjaBanner
import com.blair.cumpleanos.ui.HojaPersona
import com.blair.cumpleanos.ui.HojaRegalo
import com.blair.cumpleanos.ui.ListaPantalla
import com.blair.cumpleanos.ui.ListaViewModel
import com.blair.cumpleanos.ui.LugarNativo
import com.blair.cumpleanos.ui.PantallaConNativos
import com.blair.cumpleanos.ui.debePedirPermisos
import com.blair.cumpleanos.ui.SplashPantalla
import com.blair.cumpleanos.ui.esVacio
import com.blair.cumpleanos.ui.escena.Escenario
import com.blair.cumpleanos.ui.escena.Intensidad
import com.blair.cumpleanos.ui.nativoEn
import com.blair.cumpleanos.ui.theme.RecordarCumpleanosTheme
import java.time.LocalDate

/** epochDay de la ultima vez que se pidieron permisos, en prefs `ajustes`. */
private const val PREF_PERMISOS_DIA = "permisos_pedidos_dia"

/** Mas rapido que esto, el "no" lo dio el sistema sin mostrar la ventana. */
private const val RESPUESTA_INSTANTANEA_MS = 400L

class MainActivity : ComponentActivity() {

    /** cumpleId de la notificacion de asistencia tocada, con el momento: tocarla dos veces vuelve a subir. */
    private var asistenciaPedida by mutableStateOf<Pair<Long, Long>?>(null)

    /** Boton tocado en una notificacion: (accion, cumpleId, momento). */
    private var accionPedida by mutableStateOf<Triple<String, Long, Long>?>(null)

    /** Se consume y se borra, como la asistencia; la notificacion se cierra. */
    private fun leerAccion(i: Intent?) {
        val accion = i?.getStringExtra(EXTRA_ACCION) ?: return
        val cumpleId = i.getLongExtra(EXTRA_CUMPLE, -1L)
        val notif = i.getIntExtra(EXTRA_NOTIF, 0)
        i.removeExtra(EXTRA_ACCION)
        if (notif != 0) NotificationManagerCompat.from(this).cancel(notif)
        if (cumpleId > 0) accionPedida = Triple(accion, cumpleId, System.nanoTime())
    }

    /** Se consume y se borra: al rotar, el intent viejo no la vuelve a pedir. */
    private fun leerAsistencia(i: Intent?) {
        val id = i?.getLongExtra(EXTRA_ASISTENCIA, -1L) ?: return
        if (id <= 0) return
        i.removeExtra(EXTRA_ASISTENCIA)
        asistenciaPedida = id to System.nanoTime()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerAsistencia(intent)
        leerAccion(intent)
    }

    /** Cuando se lanzo el pedido de notificaciones: ver notificacionesBloqueadas. */
    private var pedidoNotifMillis = 0L

    private val permisoNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok && notificacionesBloqueadas()) abrirAjustesNotificaciones()
    }

    /**
     * Notificaciones DIRECTO con la ventana PERMITIR del sistema, sin cartel previo.
     * Nada de mandar a Ajustes con pasos: en Ya La Tomé, con usuarios reales, nadie
     * llegaba. Bateria no se pide: Play la revisa y la alarma de las 00:05 no la necesita.
     */
    private fun pedirNotificaciones() {
        if (notificacionesPermitidas()) return
        pedidoNotifMillis = System.currentTimeMillis()
        permisoNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * Android dejo de mostrar la ventana: tras dos rechazos el pedido vuelve negado
     * en el acto, sin que el usuario vea nada. La respuesta instantanea y sin
     * rationale es la unica señal de eso que da el sistema.
     */
    private fun notificacionesBloqueadas(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) &&
            System.currentTimeMillis() - pedidoNotifMillis < RESPUESTA_INSTANTANEA_MS

    /** Con la ventana bloqueada: la pantalla de notificaciones de ESTA app, un interruptor arriba. */
    private fun abrirAjustesNotificaciones() {
        abrir(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }

    private fun abrir(i: Intent) {
        try {
            startActivity(i)
        } catch (_: ActivityNotFoundException) {
            // Algunas capas de fabricante no exponen la pantalla: no hay a donde ir.
        }
    }

    /** POST_NOTIFICATIONS solo existe desde API 33: antes se tiene siempre. */
    private fun notificacionesPermitidas(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED


    override fun onCreate(savedInstanceState: Bundle?) {
        // Antes de super: el splash del sistema muestra el icono sobre la noche
        // del escenario. Sale con un fundido corto, achicandose: tiene que irse
        // antes de los 250 ms en que Velita empieza a crecer en SplashPantalla,
        // o por un instante hay dos Velitas.
        installSplashScreen().setOnExitAnimationListener { salida ->
            salida.view.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(150)
                .withEndAction { salida.remove() }
                .start()
        }
        super.onCreate(savedInstanceState)
        // Barras forzadas a estilo oscuro: con el telefono en modo claro,
        // enableEdgeToEdge() sin argumentos pinta los iconos oscuros y sobre la
        // noche del escenario desaparecen.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        leerAsistencia(intent)
        leerAccion(intent)
        // Solo debug: cmd jobscheduler run -f no saltea el initialDelay de WorkManager.
        // adb shell am start -n com.blair.cumpleanos/.MainActivity --ez tick true
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("tick", false) == true) {
            WorkManager.getInstance(this)
                .enqueue(OneTimeWorkRequestBuilder<TickWorker>().build())
        }
        // Solo debug: dispara la alarma de la madrugada sin esperar a las 00:05.
        // adb shell am start -n com.blair.cumpleanos.debug/com.blair.cumpleanos.MainActivity --ez madrugada true
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("madrugada", false) == true) {
            sendBroadcast(Intent(this, Madrugada::class.java).setAction(ACCION_MADRUGADA))
        }
        val app = application as CumpleanosApp
        // El show de apertura va solo en arranque en frio, no al rotar.
        val arranqueEnFrio = savedInstanceState == null
        setContent {
            RecordarCumpleanosTheme {
                val vm: ListaViewModel = viewModel(factory = ListaViewModel.factory(app.repo, app.db))
                var enSplash by rememberSaveable { mutableStateOf(arranqueEnFrio) }
                var enAlta by rememberSaveable { mutableStateOf(false) }
                // Cada saludo suma uno: el festejo se reinicia aunque se saluden dos seguidos.
                var festejo by remember { mutableIntStateOf(0) }
                var festejando by remember { mutableStateOf(false) }
                val encendido = remember { Animatable(if (arranqueEnFrio) 0f else 1f) }
                val personas by vm.personas.collectAsStateWithLifecycle()
                val pasados by vm.pasados.collectAsStateWithLifecycle()
                val proximos by vm.proximos.collectAsStateWithLifecycle()
                val conHistorial by vm.conHistorial.collectAsStateWithLifecycle()
                val haptico = LocalHapticFeedback.current

                Box(Modifier.fillMaxSize()) {
                    // Detras de TODO. Completa en splash y festejo; atenuada
                    // con tarjetas encima, o no se lee nada.
                    Escenario(
                        intensidad = if (enSplash || festejando) Intensidad.COMPLETA else Intensidad.ATENUADA,
                        encendido = { encendido.value },
                    )
                    Crossfade(targetState = enSplash, animationSpec = tween(550), label = "apertura") { splash ->
                        if (splash) {
                            SplashPantalla(encendido) { enSplash = false }
                        } else {
                            // Coordinador de nativos con scope de pantalla: uno visible a la vez.
                            PantallaConNativos {
                            Contenido(
                                vm = vm,
                                enAlta = enAlta,
                                onAlta = { enAlta = it },
                                onSaludar = { personaId, cumpleId, regalo ->
                                    vm.saludar(personaId, cumpleId, regalo)
                                    haptico.performHapticFeedback(HapticFeedbackType.LongPress)
                                    festejo++
                                },
                            )
                            }
                        }
                    }
                    Festejo(festejo, onActivo = { festejando = it })

                    // Una vez por dia mientras falte algo, despues del splash y sin el alta
                    // encima: una ventana del sistema sobre el splash se lee como error de arranque.
                    var permisosDecididos by rememberSaveable { mutableStateOf(false) }
                    val libre = !enSplash && !enAlta
                    LaunchedEffect(libre) {
                        if (permisosDecididos || !libre) return@LaunchedEffect
                        permisosDecididos = true
                        val hoy = LocalDate.now().toEpochDay()
                        val ultimo = app.prefs.getLong(PREF_PERMISOS_DIA, -1L).takeIf { it >= 0 }
                        if (!debePedirPermisos(notificacionesPermitidas(), ultimo, hoy)) return@LaunchedEffect
                        app.prefs.edit().putLong(PREF_PERMISOS_DIA, hoy).apply()
                        pedirNotificaciones()
                    }
                }
            }
        }
    }

    @Composable
    private fun Contenido(
        vm: ListaViewModel,
        enAlta: Boolean,
        onAlta: (Boolean) -> Unit,
        onSaludar: (personaId: Long, cumpleId: Long, regaloReal: String?) -> Unit,
    ) {
        val pasados by vm.pasados.collectAsStateWithLifecycle()
        val proximos by vm.proximos.collectAsStateWithLifecycle()
        val personas by vm.personas.collectAsStateWithLifecycle()
        val historial by vm.historial.collectAsStateWithLifecycle()
        val mes by vm.mes.collectAsStateWithLifecycle()
        val porAnio by vm.porAnio.collectAsStateWithLifecycle()
        val aniosHistorial by vm.aniosHistorial.collectAsStateWithLifecycle()
        val busqueda by vm.busqueda.collectAsStateWithLifecycle()
        val personaAbierta by vm.personaAbierta.collectAsStateWithLifecycle()
        val colision by vm.colision.collectAsStateWithLifecycle()
        val conHistorial by vm.conHistorial.collectAsStateWithLifecycle()
        val pendientesDePersona by vm.pendientesDePersona.collectAsStateWithLifecycle()
        val cumplesDelMes by vm.cumplesDelMes.collectAsStateWithLifecycle()
        val cumplesDelAnio by vm.cumplesDelAnio.collectAsStateWithLifecycle()
        val asistencia by vm.asistencia.collectAsStateWithLifecycle()
        val pedida = asistenciaPedida
        LaunchedEffect(pedida) { pedida?.let { vm.pedirAsistencia(it.first) } }
        // Al tocar YA SALUDÉ se abre primero la hoja del regalo; el saludo se
        // guarda recien al confirmar.
        var aSaludar by remember { mutableStateOf<FilaCumple?>(null) }
        var regaloHistorial by remember { mutableStateOf<CumpleUi?>(null) }
        // "No saludé" de una tarjeta, y el motivo de no ir al festejo tras tocar NO FUI.
        var aOmitir by remember { mutableStateOf<FilaCumple?>(null) }
        var motivoFestejo by remember { mutableStateOf<CumpleUi?>(null) }
        // Botones de las notificaciones del dia siguiente.
        val accion = accionPedida
        LaunchedEffect(accion) {
            val (que, cumpleId, _) = accion ?: return@LaunchedEffect
            accionPedida = null
            // La lista llega por Flow: se espera a que la ocurrencia este cargada.
            suspend fun fila() = withTimeoutOrNull(5_000) {
                snapshotFlow {
                    pasados.find { it.c.id == cumpleId }
                        ?: proximos.filterIsInstance<FilaLista.Dato>().map { it.fila }.find { it.c.id == cumpleId }
                }.filterNotNull().first()
            }
            when (que) {
                Accion.YA_SALUDE -> fila()?.let { aSaludar = it }
                Accion.NO_SALUDE -> fila()?.let { aOmitir = it }
                Accion.FUI -> vm.responderAsistencia(cumpleId, true)
                Accion.NO_FUI -> {
                    vm.responderAsistencia(cumpleId, false)
                    motivoFestejo = vm.cumpleUi(cumpleId)
                }
            }
        }
        var errorFecha by remember { mutableStateOf<FechaSaludo?>(null) }
        LaunchedEffect(regaloHistorial?.id) { errorFecha = null }
        // FUERA del if (enAlta) y de la LazyColumn: con el alta abierta la lista sale
        // de composicion pero sus lugares siguen anotados, asi el coordinador los
        // PAUSA (ALTA gana) en vez de destruirlos y pedir otros al volver.
        // Sin cumpleaños (aunque quede historial): ninguno en la lista, solo el
        // banner, hasta que se cargue uno. El del alta va siempre.
        val sinCumples = esVacio(pasados, proximos, personas)
        // Anotados no es pedidos: se pide solo el turno que ubicarNativos puso y
        // que esta en pantalla. Anotarlos todos fijos hace que un turno que se corre
        // o desaparece y vuelve conserve su anuncio en vez de pedir otro.
        val nativosLista = buildMap {
            if (!sinCumples) LugarNativo.entries.filter { it.enLista }.forEach { put(it, nativoEn(it)) }
        }

        Scaffold(
            // Transparente: el Escenario de atras es el fondo.
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.ime),
            bottomBar = { FranjaBanner { BannerAdaptativo() } },
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                if (!enAlta) {
                    // Sin cumpleaños, Velita se para arriba del boton y lo señala,
                    // con su globo arriba (como Ya La Tomé y Por Vencer). Por el
                    // estado: vuelve sola cada vez que se borran todos.
                    // El halo baja hasta 30dp bajo el boton y el Scaffold deja 16dp:
                    // el padding evita que pise el banner, y tapar un anuncio con UI
                    // de la app es violacion de politica.
                    Column(Modifier.padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (sinCumples) {
                            GloboMascota(
                                stringResource(R.string.mascota_globo),
                                onClick = { onAlta(true) },
                                // Pegado a Velita: la cola casi toca la punta de la llama.
                                // Por delante, asi la llama que crece queda detras.
                                modifier = Modifier
                                    .zIndex(2f)
                                    .offset(y = 34.dp)
                                    .padding(horizontal = 24.dp),
                            )
                            // Por encima del boton: la mano que lo señala se apoya sobre el.
                            MascotaInvita(Modifier.zIndex(1f).offset(y = 14.dp))
                        }
                        BotonPrincipal(
                            stringResource(R.string.accion_agregar),
                            onClick = { onAlta(true) },
                            icono = Icons.Filled.Add,
                            destacado = true,
                        )
                    }
                }
            },
        ) { innerPadding ->
            if (enAlta) {
                AltaPantalla(
                    onGuardar = { nombre, dia, mes, anio, preav, regalo, festejo ->
                        vm.crear(nombre, dia, mes, anio, preav, regalo, festejo)
                        onAlta(false)
                    },
                    onCancelar = { onAlta(false) },
                    // consumeWindowInsets: el imePadding del alta descuenta lo que
                    // ya resto el Scaffold (banner y barra) y no suma el doble.
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                )
            } else {
                ListaPantalla(
                    cumplesDelMes = cumplesDelMes,
                    cumplesDelAnio = cumplesDelAnio,
                    pasados = pasados,
                    proximos = proximos,
                    personas = personas,
                    historial = historial,
                    mes = mes,
                    porAnio = porAnio,
                    onSaludar = { f -> aSaludar = f },
                    onOmitir = { f -> aOmitir = f },
                    onEditarRegalo = { c -> regaloHistorial = c },
                    onEditarPersona = { id -> vm.abrirPersona(id) },
                    onEliminarPersona = { id -> vm.eliminarPersona(id) },
                    onBorrarHistorial = { vm.borrarHistorial() },
                    onBorrarHistorialDe = { id -> vm.borrarHistorialDe(id) },
                    conHistorial = conHistorial,
                    aniosHistorial = aniosHistorial,
                    onElegirPeriodo = vm::irA,
                    onAlternarPorAnio = vm::alternarPorAnio,
                    busqueda = busqueda,
                    onBuscar = vm::buscar,
                    asistencia = asistencia,
                    onResponderAsistencia = { id, fue ->
                        vm.responderAsistencia(id, fue)
                        // NO FUI: se ofrece anotar por que, para acordarse.
                        if (!fue) motivoFestejo = asistencia?.takeIf { it.id == id }
                    },
                    onAsistenciaMostrada = vm::asistenciaMostrada,
                    irArriba = pedida,
                    nativos = nativosLista,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        personaAbierta?.let { p ->
            HojaPersona(
                persona = p,
                regaloPlan = pendientesDePersona.minByOrNull { it.fechaDia }?.regaloPlan,
                festejo = pendientesDePersona.minByOrNull { it.fechaDia }?.fechaFestejoDia,
                fechaCumple = pendientesDePersona.minByOrNull { it.fechaDia }?.fechaDia ?: p.ancla,
                colision = colision,
                onGuardar = { editada, dia, mes, regalo, festejo ->
                    vm.editarPersona(editada, dia, mes, regalo, festejo) { vm.abrirPersona(null) }
                },
                onEliminar = { vm.eliminarPersona(p.id); vm.abrirPersona(null) },
                onBorrarHistorial = { vm.borrarHistorialDe(p.id) },
                onCerrar = { vm.abrirPersona(null) },
            )
        }

        aOmitir?.let { f ->
            HojaRegalo(
                titulo = stringResource(R.string.omitir_titulo, f.c.nombre),
                cuerpo = stringResource(R.string.omitir_cuerpo),
                etiqueta = stringResource(R.string.omitir_motivo),
                inicial = null,
                textoBoton = stringResource(R.string.accion_guardar),
                conIconoRegalo = false,
                onGuardar = { motivo, _ ->
                    aOmitir = null
                    vm.omitir(f.c.personaId, f.c.id, motivo)
                },
                onCerrar = { aOmitir = null },
            )
        }

        motivoFestejo?.let { c ->
            HojaRegalo(
                titulo = stringResource(R.string.no_fui_titulo, c.nombre),
                cuerpo = stringResource(R.string.no_fui_cuerpo),
                etiqueta = stringResource(R.string.no_fui_motivo),
                inicial = c.motivoNoAsistio,
                textoBoton = stringResource(R.string.accion_guardar),
                conIconoRegalo = false,
                onGuardar = { motivo, _ ->
                    vm.anotarMotivoNoAsistio(c.id, motivo)
                    motivoFestejo = null
                },
                onCerrar = { motivoFestejo = null },
            )
        }

        aSaludar?.let { f ->
            HojaRegalo(
                titulo = stringResource(R.string.saludo_titulo, f.c.nombre),
                cuerpo = stringResource(R.string.saludo_cuerpo),
                etiqueta = stringResource(R.string.regalo_real_etiqueta),
                inicial = f.c.regaloPlan,
                textoBoton = stringResource(R.string.accion_guardar_saludo),
                onGuardar = { regalo, _ ->
                    aSaludar = null
                    onSaludar(f.c.personaId, f.c.id, regalo)
                },
                onCerrar = { aSaludar = null },
            )
        }

        regaloHistorial?.let { c ->
            // Un "No saludé" se edita igual, pero el campo es su motivo y no un regalo.
            val omitido = c.omitidoDia != null
            HojaRegalo(
                titulo = if (omitido) stringResource(R.string.omitir_titulo, c.nombre) else stringResource(R.string.regalo_editar_titulo, c.nombre),
                cuerpo = if (omitido) null else c.regaloPlan?.let { stringResource(R.string.regalo_plan_era, it) },
                etiqueta = stringResource(if (omitido) R.string.omitir_motivo else R.string.regalo_real_etiqueta),
                inicial = if (omitido) c.motivoOmitido else c.regaloReal,
                textoBoton = stringResource(R.string.accion_guardar),
                conIconoRegalo = !omitido,
                etiquetaFecha = if (omitido) R.string.saludo_fecha_etiqueta_omitido else R.string.saludo_fecha_etiqueta,
                motivoNoAsistioInicial = c.motivoNoAsistio,
                onMotivoNoAsistio = { vm.anotarMotivoNoAsistio(c.id, it) },
                onGuardar = { regalo, dia ->
                    if (omitido) vm.anotarMotivoOmitido(c.id, regalo) else vm.anotarRegaloReal(c.id, regalo)
                    if (dia == null || dia == c.saludadoDia) {
                        regaloHistorial = null
                    } else {
                        // La fecha se valida contra la base: con aviso, la hoja queda abierta.
                        vm.editarFechaSaludo(c.id, dia) { r ->
                            if (r == FechaSaludo.OK) regaloHistorial = null else errorFecha = r
                        }
                    }
                },
                onCerrar = { regaloHistorial = null },
                fechaSaludoInicial = c.saludadoDia,
                errorFecha = when (errorFecha) {
                    FechaSaludo.FUTURA -> stringResource(R.string.saludo_fecha_futura)
                    FechaSaludo.CHOQUE -> stringResource(R.string.saludo_fecha_choque, c.nombre)
                    FechaSaludo.OK, null -> null
                },
                onFechaCambiada = { errorFecha = null },
                onEliminar = {
                    vm.eliminarSaludo(c.id)
                    regaloHistorial = null
                },
                asistioInicial = c.asistio,
                onAsistencia = if (c.fechaFestejoDia != null || c.asistio != null) {
                    { vm.responderAsistencia(c.id, it) }
                } else null,
            )
        }
    }
}
