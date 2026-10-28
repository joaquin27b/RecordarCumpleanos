package com.blair.cumpleanos.ui

import android.app.Application
import android.content.Context
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blair.cumpleanos.BuildConfig
import com.blair.cumpleanos.R
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.Escenario1
import com.blair.cumpleanos.ui.theme.EscenarioBorde
import com.blair.cumpleanos.ui.theme.NocheBase
import com.blair.cumpleanos.ui.theme.TextoSecundario
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val TAG = "Nativo"

/**
 * Donde puede ir un nativo. ALTA, HOJA y DIALOGO tapan la lista: de esos se
 * muestra uno solo, el de mayor prioridad (1 gana).
 *
 * Los [enLista] son turnos, no lugares fijos: [ubicarNativos] decide despues de
 * que seccion va cada uno segun cuantas filas hay. Cada turno se pide la primera
 * vez que su ancla queda en pantalla y desde ahi conserva su anuncio aunque la
 * lista cambie y el turno se corra: cargar una persona no pide otro.
 */
enum class LugarNativo(val prioridad: Int, val tamano: TamanoNativo, val enLista: Boolean = false) {
    /** AltaPantalla, debajo de Guardar. Tapa la lista entera: gana a todos. */
    ALTA(1, TamanoNativo.COMPLETO),

    /**
     * Al pie de una hoja abierta: persona, regalo, periodo. Tapa la lista. Mini:
     * toda ventana lleva el nativo mas chico posible. Las confirmaciones y los
     * calendarios no llevan: el anuncio quedaria pegado a Borrar/Cancelar y
     * encima de otro anuncio.
     */
    HOJA(2, TamanoNativo.MINI),

    /** Ventana de borrar historial. El scrim deja ver la lista: la tapa igual que una hoja. */
    DIALOGO(3, TamanoNativo.MINI),

    LISTA_1(4, TamanoNativo.COMPACTO, enLista = true),
    LISTA_2(5, TamanoNativo.COMPACTO, enLista = true),
    LISTA_3(6, TamanoNativo.COMPACTO, enLista = true),
    LISTA_4(7, TamanoNativo.COMPACTO, enLista = true),

    /**
     * Al final de todo el contenido (despues del historial). Mediano, como el del
     * final de Ya La Tomé: con media mas bajo que el del alta, para que no ocupe
     * la pantalla y el scroll no quede bajo el dedo.
     */
    LISTA_FINAL(8, TamanoNativo.MEDIANO, enLista = true),
}

enum class TamanoNativo {
    /** Una linea de alto fijo: etiqueta, icono, titulo y CTA. Para una ventana chica. */
    MINI,

    /** Una fila de alto fijo: etiqueta, icono, titulo, anunciante y CTA. Sin media. */
    COMPACTO,

    /** Con media, topeado a [ALTO_MAX_MEDIA_DP]. Solo en el alta: en la lista traba el scroll. */
    COMPLETO,

    /** Como el completo, con el media topeado a [ALTO_MAX_MEDIA_MEDIANO_DP]. Al final de la lista. */
    MEDIANO,
}

/** Tope del media del completo: una creatividad vertical real ocupaba media pantalla. */
private const val ALTO_MAX_MEDIA_DP = 180

/** Tope del media del mediano. 120 es el minimo de AdMob para el media. */
private const val ALTO_MAX_MEDIA_MEDIANO_DP = 120

/**
 * Aire entre los assets y el borde de la NativeAdView. Pegados al borde, el
 * redondeo de dp a px los dejaba un pixel afuera segun el ancho de pantalla, y
 * el validador marcaba "Advertiser assets outside native ad view" (en la lista
 * si, en el alta no: 16 y 18dp de margen redondean distinto). Lo mismo se
 * arreglo asi en Ya La Tomé. Se descuenta del padding de afuera: el tamaño
 * visible no cambia.
 */
private const val AIRE_ASSETS_DP = 4

/** Alto fijo de la fila del compacto: dos lineas de titulo y una de anunciante. */
internal const val ALTO_COMPACTO_DP = 64

