package com.blair.cumpleanos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pedirPermisoNotificaciones()
        // Solo debug: cmd jobscheduler run -f no saltea el initialDelay de WorkManager.
        // adb shell am start -n com.blair.cumpleanos/.MainActivity --ez tick true
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("tick", false) == true) {
            WorkManager.getInstance(this)
                .enqueue(OneTimeWorkRequestBuilder<TickWorker>().build())
        }
        val app = application as CumpleanosApp
        setContent {
            RecordarCumpleanosTheme {
                val vm: ListaViewModel = viewModel(factory = ListaViewModel.factory(app.repo, app.db))
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
    }
}
