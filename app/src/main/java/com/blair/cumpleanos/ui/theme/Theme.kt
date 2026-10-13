package com.blair.cumpleanos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/*
 * Modo UNICO, el del recital. No mira el ajuste claro/oscuro del telefono y no
 * usa color dinamico a proposito: Material You toma los colores del fondo de
 * pantalla, pisa la paleta de marca en Android 12+ y la app sale gris. Ya paso
 * dos veces.
 */
private val Esquema = darkColorScheme(
    primary = CianNeon,
    onPrimary = TintaSobreLuz,
    primaryContainer = Escenario1,
    onPrimaryContainer = BlancoEscena,
    secondary = Dorado,
    onSecondary = TintaSobreLuz,
    secondaryContainer = Escenario2,
    onSecondaryContainer = BlancoEscena,
    tertiary = HazFucsia,
    onTertiary = BlancoEscena,
    background = NocheBase,
    onBackground = BlancoEscena,
    surface = NocheMedia,
    onSurface = BlancoEscena,
    surfaceVariant = Resplandor,
    // Opaco: el esquema no lleva alfa. Equivale al blanco al 75% sobre violeta.
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD2CCEC),
    surfaceTint = Escenario1,
    surfaceContainerLowest = NochePiso,
    surfaceContainerLow = NocheBase,
    surfaceContainer = NocheMedia,
    surfaceContainerHigh = NocheMedia2,
    surfaceContainerHighest = Resplandor,
    outline = EscenarioBorde,
    outlineVariant = Escenario1,
    error = RojoCta,
    onError = BlancoEscena,
    errorContainer = RojoProfundo,
    onErrorContainer = BlancoEscena,
    scrim = NochePiso,
)

/** Esquinas de tarjeta: una sola medida para que el ritmo de la lista no se rompa. */
val RadioTarjeta = 26.dp

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(RadioTarjeta),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RecordarCumpleanosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, typography = Tipografia, shapes = Formas, content = content)
}
