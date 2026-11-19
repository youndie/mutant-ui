package io.github.youndie.mutant.sample

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectBase
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCValues
import platform.Foundation.NSStringFromClass
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIApplicationDelegateProtocolMeta
import platform.UIKit.UIApplicationMain
import platform.UIKit.UIResponder
import platform.UIKit.UIResponderMeta
import platform.UIKit.UIScreen
import platform.UIKit.UIWindow

/**
 * B-138: the sample on iOS, a host with no Xcode project, as `scripts/sim-bench.sh` bundles it for the simulator. The
 * same `MutantUiSample` as the desktop window and the browser, or B-138's frame bench when launched with `-bench YES`
 * ([IosBench]).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun main(args: Array<String>) {
    memScoped {
        val argv = (arrayOf("mutant-sample") + args).map { it.cstr.ptr }.toCValues()
        UIApplicationMain(args.size + 1, argv, null, NSStringFromClass(SampleAppDelegate))
    }
}

@OptIn(BetaInteropApi::class)
private class SampleAppDelegate
    @ObjCObjectBase.OverrideInit
    constructor() :
    UIResponder(),
        UIApplicationDelegateProtocol {
        companion object : UIResponderMeta(), UIApplicationDelegateProtocolMeta

        private var shown: UIWindow? = null

        override fun window(): UIWindow? = shown

        override fun setWindow(window: UIWindow?) {
            shown = window
        }

        @OptIn(ExperimentalForeignApi::class)
        override fun application(
            application: UIApplication,
            didFinishLaunchingWithOptions: Map<Any?, *>?,
        ): Boolean {
            val defaults = NSUserDefaults.standardUserDefaults
            val bench = defaults.boolForKey("bench")
            shown =
                UIWindow(frame = UIScreen.mainScreen.bounds).apply {
                    rootViewController =
                        ComposeUIViewController {
                            if (bench) {
                                IosBench(
                                    rounds = defaults.integerForKey("rounds").toInt().takeIf { it > 0 } ?: 6,
                                    sceneMs = defaults.integerForKey("sceneMs").takeIf { it > 0 } ?: 10_000L,
                                    only = defaults.stringForKey("scenes")?.split(','),
                                )
                            } else {
                                MutantUiSample()
                            }
                        }
                    makeKeyAndVisible()
                }
            return true
        }
    }
