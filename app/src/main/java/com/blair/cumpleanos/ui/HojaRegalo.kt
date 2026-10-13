package com.blair.cumpleanos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.ui.escena.Escenario
import com.blair.cumpleanos.ui.escena.Intensidad
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.NocheMedia
import com.blair.cumpleanos.ui.theme.TextoSecundario

/** Campo del regalo con su icono. Vacio se guarda como NULL: "sin regalo anotado". */
@Composable
fun CampoRegalo(valor: String, onCambio: (String) -> Unit, etiqueta: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_regalo), contentDescription = null, tint = Dorado, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        CampoTexto(
            valor = valor,
            onCambio = { onCambio(it.take(120)) },
            etiqueta = etiqueta,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Hoja del regalo: al saludar (confirma o cambia el plan antes de guardar el
 * saludo) y desde el historial (edita el regalo real cuando sea).
 *
 * @param inicial lo que se precarga: el plan al saludar, el real en el historial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaRegalo(
    titulo: String,
    cuerpo: String?,
    etiqueta: String,
    inicial: String?,
    textoBoton: String,
    onGuardar: (String?) -> Unit,
    onCerrar: () -> Unit,
) {
    var texto by rememberSaveable { mutableStateOf(inicial ?: "") }
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NocheMedia,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CianNeon.copy(alpha = 0.6f)) },
    ) {
        Box {
            Escenario(Intensidad.ATENUADA, Modifier.matchParentSize())
            Column(
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        titulo,
                        color = BlancoEscena,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(10.dp))
                    DestelloIcono(Dorado, 16.dp)
                }
                if (cuerpo != null) {
                    Text(cuerpo, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
                }
                CampoRegalo(texto, { texto = it }, etiqueta)
                BotonPrincipal(textoBoton, onClick = { onGuardar(texto) }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = onCerrar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.accion_cancelar), color = CianNeon)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
