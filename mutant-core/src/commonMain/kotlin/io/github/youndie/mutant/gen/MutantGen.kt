package io.github.youndie.mutant.gen

/**
 * The generator's package (research D1, D3): the 1:1 port of `mutant-generator.js` lands here in B-03.
 * Numbers in, path nodes out; nothing in this package imports Compose, so the generator could become
 * a module of its own by a move. `GeneratorBoundaryTest` holds that.
 */
internal const val GEN_PACKAGE: String = "io.github.youndie.mutant.gen"
