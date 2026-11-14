// The library: everything a consumer of mutant-ui depends on (research D1). One module; the generator
// lives in `io.github.youndie.mutant.gen` and imports nothing from Compose, so a split stays a move.
// No Material artefact, no sample and no design references here.
plugins {
    alias(wip.plugins.kotlinMultiplatform)
    alias(wip.plugins.composeMultiplatform)
    alias(wip.plugins.composeCompiler)
    alias(wip.plugins.androidKotlinMultiplatformLibrary)
    id("io.github.youndie.sborka.kmp")
    id("io.github.youndie.sborka.lint")
    // Every push to `main` publishes `<version>.<run>` to the Reposilite (B-41).
    id("io.github.youndie.sborka.publish")
}

// Desktop, Android, iOS and wasm. Desktop is where the screenshot and parity gate runs, so a green
// `check` says nothing visual about the other three (research D12).
kotlin {
    jvm("desktop")

    // The public API is pinned: `mutant-core/api/` is the committed dump, and `check` fails on any
    // change to it that was not recorded with `updateKotlinAbi`.
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {}

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // Required by the Compose plugin's own check (CMP-4906): without an executable binary the
        // Skiko runtime cannot be bundled for the target's tests, and `build` fails on it.
        binaries.executable()
    }

    iosArm64()
    iosSimulatorArm64()

    android {
        // Off by default in AGP's KMP plugin, and with it off the AAR ships without the bundled
        // fonts B-02 adds, green (kvadrant-ui B-37).
        androidResources { enable = true }

        namespace = "io.github.youndie.mutant"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    // `foundation` and nothing above it (research D1).
    sourceSets {
        commonMain.dependencies {
            implementation(wip.compose.runtime)
            implementation(wip.compose.foundation)
            implementation(wip.compose.ui)
            implementation(libs.navigationevent.compose)
            // The bundled fonts (B-02). Not in `wip`, and the plugin's accessor is the one place its
            // version follows the Compose plugin's own (toon-ui and plakat-ui do the same).
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("desktopTest").dependencies {
            // The host's skia. Never in a published source set: the POM would pin the machine
            // that published.
            implementation(compose.desktop.currentOs)
            implementation(wip.compose.ui.test)
            // The tokens' second reading (B-08) parses mutant-tokens.json on its own.
            implementation(wip.kotlinx.serialization.json)
        }
    }
}

compose.resources {
    packageOfResClass = "io.github.youndie.mutant.resources"
}

// A test that reads files outside the compiled classpath declares them, or Gradle leaves it
// UP-TO-DATE over a change and reports the last run's verdict. The generator-boundary test reads the
// generator's sources; later tests read the vendored design.
tasks.withType<Test>().configureEach {
    inputs
        .dir(layout.projectDirectory.dir("src/commonMain/kotlin/io/github/youndie/mutant/gen"))
        .withPropertyName("generatorSources")
        .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    // The floor test (B-05) and later parity read the Chrome references.
    inputs
        .dir(layout.projectDirectory.dir("src/desktopTest/snapshots"))
        .withPropertyName("designReferences")
        .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    // The font tests read the bundled files straight from the resources directory.
    inputs
        .dir(layout.projectDirectory.dir("src/commonMain/composeResources/font"))
        .withPropertyName("fonts")
        .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    inputs
        .dir(rootProject.layout.projectDirectory.dir("reference/design-system"))
        .withPropertyName("designSystem")
        .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
}

// The shared iOS metadata compilation, and only it, without warnings-as-errors. CMP 1.12's own graph
// puts two lifecycle klibs with one `unique_name` on it, and the KLIB loader's warning about that is
// not something this module can fix (toon-ui, oldge-ui and plakat-ui made the same cut). The
// per-target iOS compilations keep -Werror, so a warning in this library's own code still fails.
tasks
    .matching {
        it.name in
            setOf("compileIosMainKotlinMetadata", "compileAppleMainKotlinMetadata", "compileNativeMainKotlinMetadata")
    }.configureEach {
        (this as org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>).compilerOptions.allWarningsAsErrors.set(
            false,
        )
    }

// B-06: the field's cost on the desktop JVM, run by hand into logs/b-06/; measures nothing in `check`.
// `./gradlew :mutant-core:fieldBench --args="$(git rev-parse --short HEAD)" > logs/b-06/<date>-desktop.log`
tasks.register<JavaExec>("fieldBench") {
    group = "verification"
    description = "Measures the generator's instance and frame cost; not part of check."
    mainClass = "io.github.youndie.mutant.bench.FieldBenchKt"
    val test =
        kotlin.targets
            .getByName("desktop")
            .compilations
            .getByName("test")
    classpath = test.output.allOutputs + test.runtimeDependencyFiles!!
    dependsOn(test.compileAllTaskName)
    notCompatibleWithConfigurationCache("a measurement, run by hand")
}

// B-50: the main thread's frame when ten cards appear, built in the frame or off it; run by hand into logs/b-50/.
// `./gradlew :mutant-core:appearBench --args="$(git rev-parse --short HEAD)" > logs/b-50/<date>-desktop.log`
tasks.register<JavaExec>("appearBench") {
    group = "verification"
    description = "Measures the frame in which ten bodies appear; not part of check."
    mainClass = "io.github.youndie.mutant.bench.AppearBenchKt"
    val test =
        kotlin.targets
            .getByName("desktop")
            .compilations
            .getByName("test")
    classpath = test.output.allOutputs + test.runtimeDependencyFiles!!
    dependsOn(test.compileAllTaskName)
    notCompatibleWithConfigurationCache("a measurement, run by hand")
}

// B-71: a test task is never taken from the build cache. Under `allTests` and `check`, Kotlin lets a test task
// with failing tests succeed and fails the aggregate after it, so the failed run is stored like any other; a
// fresh checkout (every CI run) then loads it and the build passes without a single test having run.
tasks.withType<AbstractTestTask>().configureEach {
    outputs.cacheIf("a cached test run replays its failures as a pass (B-71)") { false }
}
