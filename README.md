## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).


# 🧩 Image Slider Puzzle (JavaFX)

A **JavaFX sliding image puzzle game** where an image is split into tiles and shuffled. The player must slide the pieces back into the correct order to solve the puzzle.

The game supports **5 difficulty levels**, an **original image preview**, move & time tracking, and guarantees **solvable puzzles**.

## ✨ Features

* 📷 Load any image (JPG, PNG, BMP, GIF)
* 🧠 Sliding puzzle logic (always solvable)
* 🎚 Levels 1–20 with increasing grid size
* 📐 Grid auto-capped for real screen sizes
* 👁 Original image preview (right panel)
* 🔀 Shuffle & reset controls
* ⏱ Timer (starts on first move)
* 🔢 Move counter
* 🖥 Responsive resizing


## 🧩 Level System

Grid size increases by level:

```
Columns = 5 + (level - 1) × 2
Rows    = 4 + (level - 1) × 2
```

To keep the game usable on real screens:

* **Max Columns:** 12
* **Max Rows:** 10

If a level exceeds this, the grid is automatically capped.


## 🖼 How the Puzzle Works

* The selected image is resized to fit the grid
* It is sliced into tiles
* The **last tile is empty**
* Tiles can only move if adjacent to the empty space
* Shuffling is done by valid moves → **always solvable**


## ▶ Controls

* **Load Image** – Select an image from your computer
* **Level Selector / Prev / Next** – Change difficulty
* **Shuffle** – Randomize tiles
* **Reset** – Restore solved image
* **Auto Fit** – Recalculate tile size for window


## 🛠 Requirements

* Java **17+** (JavaFX compatible)
* JavaFX SDK (if not bundled with your JDK)
* Desktop OS (Windows / macOS / Linux)


## 🚀 How to Run

### 1️⃣ Compile

```bash
javac ImageSlidePuzzleFX.java
```

### 2️⃣ Run

```bash
java ImageSlidePuzzleFX
```

> ⚠ If JavaFX is not bundled with your JDK, run with:

```bash
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml ImageSlidePuzzleFX
```


## 🗂 Project Structure

```
ImageSlidePuzzleFX.java   # Main JavaFX application
README.md                # Project documentation
```

## 🧪 Gameplay Tips

* Start with **lower levels** to understand movement
* Use the **original image preview** as reference
* Higher levels require more planning than speed


## 🧱 Built With

* Java
* JavaFX
* GridPane, BorderPane, Timeline, ImageView

## 📌 Possible Enhancements

* ⭐ Save best times per level - in progress
* 🖱 Drag-to-slide tiles
* 🔊 Sound effects
* 📱 Touch-screen support
* 🌙 Dark/Light themes - done
* 🧩 Scene Builder (FXML) version - in progress 


## 📜 License

This project is free to use for **learning and personal projects**.


Happy puzzling! 🧠🧩