/** Alto fijo del mini: una sola linea de titulo. */
internal const val ALTO_MINI_DP = 44

/**
 * Coordinador con scope de ViewModel: girar el telefono recrea la actividad pero
 * no los anuncios, y no se vuelve a pedir nada. Cada lugar se anota mientras esta en
 * composicion. De ALTA y HOJA solo se pide el activo; los de la lista, al verse.
 *
 * El NativeAd vive ACA, no en la fila de la LazyColumn: la fila solo lo dibuja.
 * Adentro de un item se destruiria y recrearia al scrollear, y con un nativo
 * cada recreacion es una request nueva: invalid traffic, el error del banner.
 *
 * Con una hoja o el alta abiertos la lista queda TAPADA: sus anuncios siguen en
 * su lugar pero invisibles, y al cerrar vuelven los mismos sin pedir otros. Se
 * destruyen cuando el lugar sale de composicion.
 */
@Stable
class CoordinadorNativos(private val ctx: Context, private val pedir: Boolean) {
    private val anotados = mutableStateMapOf<LugarNativo, Int>()
    private val anuncios = mutableStateMapOf<LugarNativo, NativeAd>()
    private val enCurso = mutableSetOf<LugarNativo>()

    /** Fallo una vez: no se reintenta mientras el lugar siga en pantalla. */
    private val fallidos = mutableSetOf<LugarNativo>()

    /** El de ALTA/HOJA/DIALOGO que se muestra; null si no hay ninguno abierto. */
    val activo: LugarNativo? get() = anotados.keys.filter { !it.enLista }.minByOrNull { it.prioridad }

    /** Hay una hoja o el alta encima: los de la lista no se ven. */
    val listaTapada: Boolean get() = activo != null

    /** Los lugares de la lista en pantalla, al detenerse el scroll: se piden los que falten. */
    fun enPantalla(visibles: Set<LugarNativo>) {
        if (listaTapada) return
        visibles.filter { it.enLista && it in anotados }.forEach { pedir(it) }
    }

    /** El anuncio a dibujar en [lugar], o null: no cargo, o es de ALTA/HOJA y no es el activo. */
    fun anuncio(lugar: LugarNativo): NativeAd? =
        if (lugar.enLista || activo == lugar) anuncios[lugar] else null

    fun anotar(lugar: LugarNativo) {
        anotados[lugar] = (anotados[lugar] ?: 0) + 1
    }

    /** [destruir] false al girar: la actividad nueva lo vuelve a anotar y usa el mismo anuncio. */
    fun soltar(lugar: LugarNativo, destruir: Boolean = true) {
        val quedan = (anotados[lugar] ?: 0) - 1
        if (quedan > 0) {
            anotados[lugar] = quedan
            return
        }
        anotados.remove(lugar)
        if (!destruir) return
        fallidos.remove(lugar)
        anuncios.remove(lugar)?.let {
            Log.i(TAG, "destroy lugar=$lugar")
            it.destroy()
        }
    }

    /** Pide para el activo de ALTA/HOJA, si hace falta. */
    fun pedirSiHaceFalta() {
        activo?.let { pedir(it) }
    }

