// The sample kit: every component grouped as the design's pages group them, and later the Demo
// page's three screens (B-37). Not published, not a dependency of anything: `./gradlew :sample:run`
// on the desktop, `:sample:wasmJsBrowserDistribution` for the browser.
plugins {
    alias(wip.plugins.kotlinMultiplatform)
    alias(wip.plugins.composeMultiplatform)
    alias(wip.plugins.composeCompiler)
    id("io.github.youndie.sborka.kmp")
    id("io.github.youndie.sborka.lint")
}

kotlin {
    // A sample, not a library: sborka's explicit API mode guards a published surface it does not have.
    explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled

    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser { commonWebpackConfig { outputFileName = "sample.js" } }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":mutant-core"))
            implementation(wip.compose.runtime)
            implementation(wip.compose.foundation)
            implementation(wip.compose.ui)
        }
        getByName("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
        }
        getByName("desktopTest").dependencies {
            implementation(kotlin("test"))
            implementation(wip.compose.ui.test)
        }
    }
}

compose.desktop.application {
    mainClass = "io.github.youndie.mutant.sample.MainKt"
    nativeDistributions { packageName = "mutant-ui" }
}

// B-71: a test task is never taken from the build cache. Under `allTests` and `check`, Kotlin lets a test task
// with failing tests succeed and fails the aggregate after it, so the failed run is stored like any other; a
// fresh checkout (every CI run) then loads it and the build passes without a single test having run.
tasks.withType<AbstractTestTask>().configureEach {
    outputs.cacheIf("a cached test run replays its failures as a pass (B-71)") { false }
}
