package io.github.youndie.mutant.type

import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface
import java.io.File

/** The bundled font files as Skia sees them, read straight from the module's resources. */
internal object BundledFonts {
    private val dir = File("src/commonMain/composeResources/font")

    fun typeface(name: String): Typeface =
        FontMgr.default.makeFromData(Data.makeFromBytes(File(dir, "$name.ttf").readBytes()))
            ?: error("$name.ttf is not a font Skia can read")

    val handjetRegular: Typeface by lazy { typeface("handjet_regular") }
    val handjetSemiBold: Typeface by lazy { typeface("handjet_semibold") }
    val rubikRegular: Typeface by lazy { typeface("rubik_regular") }
    val rubikMedium: Typeface by lazy { typeface("rubik_medium") }

    val all: List<Typeface> get() = listOf(handjetRegular, handjetSemiBold, rubikRegular, rubikMedium)

    /** The vendored design's pages, the corpus every string the kit renders is copied from. */
    val designPages: List<File> =
        File("../reference/design-system")
            .listFiles { f -> f.name.endsWith(".dc.html") }
            .orEmpty()
            .sortedBy { it.name }
}
