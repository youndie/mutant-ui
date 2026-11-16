// B-49: the frame benchmark on the attached phone. `scripts/device-bench.sh install`, then `run`, which starts the
// activity and pulls its logs. Not the kit and not a sample: a harness, never published.
plugins {
    alias(wip.plugins.androidApplication)
    // No `org.jetbrains.kotlin.android`: since AGP 9 the Android plugin brings Kotlin itself.
    alias(wip.plugins.composeCompiler)
    id("io.github.youndie.sborka.lint")
}

android {
    namespace = "io.github.youndie.mutant.bench.android"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "io.github.youndie.mutant.bench"
        // A harness for one phone, and what it reads off the platform takes 31: a frame's deadline and its GPU time.
        minSdk = 31
        targetSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures { compose = true }

    // B-06's cases, as the vendored generator made them: the instance bench generates the same slots.
    sourceSets.getByName("main").kotlin.directories +=
        "../mutant-core/src/commonTest/kotlin/io/github/youndie/mutant/gen/goldens"

    // Release with R8, because a debuggable Compose app runs paths no user does and any frame time would be meaningless.
    // Signed with the debug key: it is installed by hand onto one phone, never shipped.
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        // The host reads the kit's internals (its clock, its generator) as a friend, as a module's own tests do; a
        // harness, and nothing it builds is published. The path is the jar AGP compiles against.
        val core = project(":mutant-core").layout.buildDirectory
        freeCompilerArgs.add(
            core
                .file(
                    "intermediates/compile_library_classes_jar/androidMain/bundleAndroidMainClassesToCompileJar/classes.jar",
                ).map { "-Xfriend-paths=${it.asFile.absolutePath}" },
        )
    }
}

dependencies {
    implementation(project(":mutant-core"))
    implementation(wip.compose.runtime)
    implementation(wip.compose.foundation)
    implementation(wip.compose.ui)
    implementation(libs.androidx.activity.compose)
}
