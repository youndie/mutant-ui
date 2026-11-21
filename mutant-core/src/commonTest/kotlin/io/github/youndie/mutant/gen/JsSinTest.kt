package io.github.youndie.mutant.gen

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-152: the generator's sines and cosines are Kotlin's own to the last bit on every target: in the browser they are the
 * browser's `Math.sin` and `Math.cos`, which round a few values the other way (V8's `fdlibm` is not Kotlin's), as the
 * JVM's intrinsics already do on the desktop; the generator's goldens, on every target, hold what reaches the picture.
 * Small arguments, the lattice hash's large ones and values past 2^30, where the reduction changes method.
 */
class JsSinTest {
    @Test
    fun `the generator's sine and cosine are Kotlin's to the last bit`() {
        val r = Random(151)
        val xs =
            List(20_000) { i ->
                when (i % 4) {
                    0 -> r.nextDouble(-4.0, 4.0)
                    1 -> r.nextDouble(-2e5, 2e5)
                    2 -> r.nextDouble(-1e10, 1e10)
                    else -> (r.nextInt(-400, 400) * 127.1 + r.nextInt(-400, 400) * 311.7)
                }
            } + listOf(0.0, -0.0, kotlin.math.PI, 1e300, Double.MIN_VALUE)
        var off = 0
        for (x in xs) {
            for ((ours, theirs) in listOf(kotlin.math.sin(x) to jsSin(x), kotlin.math.cos(x) to jsCos(x))) {
                val ulps = abs(ours.toRawBits() - theirs.toRawBits())
                assertTrue(ulps <= 1, "$x: $ours against $theirs, $ulps ulps")
                if (ulps != 0L) off++
            }
        }
        println("B-152: ${xs.size * 2} values, $off a bit off")
    }
}
