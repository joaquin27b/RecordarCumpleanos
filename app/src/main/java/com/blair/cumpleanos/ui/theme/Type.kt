package com.blair.cumpleanos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.blair.cumpleanos.R

/**
 * Outfit (OFL, licencias/OFL-Outfit.txt) empaquetada en res/font. Nada de
 * fuente descargable: depende de Play Services y de red en el primer arranque,
 * y mientras tanto la app sale en Roboto.
 */
val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
    Font(R.font.outfit_black, FontWeight.Black),
)

private val base = Typography()

private fun TextStyle.o(peso: FontWeight? = null, tracking: Float? = null): TextStyle = copy(
    fontFamily = Outfit,
    fontWeight = peso ?: fontWeight,
    letterSpacing = tracking?.sp ?: letterSpacing,
)

/**
 * Toda la escala de M3 en Outfit: un estilo sin tocar vuelve a Roboto y se nota
 * al lado de los otros. Titulares con tracking apretado, etiquetas abiertas.
 */
val Tipografia = Typography(
    displayLarge = base.displayLarge.o(FontWeight.Black, -1.5f),
    displayMedium = base.displayMedium.o(FontWeight.Black, -1f),
    displaySmall = base.displaySmall.o(FontWeight.ExtraBold, -0.5f),
    headlineLarge = base.headlineLarge.o(FontWeight.ExtraBold, -0.5f),
    headlineMedium = base.headlineMedium.o(FontWeight.ExtraBold, -0.5f),
    headlineSmall = base.headlineSmall.o(FontWeight.Bold, -0.25f),
    titleLarge = base.titleLarge.o(FontWeight.Bold, -0.2f),
    titleMedium = base.titleMedium.o(FontWeight.Bold),
    titleSmall = base.titleSmall.o(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.o(),
    bodyMedium = base.bodyMedium.o(),
    bodySmall = base.bodySmall.o(),
    labelLarge = base.labelLarge.o(FontWeight.Bold, 0.4f),
    labelMedium = base.labelMedium.o(FontWeight.Bold, 0.6f),
    labelSmall = base.labelSmall.o(FontWeight.Bold, 1f),
)
