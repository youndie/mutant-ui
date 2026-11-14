# mutant-ui

A Compose Multiplatform UI kit in the look of a damaged copy: grime, glitch and VHS. Every component
is a generated slime body with drips, compression mosaic and flow lines under clean text, icons and
touch targets, with tape noise over the screen. The same instance is damaged the same way on every
launch and platform. No Material dependency.

**[Try it in the browser →](https://youndie.github.io/mutant-ui/)** Every component on one page,
both palettes, reduce motion and the clean signal, and the demo screens beside it.

The bodies come from the design's own generator, ported to common Kotlin and held to it number for
number; the components are held in pixels to references rendered from the design's pages in Chrome.
What the design does not draw is designed here, and says so in its backlog item.

## Using it

```kotlin
repositories {
    maven("https://reposilite.kotlin.website/snapshots") {
        mavenContent { includeGroupAndSubgroups("io.github.youndie") }
    }
}

dependencies {
    implementation("io.github.youndie:mutant-core:0.1.0.<build>")
}
```

Every build of `main` publishes a version: `<head>.<build>`, where the head is the next release. The
latest is the last `<version>` in
[maven-metadata.xml](https://reposilite.kotlin.website/snapshots/io/github/youndie/mutant-core/maven-metadata.xml).
There is no release yet.

```kotlin
MutantTheme {
    MutantButton("Play", onClick = {})
}
```

Targets: desktop (JVM), Android, iOS (arm64 and the arm64 simulator), and wasm.

The sample app, every component and the demo in one window: `./gradlew :sample:run`.

## Where things are

The design is frozen in [reference/design-system/](reference/design-system/), the plan is
[backlog.md](backlog.md), and the documentation is [docs/](docs/README.md). Why it is built the way
it is: [the research document](docs/research/research-architecture.md).

## Licence

MIT. The bundled fonts, Handjet and Rubik, keep their OFL.
