package io.github.youndie.mutant.theme

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
internal actual fun platformReduceMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
