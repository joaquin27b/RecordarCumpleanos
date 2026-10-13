package com.blair.cumpleanos.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Paleta RECITAL, muestreada del poster de referencia. Modo UNICO: no hay
// variante clara ni oscura. Todos los colores de aca son OPACOS: un
// .copy(alpha = X) REEMPLAZA el canal alfa, asi que la transparencia se
// aplica en el punto de uso y nunca viene horneada en el token.

// Noche: de abajo hacia arriba, el escenario se aclara al centro.
val NochePiso = Color(0xFF080030)
val NocheBase = Color(0xFF100038)
val NocheBase2 = Color(0xFF100040)
val NocheMedia = Color(0xFF180848)
val NocheMedia2 = Color(0xFF181060)
val Resplandor = Color(0xFF202078)
val Resplandor2 = Color(0xFF300870)
val Resplandor3 = Color(0xFF300078)

// Haces de luz, magenta a violeta.
val HazMagenta = Color(0xFF8828F0)
val HazMagenta2 = Color(0xFF9028F0)
val HazLavanda = Color(0xFF8868E8)
val HazVioleta = Color(0xFF7840E0)
val HazFucsia = Color(0xFFC840E0)

// Violeta de escenario: superficies elevadas.
val Escenario1 = Color(0xFF4018B0)
val Escenario2 = Color(0xFF4818B8)
val Escenario3 = Color(0xFF4020D0)
val EscenarioBorde = Color(0xFF7040F8)

// Cian: mascota, luces frias, estado activo.
val Cian = Color(0xFF08A8F8)
val Cian2 = Color(0xFF20B0F8)
val Cian3 = Color(0xFF10C8F8)
val CianNeon = Color(0xFF30F0F8)
val CianClaro = Color(0xFF48C8F8)
val CianProfundo = Color(0xFF0880F8)

// Dorado: destacado, llama, lucecitas del publico.
val Dorado = Color(0xFFF8D818)
val Dorado2 = Color(0xFFF0C010)
val Dorado3 = Color(0xFFE8B008)

// Rojo de llamada a la accion: urgencia, error, "cumple hoy".
val RojoCta = Color(0xFFE03840)
val RojoCta2 = Color(0xFFD83038)
val RojoProfundo = Color(0xFFA80808)

// Cromado del titulo, de arriba hacia abajo.
val Cromo1 = Color(0xFFF8F8F8)
val Cromo2 = Color(0xFFF8E8F8)
val Cromo3 = Color(0xFFF0C0F8)
val Cromo4 = Color(0xFFE8A0F8)

/** Destellos, confeti y texto principal. Nunca blanco puro de pantalla. */
val BlancoEscena = Color(0xFFF8F8F8)

/** Cachetes de Velita. */
val Coral = Color(0xFFF84058)

/** Texto secundario: blanco translucido, nunca gris plano. */
val TextoSecundario = BlancoEscena.copy(alpha = 0.74f)

/** Texto sobre el dorado y el cian: la noche, no negro. */
val TintaSobreLuz = NocheBase

val DegradeCromo: Brush = Brush.verticalGradient(listOf(Cromo1, Cromo2, Cromo3, Cromo4))
val DegradeRojo: Brush = Brush.horizontalGradient(listOf(RojoCta, RojoProfundo))
val DegradeCian: Brush = Brush.horizontalGradient(listOf(CianProfundo, CianNeon))