    /** Una request para [lugar], solo si no tiene anuncio, ni pedido en curso, ni fallo. */
    private fun pedir(lugar: LugarNativo) {
        if (!pedir || lugar in anuncios || lugar in enCurso || lugar in fallidos) return
        enCurso += lugar
        Log.i(TAG, "request lugar=$lugar")
        // El del final tiene su propia unidad: se mide aparte en el panel de AdMob.
        val unidad = if (lugar == LugarNativo.LISTA_FINAL) BuildConfig.AD_UNIT_NATIVE_FINAL else BuildConfig.AD_UNIT_NATIVE
        AdLoader.Builder(ctx, unidad)
            .forNativeAd { ad ->
                enCurso -= lugar
                // Llego tarde, con el lugar ya cerrado: no se muestra nunca.
                if (lugar in anotados) anuncios[lugar] = ad else ad.destroy()
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    enCurso -= lugar
                    fallidos += lugar
                    Log.i(TAG, "fallo lugar=$lugar codigo=${error.code} ${error.message}")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    // Sin esto llega cualquier formato, vertical incluido: el test de
                    // Google siempre es apaisado y en debug nunca se veia.
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .build()
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    fun destruirTodo() {
        anuncios.values.forEach { it.destroy() }
        anuncios.clear()
        anotados.clear()
    }
}

/** Dueño del coordinador: sobrevive a la rotacion y destruye todo al cerrar la app. */
class NativosViewModel(app: Application) : AndroidViewModel(app) {
    val coordinador = CoordinadorNativos(app, BuildConfig.PEDIR_ANUNCIO)

    override fun onCleared() = coordinador.destruirTodo()
}

private val LocalNativos = staticCompositionLocalOf<CoordinadorNativos?> { null }

private const val ESPERA_PEDIDO_MS = 400L

/**
 * Le avisa al coordinador que lugares de la lista estan en pantalla. Cada lugar
 * tiene un ancla: la fila junto a la que va su anuncio, que existe aunque el
 * anuncio no. Al salir de composicion (se abre el alta) no queda ninguno.
 *
 * Se mira SOLO cuando el scroll se detiene (y una vez al haber filas): un lugar
 * que se pasa de largo scrolleando rapido no llega a pedir.
 */
@Composable
fun LugaresEnPantalla(estado: LazyListState, anclas: Map<Any, LugarNativo>) {
    val coordinador = LocalNativos.current ?: return
    LaunchedEffect(estado, anclas) {
        snapshotFlow { estado.isScrollInProgress || estado.layoutInfo.visibleItemsInfo.isEmpty() }
            .distinctUntilChanged()
            .filter { quieto -> !quieto }
            .collect {
                coordinador.enPantalla(estado.layoutInfo.visibleItemsInfo.mapNotNullTo(mutableSetOf()) { anclas[it.key] })
            }
    }
}

/** True con una hoja o el alta encima de la lista. */
@Composable
fun listaTapada(): Boolean = LocalNativos.current?.listaTapada ?: false

/** Una pantalla con el coordinador de la actividad. Cada lugar se destruye al soltarse. */
@Composable
fun PantallaConNativos(contenido: @Composable () -> Unit) {
    val coordinador = viewModel<NativosViewModel>().coordinador
    LaunchedEffect(coordinador) {
        // Un lugar que se pasa de largo al scrollear rapido no llega a pedir.
        snapshotFlow { coordinador.activo }.collectLatest {
            delay(ESPERA_PEDIDO_MS)
            coordinador.pedirSiHaceFalta()
        }
    }
    CompositionLocalProvider(LocalNativos provides coordinador) { contenido() }
}

/**
 * Anota [lugar] mientras quien lo llama este en composicion y devuelve su
 * anuncio si es el activo. Llamarlo FUERA de la LazyColumn: adentro, scrollear
 * lo sacaria de composicion y lo destruiria.
 */
@Composable
fun nativoEn(lugar: LugarNativo): NativeAd? {
    val coordinador = LocalNativos.current ?: return null
    val actividad = LocalActivity.current
    DisposableEffect(coordinador, lugar) {
        coordinador.anotar(lugar)
        // Al girar, la composicion se va pero el anuncio queda para la actividad nueva.
        onDispose { coordinador.soltar(lugar, destruir = actividad?.isChangingConfigurations != true) }
    }
    return coordinador.anuncio(lugar)
}

/** Colores del tema pasados a la vista clasica. */
internal data class ColoresNativo(
    val texto: Int,
    val mutado: Int,
    val etiqueta: Int,
    val boton: Int,
    val textoBoton: Int,
)

/**
 * CTA apagado, del violeta de la tarjeta y apenas mas claro: se lee como el boton
 * del anuncio pero se funde con el fondo. No compite con NUEVO CUMPLEAÑOS ni YA
 * SALUDÉ, y no se parece al cian, al degradé, al dorado ni al rojo de la app.
 */
private const val FONDO_CTA = 0xFF3B2A86.toInt()
private const val TINTA_CTA = 0xFFCFC8EE.toInt()

/**
 * El nativo en su propia tarjeta, con el lenguaje de la app: esquinas de 24dp,
 * violeta translucido, borde de 1dp y el padding de las tarjetas de cumpleaños.
 * Sin halo ni neon: no se confunde con un cumpleaños. Etiqueta "Anuncio" arriba
 * a la izquierda, AdChoices arriba a la derecha. El alto tiene tope en los dos
 * tamaños: un titular largo o un media vertical no estiran la tarjeta.
 * Solo se llama con un anuncio cargado: sin anuncio no hay tarjeta ni hueco.
 */
@Composable
fun TarjetaNativo(anuncio: NativeAd, tamano: TamanoNativo, modifier: Modifier = Modifier) {
    val colores = ColoresNativo(
        texto = BlancoEscena.toArgb(),
        mutado = TextoSecundario.toArgb(),
        etiqueta = BlancoEscena.toArgb(),
        boton = FONDO_CTA,
        textoBoton = TINTA_CTA,
    )
    val etiqueta = stringResource(R.string.anuncio_etiqueta)
    val forma = RoundedCornerShape(24.dp)
    val caja = modifier
        .fillMaxWidth()
        .clip(forma)
        // Solido: translucido, el confeti y los destellos del escenario se veian a
        // traves del anuncio y le cortaban el borde.
        .background(Escenario1.copy(alpha = 0.55f).compositeOver(NocheBase))
        .border(1.dp, EscenarioBorde.copy(alpha = 0.6f), forma)
    if (tamano == TamanoNativo.MINI || tamano == TamanoNativo.COMPACTO) {
        val mini = tamano == TamanoNativo.MINI
        AndroidView(
            factory = { c -> VistaNativoCompacto(c, mini) },
            update = { it.mostrar(anuncio, colores, etiqueta) },
            onRelease = { it.raiz.destroy() },
            modifier = caja.padding(horizontal = (12 - AIRE_ASSETS_DP).dp, vertical = if (mini) 2.dp else 6.dp),
        )
        return
    }
    AndroidView(
        factory = { c ->
            VistaNativo(
                c,
                if (tamano == TamanoNativo.MEDIANO) ALTO_MAX_MEDIA_MEDIANO_DP else ALTO_MAX_MEDIA_DP,
                radioClipDp = 16 - AIRE_ASSETS_DP,
            )
        },
        update = { it.mostrar(anuncio, colores, etiqueta) },
        // Libera la VISTA; el NativeAd lo destruye el coordinador.
        onRelease = { it.raiz.destroy() },
        modifier = caja.padding((16 - AIRE_ASSETS_DP).dp),
    )
}

/**
 * Mide su hijo con el alto que pide la relacion de aspecto del media, sin pasar
 * de [altoMaxDp]: el inventario real trae cualquier aspecto, vertical incluido.
 * Nunca menos de 120dp, el minimo de AdMob.
 */
internal class CajaAspecto(ctx: Context, private val altoMaxDp: Int) : FrameLayout(ctx) {
    var aspecto = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val ancho = MeasureSpec.getSize(widthMeasureSpec)
        if (ancho == 0) return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val densidad = resources.displayMetrics.density
        val tope = (altoMaxDp * densidad).toInt()
        val natural = if (aspecto > 0f) (ancho / aspecto).toInt() else tope
        val minimo = (120 * densidad).toInt()
        // Con tope, FIT_CENTER achica la imagen entera: no se recorta.
        val alto = maxOf(minOf(natural, tope), minimo)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(alto, MeasureSpec.EXACTLY))
    }
}

