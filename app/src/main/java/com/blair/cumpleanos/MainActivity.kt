package com.blair.cumpleanos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.blair.cumpleanos.ui.SplashPantalla
import com.blair.cumpleanos.ui.escena.Escenario
import com.blair.cumpleanos.ui.escena.Intensidad
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.blair.cumpleanos.notif.TickWorker
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blair.cumpleanos.ui.AltaPantalla
import com.blair.cumpleanos.ui.BannerAdaptativo
import com.blair.cumpleanos.ui.HojaPersona
import com.blair.cumpleanos.ui.ListaPantalla
import com.blair.cumpleanos.ui.ListaViewModel
import com.blair.cumpleanos.ui.theme.RecordarCumpleanosTheme

class MainActivity : ComponentActivity() {

    private val permiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** POST_NOTIFICATIONS solo existe desde API 33. */
    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val ok = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (ok != PackageManager.PERMISSION_GRANTED) permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Antes de super: el splash del sistema muestra solo la noche del
        // escenario y le pasa la posta a SplashPantalla sin corte de color.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Barras forzadas a estilo oscuro: con el telefono en modo claro,
        // enableEdgeToEdge() sin argumentos pinta los iconos oscuros y sobre la
        // noche del escenario desaparecen.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        pedirPermisoNotificaciones()
        // Solo debug: cmd jobscheduler run -f no saltea el initialDelay de WorkManager.
        // adb shell am start -n com.blair.cumpleanos/.MainActivity --ez tick true
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("tick", false) == true) {
            WorkManager.getInstance(this)
                .enqueue(OneTimeWorkRequestBuilder<TickWorker>().build())
        }
        val app = application as CumpleanosApp
        val vista = if (BuildConfig.DEBUG) intent?.getStringExtra("vista") else null
        if (vista != null) {
            setContent { RecordarCumpleanosTheme { com.blair.cumpleanos.ui.VistaPrevia(vista) } }
            return
        }
        // El show de apertura va solo en arranque en frio, no al rotar.
        val arranqueEnFrio = savedInstanceState == null
        setContent {
            RecordarCumpleanosTheme {
                val vm: ListaViewModel = viewModel(factory = ListaViewModel.factory(app.repo, app.db))
                var enSplash by rememberSaveable { mutableStateOf(arranqueEnFrio) }
                val encendido = remember { Animatable(if (arranqueEnFrio) 0f else 1f) }
                val personas by vm.personas.collectAsStateWithLifecycle()
                val pasados by vm.pasados.collectAsStateWithLifecycle()
                val proximos by vm.proximos.collectAsStateWithLifecycle()
                val vacio = personas.isEmpty() && pasados.isEmpty() && proximos.isEmpty()

                Box(Modifier.fillMaxSize()) {
                    // Detras de TODO. Completa en el splash y con la lista vacia;
                    // atenuada con tarjetas encima, o no se lee nada.
                    Escenario(
                        intensidad = if (enSplash || vacio) Intensidad.COMPLETA else Intensidad.ATENUADA,
                        encendido = { encendido.value },
                    )
                    Crossfade(targetState = enSplash, animationSpec = tween(550), label = "apertura") { splash ->
                        if (splash) {
                            SplashPantalla(encendido) { enSplash = false }
                        } else {
                            Contenido(vm)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Contenido(vm: ListaViewModel) {
        val pasados by vm.pasados.collectAsStateWithLifecycle()
        val proximos by vm.proximos.collectAsStateWithLifecycle()
        val personas by vm.personas.collectAsStateWithLifecycle()
        val historial by vm.historial.collectAsStateWithLifecycle()
        val mes by vm.mes.collectAsStateWithLifecycle()
        val porAnio by vm.porAnio.collectAsStateWithLifecycle()
        var enAlta by remember { mutableStateOf(false) }
        val personaAbierta by vm.personaAbierta.collectAsStateWithLifecycle()
        val colision by vm.colision.collectAsStateWithLifecycle()
        val conHistorial by vm.conHistorial.collectAsStateWithLifecycle()

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.ime),
            bottomBar = { BannerAdaptativo() },
            floatingActionButton = {
                if (!enAlta) {
                    ExtendedFloatingActionButton(
                        onClick = { enAlta = true },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.accion_agregar)) },
                    )
                }
            },
        ) { innerPadding ->
            if (enAlta) {
                AltaPantalla(
                    onGuardar = { nombre, dia, mes, anio, preav ->
                        vm.crear(nombre, dia, mes, anio, preav)
                        enAlta = false
                    },
                    onCancelar = { enAlta = false },
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
            ListaPantalla(
                pasados = pasados,
                proximos = proximos,
                personas = personas,
                historial = historial,
                mes = mes,
                porAnio = porAnio,
                onSaludar = { f -> vm.saludar(f.c.personaId, f.c.id) },
                onEditarPersona = { id -> vm.abrirPersona(id) },
                onEliminarPersona = { id -> vm.eliminarPersona(id) },
                onBorrarHistorial = { vm.borrarHistorial() },
                onBorrarHistorialDe = { id -> vm.borrarHistorialDe(id) },
                conHistorial = conHistorial,
                onMesAnterior = vm::mesAnterior,
                onMesSiguiente = vm::mesSiguiente,
                onAlternarPorAnio = vm::alternarPorAnio,
                modifier = Modifier.padding(innerPadding),
            )
            }
        }

        personaAbierta?.let { p ->
            HojaPersona(
                persona = p,
                colision = colision,
                onGuardar = { editada, dia, mes ->
                    vm.editarPersona(editada, dia, mes) { vm.abrirPersona(null) }
                },
                onEliminar = { vm.eliminarPersona(p.id); vm.abrirPersona(null) },
                onBorrarHistorial = { vm.borrarHistorialDe(p.id) },
                onCerrar = { vm.abrirPersona(null) },
            )
        }
    }
}
