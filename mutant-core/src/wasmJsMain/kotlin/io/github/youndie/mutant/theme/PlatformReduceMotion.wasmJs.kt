package io.github.youndie.mutant.theme

import androidx.compose.runtime.Composable
import kotlinx.browser.window

@Composable
internal actual fun platformReduceMotion(): Boolean = window.matchMedia("(prefers-reduced-motion: reduce)").matches