/**
 * La NativeAdView armada en codigo, sin layout XML. Adaptada de Por Vencer.
 *
 * [radioClipDp]: radio con el que la vista se recorta a si misma, en dp. Compose
 * .clip(forma) NO redondea una View dentro de un AndroidView: la View dibuja en
 * su propia capa rectangular. Con los IDs de test el media viene chico y centrado
 * (FIT_CENTER) y no se nota, pero una creatividad real con media a pantalla
 * completa pintaba esquinas cuadradas por encima del borde redondeado de la
 * tarjeta. Con clipToOutline + este outline la vista respeta la forma. El radio
 * es el inset de la tarjeta (el padding de afuera): asi el arco de la vista queda
 * CONCENTRICO con el del borde, a esa distancia adentro. Solo el completo/mediano
 * lleva media; el compacto/mini no desbordan y no clipean.
 */
internal class VistaNativo(ctx: Context, altoMaxMedia: Int, radioClipDp: Int) : LinearLayout(ctx) {
    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics,
    ).toInt()

    val raiz = NativeAdView(ctx)
    private val etiqueta = TextView(ctx)
    private val icono = ImageView(ctx)
    private val titulo = TextView(ctx)
    private val anunciante = TextView(ctx)
    private val cuerpo = TextView(ctx)
    private val caja = CajaAspecto(ctx, altoMaxMedia)
    private val media = MediaView(ctx)
    private val accion = TextView(ctx)

    init {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dp(radioClipDp).toFloat())
            }
        }
        clipToOutline = true

        val contenido = LinearLayout(ctx).apply {
            orientation = VERTICAL
            setPadding(dp(AIRE_ASSETS_DP), dp(AIRE_ASSETS_DP), dp(AIRE_ASSETS_DP), dp(AIRE_ASSETS_DP))
        }

        val cabecera = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        etiqueta.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8), dp(2), dp(8), dp(2))
        }
        cabecera.addView(etiqueta, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        // A la derecha va el AdChoices: lo superpone el SDK (ADCHOICES_TOP_RIGHT).
        cabecera.addView(View(ctx), LayoutParams(0, 0, 1f))
        contenido.addView(cabecera, LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val fila = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        icono.adjustViewBounds = true
        fila.addView(icono, LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) })
        val textos = LinearLayout(ctx).apply { orientation = VERTICAL }
        titulo.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        anunciante.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        textos.addView(titulo)
        textos.addView(anunciante)
        fila.addView(textos, LayoutParams(0, WRAP_CONTENT, 1f))
        contenido.addView(fila, LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })

        cuerpo.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        contenido.addView(cuerpo, LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) })

        // FIT_CENTER: la imagen entra entera, sin recorte. El alto lo pone CajaAspecto.
        media.setImageScaleType(ImageView.ScaleType.FIT_CENTER)
        caja.addView(media, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        contenido.addView(caja, LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })

        accion.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(24), 0, dp(24), 0)
        }
        contenido.addView(accion, LayoutParams(MATCH_PARENT, dp(44)).apply { topMargin = dp(12) })

        raiz.addView(contenido, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        raiz.headlineView = titulo
        raiz.bodyView = cuerpo
        raiz.iconView = icono
        raiz.advertiserView = anunciante
        raiz.callToActionView = accion
        raiz.mediaView = media
        addView(raiz, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private var actual: NativeAd? = null

    fun mostrar(ad: NativeAd, c: ColoresNativo, textoEtiqueta: String) {
        etiqueta.text = textoEtiqueta
        etiqueta.setTextColor(c.etiqueta)
        etiqueta.background = GradientDrawable().apply {
            cornerRadius = dp(6).toFloat()
            setStroke(dp(1), c.etiqueta)
        }
        titulo.setTextColor(c.texto)
        anunciante.setTextColor(c.mutado)
        cuerpo.setTextColor(c.mutado)
        accion.setTextColor(c.textoBoton)
        accion.background = GradientDrawable().apply {
            cornerRadius = dp(22).toFloat()
            setColor(c.boton)
        }
        // Se vincula una vez por anuncio: recomponer no lo vuelve a registrar.
        if (actual === ad) return
        actual = ad

        titulo.text = ad.headline
        cuerpo.text = ad.body
        cuerpo.visibility = if (ad.body.isNullOrBlank()) GONE else VISIBLE
        anunciante.text = ad.advertiser
        anunciante.visibility = if (ad.advertiser.isNullOrBlank()) GONE else VISIBLE
        icono.setImageDrawable(ad.icon?.drawable)
        icono.visibility = if (ad.icon == null) GONE else VISIBLE
        accion.text = ad.callToAction
        accion.visibility = if (ad.callToAction.isNullOrBlank()) GONE else VISIBLE
        val contenidoMedia = ad.mediaContent
        caja.aspecto = contenidoMedia?.aspectRatio ?: 0f
        if (contenidoMedia != null) media.mediaContent = contenidoMedia
        caja.visibility = if (contenidoMedia == null) GONE else VISIBLE
        raiz.setNativeAd(ad)
    }
}

/**
 * El nativo de la lista, como el "small template" de Google: UNA fila de alto
 * FIJO con etiqueta "Anuncio", icono, titulo (2 lineas con "…"), anunciante
 * (1 linea) y CTA. Sin media. Un titular largo no estira la tarjeta. El CTA deja
 * libre la esquina de arriba a la derecha, donde el SDK pone AdChoices.
 *
 * [mini]: una sola linea de titulo, sin anunciante, mas bajo. Para ventanas.
 */
internal class VistaNativoCompacto(ctx: Context, private val mini: Boolean = false) : LinearLayout(ctx) {
    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics,
    ).toInt()

    val raiz = NativeAdView(ctx)
    private val etiqueta = TextView(ctx)
    private val icono = ImageView(ctx)
    private val titulo = TextView(ctx)
    private val anunciante = TextView(ctx)
    private val accion = TextView(ctx)

    init {
        val fila = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(AIRE_ASSETS_DP), dp(2), dp(AIRE_ASSETS_DP), dp(2))
        }
        etiqueta.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(5), dp(1), dp(5), dp(1))
        }
        fila.addView(etiqueta, LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = dp(8) })
        icono.adjustViewBounds = true
        val ladoIcono = if (mini) 24 else 28
        fila.addView(icono, LayoutParams(dp(ladoIcono), dp(ladoIcono)).apply { marginEnd = dp(8) })
        titulo.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = if (mini) 1 else 2
            ellipsize = TextUtils.TruncateAt.END
        }
        anunciante.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val textos = LinearLayout(ctx).apply { orientation = VERTICAL }
        textos.addView(titulo)
        textos.addView(anunciante)
        fila.addView(textos, LayoutParams(0, WRAP_CONTENT, 1f))
        accion.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
        }
        fila.addView(
            accion,
            LayoutParams(WRAP_CONTENT, dp(30)).apply {
                marginStart = dp(8)
                marginEnd = dp(20)
            },
        )

        raiz.addView(fila, FrameLayout.LayoutParams(MATCH_PARENT, dp(if (mini) ALTO_MINI_DP else ALTO_COMPACTO_DP)))
        raiz.headlineView = titulo
        raiz.advertiserView = anunciante
        if (!mini) raiz.iconView = icono
        raiz.callToActionView = accion
        addView(raiz, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private var actual: NativeAd? = null

    fun mostrar(ad: NativeAd, c: ColoresNativo, textoEtiqueta: String) {
        etiqueta.text = textoEtiqueta
        etiqueta.setTextColor(c.etiqueta)
        etiqueta.background = GradientDrawable().apply {
            cornerRadius = dp(5).toFloat()
            setStroke(dp(1), c.etiqueta)
        }
        titulo.setTextColor(c.texto)
        anunciante.setTextColor(c.mutado)
        accion.setTextColor(c.textoBoton)
        accion.background = GradientDrawable().apply {
            cornerRadius = dp(15).toFloat()
            setColor(c.boton)
        }
        // Se vincula una vez por anuncio: recomponer no lo vuelve a registrar.
        if (actual === ad) return
        actual = ad

        titulo.text = ad.headline
        anunciante.text = ad.advertiser
        anunciante.visibility = if (mini || ad.advertiser.isNullOrBlank()) GONE else VISIBLE
        icono.setImageDrawable(ad.icon?.drawable)
        // En el mini, sin icono (es opcional): en una ventana angosta el titulo
        // quedaba en "Anunci…".
        icono.visibility = if (mini || ad.icon == null) GONE else VISIBLE
        accion.text = ad.callToAction
        accion.visibility = if (ad.callToAction.isNullOrBlank()) GONE else VISIBLE
        raiz.setNativeAd(ad)
    }
}
