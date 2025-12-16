# ImageSlidePuzzle (JavaFX)

A small JavaFX sliding image puzzle game. The app splits an image into tiles, shuffles them, and the player slides tiles to restore the original image.

This repository contains source code (Java + FXML), stylesheets, helper scripts, and a CI workflow to build and package the app for Windows.

## Key features

- Load images (JPG / PNG / BMP / GIF)
- 5 difficulty levels (increasing grid size)
- Move counter and timer
- Original image preview
- Light / Dark themes (toggle at runtime)

## Requirements

- Java 21+ (recommended)
- JavaFX SDK (only required if your JDK does not include JavaFX)

## Quick run (from source)

1. Clone the repository and open a terminal in the project root.
2. Compile and run on Windows (example using local JDK paths):

```powershell
# compile (creates bin/)
"C:\Program Files\Java\jdk-21\bin\javac.exe" --module-path javafx\lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -d bin src\ImageSlidePuzzleMain.java src\ImageSlidePuzzleController.java

# run
"C:\Program Files\Java\jdk-21\bin\java.exe" --module-path javafx\lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp bin ImageSlidePuzzleMain
```

If you use a different JDK location, adjust the paths above. If your JDK already bundles JavaFX, you can omit `--module-path` / `--add-modules`.

## Run from JAR

After packaging the JAR (see Build below), run:

```powershell
java --module-path javafx/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar ImageSlidePuzzle.jar
```

## Build systems included

- `pom.xml` — Maven project configuration (uses `src/` for sources and resources)
- `build.gradle` — simple Gradle build (alternative)

## Continuous Integration (GitHub Actions)

There is a workflow at `.github/workflows/release.yml` that builds the project with Maven and runs `jpackage` on a Windows runner to create an EXE when a tag (`v*`) is pushed or when manually triggered. The produced installer/exe is uploaded as an artifact.

Usage on GitHub:

1. Commit and push the repo to GitHub.
2. Push a tag (for example `git tag v1.0.0 && git push --tags`) or run the workflow manually in Actions.
3. Download the `release` artifact from the workflow run.

Notes:

- The workflow downloads the JavaFX SDK for Windows at runtime — you do not need to check JavaFX into the repository.
- `jpackage` requires a matching JDK on the runner; the workflow uses Temurin/Adoptium on `windows-latest`.

## Packaging options

- `jpackage` — produces native installers/executables (used by CI)
- Launch4j — wrap the JAR into a Windows EXE (useful for GUI-only wrappers)
- `gradle jlink` / `badass-jlink` — produce self-contained runtime images

## Project layout (important files)

```
src/                       # Java sources, FXML and CSS resources
	ImageSlidePuzzleMain.java
	ImageSlidePuzzleController.java
	ImageSlidePuzzle.fxml
	light.css
	dark.css
bin/                       # compiled classes (created by build scripts)
ImageSlidePuzzle.jar        # packaged JAR (if built)
.github/workflows/release.yml
pom.xml
build.gradle
README.md
```

## Build locally with Maven

If you have Maven installed:

```powershell
mvn -B -DskipTests package
```

The Maven build uses `src/` as sources and will create the JAR under `target/`.

## Build locally with Gradle

If you prefer Gradle:

```powershell
gradle clean jar
```

## Notes about themes and resources

`light.css` and `dark.css` are loaded from the classpath at runtime, so they work both when running from the source tree and when running from the packaged JAR, provided resources are included in the build.

## Contributing

PRs and issues welcome. For changes that affect the build or packaging, please include CI-friendly updates (Maven/Gradle + workflow adjustments).

## License

Free to use for learning and personal projects.

Happy puzzling! 🧠🧩