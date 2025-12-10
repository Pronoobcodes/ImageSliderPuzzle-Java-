// ImageSlidePuzzleFX.java
// JavaFX sliding-picture puzzle (levels 1..20). Level formula:
// cols = 5 + (level-1)*2, rows = 4 + (level-1)*2
// Grid is capped to MAX_COLS x MAX_ROWS for usability.
// Shuffling is done by performing many random valid moves from solved state
// which guarantees the puzzle remains solvable.

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImageSlidePuzzleFX extends Application {

    // Maximum grid size (to keep things practical)
    private static final int MAX_COLS = 14;
    private static final int MAX_ROWS = 12;

    private Image sourceImage;
    private GridPane gridPane;
    private BorderPane root;
    private ComboBox<Integer> levelBox;
    private Label movesLabel, timeLabel, noteLabel;
    private Button loadBtn, shuffleBtn, resetBtn, prevBtn, nextBtn, autoFit;
    private int cols = 5, rows = 4;
    private int level = 1;

    // tile data
    private Tile[][] tiles; // tiles[row][col]
    private int emptyR, emptyC; // position of empty cell

    // board state helpers
    private int moves = 0;
    private Timeline timer;
    private int secondsElapsed = 0;
    private AtomicBoolean isShuffling = new AtomicBoolean(false);

    // UI sizing
    private double tileSize = 80; // will adjust to window size

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();
        gridPane = new GridPane();
        gridPane.setHgap(1);
        gridPane.setVgap(1);
        gridPane.setStyle("-fx-background-color: #444; -fx-padding: 8;");

        // top controls
        HBox controls = new HBox(8);
        controls.setPadding(new Insets(8));
        loadBtn = new Button("Load Image");
        loadBtn.setOnAction(e -> loadImage(primaryStage));

        controls.getChildren().add(loadBtn);

        controls.getChildren().add(new Label("Level:"));
        levelBox = new ComboBox<>();
        for (int i = 1; i <= 5; i++) levelBox.getItems().add(i);
        levelBox.setValue(1);
        levelBox.setOnAction(e -> {
            level = levelBox.getValue();
            computeColsRows();
            if (sourceImage != null) buildBoard();
        });
        controls.getChildren().add(levelBox);

        prevBtn = new Button("Prev");
        prevBtn.setOnAction(e -> {
            if (levelBox.getValue() > 1) levelBox.setValue(levelBox.getValue() - 1);
        });
        nextBtn = new Button("Next");
        nextBtn.setOnAction(e -> {
            if (levelBox.getValue() < 20) levelBox.setValue(levelBox.getValue() + 1);
        });
        controls.getChildren().addAll(prevBtn, nextBtn);

        shuffleBtn = new Button("Shuffle");
        shuffleBtn.setOnAction(e -> {
            if (sourceImage != null && !isShuffling.get()) shuffleBoard();
        });
        resetBtn = new Button("Reset");
        resetBtn.setOnAction(e -> {
            if (sourceImage != null && !isShuffling.get()) restoreSolved();
        });

        autoFit = new Button("Auto Fit");
        autoFit.setOnAction(e -> {
            if (sourceImage != null) buildBoard();
        });

        controls.getChildren().addAll(shuffleBtn, resetBtn, autoFit);

        // status
        movesLabel = new Label("Moves: 0");
        timeLabel = new Label("Time: 00:00");
        noteLabel = new Label("");
        noteLabel.setTextFill(Color.ORANGE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(8, controls, spacer, movesLabel, new Label("  "), timeLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(4));

        VBox topContainer = new VBox(topRow, noteLabel);
        root.setTop(topContainer);

        // center with scrollpane
        ScrollPane sp = new ScrollPane(gridPane);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        root.setCenter(sp);

        // timer
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            updateTimeLabel();
        }));
        timer.setCycleCount(Timeline.INDEFINITE);

        // compute default
        computeColsRows();

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Sliding Image Puzzle");
        primaryStage.show();

        // responsive: rebuild board when window size changes (if image loaded)
        scene.widthProperty().addListener((obs, oldV, newV) -> {
            if (sourceImage != null) buildBoard();});
        scene.heightProperty().addListener((obs, oldV, newV) -> {
            if (sourceImage != null)
                buildBoard();});
    }

    private void computeColsRows() {
        int computedCols = 5 + (level - 1) * 2;
        int computedRows = 4 + (level - 1) * 2;
        int cappedCols = Math.min(computedCols, MAX_COLS);
        int cappedRows = Math.min(computedRows, MAX_ROWS);

        cols = cappedCols;
        rows = cappedRows;

        if (computedCols != cappedCols || computedRows != cappedRows) {
            noteLabel.setText(String.format("Level %d requested %dx%d but capped to %dx%d for usability.",
                    level, computedCols, computedRows, cols, rows));
        } else {
            noteLabel.setText("");
        }
    }

    private void loadImage(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select an Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File f = chooser.showOpenDialog(stage);
        if (f != null) {
            try {
                Image img = new Image(f.toURI().toString(), false);
                sourceImage = img;
                computeColsRows();
                buildBoard();
            } catch (Exception ex) {
                showAlert("Failed to load image: " + ex.getMessage());
            }
        }
    }

    private void buildBoard() {
        isShuffling.set(true); // block user actions until built
        timer.stop();
        secondsElapsed = 0;
        moves = 0;
        updateMovesLabel();
        updateTimeLabel();

        // compute tileSize to fit grid reasonably into available area
        double availableWidth = Math.max(400, root.getWidth() - 100);
        double availableHeight = Math.max(300, root.getHeight() - 150);
        tileSize = Math.min((availableWidth - (cols - 1)) / cols, (availableHeight - (rows - 1)) / rows);
        tileSize = Math.max(24, tileSize); // minimum tile size

        // scale source image to exact grid size in pixels
        int totalW = (int) Math.round(tileSize * cols);
        int totalH = (int) Math.round(tileSize * rows);

        Image scaled = resampleImage(sourceImage, totalW, totalH);

        // create tiles
        tiles = new Tile[rows][cols];
        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                WritableImage wi = new WritableImage((int) tileSize, (int) tileSize);
                PixelReader reader = scaled.getPixelReader();
                // copy subimage
                for (int y = 0; y < (int) tileSize; y++) {
                    for (int x = 0; x < (int) tileSize; x++) {
                        int sx = c * (int) tileSize + x;
                        int sy = r * (int) tileSize + y;
                        if (sx < scaled.getWidth() && sy < scaled.getHeight()) {
                            wi.getPixelWriter().setColor(x, y, reader.getColor(sx, sy));
                        } else {
                            wi.getPixelWriter().setColor(x, y, Color.BLACK);
                        }
                    }
                }
                ImageView iv = new ImageView(wi);
                iv.setFitWidth(tileSize);
                iv.setFitHeight(tileSize);
                iv.setPreserveRatio(false);
                Tile t = new Tile(r, c, idx, iv);
                tiles[r][c] = t;
                idx++;
            }
        }

        // set last tile as empty
        emptyR = rows - 1;
        emptyC = cols - 1;
        tiles[emptyR][emptyC].setEmpty(true);

        // display solved board
        refreshGrid();

        // start timer only after first move; keep stopped now
        timer.stop();
        secondsElapsed = 0;
        moves = 0;
        updateMovesLabel();

        isShuffling.set(false);
    }

    // resample into target WxH using ImageView snapshot trick for quality
    private Image resampleImage(Image src, int targetW, int targetH) {
        if ((int) src.getWidth() == targetW && (int) src.getHeight() == targetH)
            return src;
        ImageView iv = new ImageView(src);
        iv.setFitWidth(targetW);
        iv.setFitHeight(targetH);
        iv.setPreserveRatio(false);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.BLACK);
        return iv.snapshot(sp, null);
    }

    private void refreshGrid() {
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();
        gridPane.getRowConstraints().clear();

        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints(tileSize);
            gridPane.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints(tileSize);
            gridPane.getRowConstraints().add(rc);
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Tile t = tiles[r][c];
                StackPane cell = new StackPane();
                cell.setPrefSize(tileSize, tileSize);
                cell.setMaxSize(tileSize, tileSize);
                cell.getStyleClass().removeAll();
                if (t.isEmpty()) {
                    // show empty as a blank dark tile
                    Region empty = new Region();
                    empty.setPrefSize(tileSize, tileSize);
                    empty.setStyle(
                            "-fx-background-color: linear-gradient(#222, #444); -fx-border-color: #333; -fx-border-width: 1;");
                    cell.getChildren().add(empty);
                } else {
                    ImageView iv = t.getImageView();
                    iv.setFitWidth(tileSize);
                    iv.setFitHeight(tileSize);
                    cell.getChildren().add(iv);
                }

                final int rr = r, cc = c;
                cell.setOnMouseClicked(ev -> {
                    if (ev.getButton() == MouseButton.PRIMARY && !isShuffling.get()) {
                        handleTileClick(rr, cc);
                    }
                });

                gridPane.add(cell, c, r);
            }
        }
    }

    private void handleTileClick(int r, int c) {
        if (tiles[r][c].isEmpty())
            return;
        if (isAdjacentToEmpty(r, c)) {
            moveTileToEmpty(r, c);
            moves++;
            updateMovesLabel();
            if (moves == 1)
                timer.play(); // start timer on first move
            refreshGrid();
            if (isSolved()) {
                timer.stop();
                showWin();
            }
        }
    }

    private boolean isAdjacentToEmpty(int r, int c) {
        int dr = Math.abs(r - emptyR);
        int dc = Math.abs(c - emptyC);
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1);
    }

    private void moveTileToEmpty(int r, int c) {
        // swap tile at (r,c) with empty
        Tile t = tiles[r][c];
        tiles[emptyR][emptyC] = t;
        tiles[r][c] = new Tile(r, c, -1, null); // temporary placeholder, will be set as empty below
        tiles[r][c].setEmpty(true);
        // update new empty coordinates
        emptyR = r;
        emptyC = c;
    }

    private void shuffleBoard() {
        isShuffling.set(true);
        // perform a number of random valid moves from solved state to guarantee
        // solvable
        int movesToDo = Math.max(200, cols * rows * 10); // more tiles -> more moves
        Random rnd = new Random();
        // ensure we start from solved layout
        buildSolvedTilesIfNeeded();

        for (int i = 0; i < movesToDo; i++) {
            List<int[]> neighbors = neighborsOf(emptyR, emptyC);
            int[] pick = neighbors.get(rnd.nextInt(neighbors.size()));
            // move the neighbor tile into empty
            moveTileToEmpty(pick[0], pick[1]);
        }

        // reset counters
        moves = 0;
        secondsElapsed = 0;
        updateMovesLabel();
        updateTimeLabel();
        // show board
        refreshGrid();

        // small delay before allowing user actions to avoid race
        Timeline t = new Timeline(new KeyFrame(Duration.millis(150), ev -> isShuffling.set(false)));
        t.play();
    }

    // ensures tiles array is in solved state (useful before shuffle)
    private void buildSolvedTilesIfNeeded() {
        // if tiles is null or sourceImage changed, rebuild solved board images
        if (tiles == null) {
            buildBoard();
            return;
        }
        // create solved ordering of tiles with image pieces and empty at last position
        // assemble images again but preserve tile sizes (re-slice existing image)
        double savedTileSize = tileSize;
        Image scaled = resampleImage(sourceImage, (int) (savedTileSize * cols), (int) (savedTileSize * rows));
        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                WritableImage wi = new WritableImage((int) savedTileSize, (int) savedTileSize);
                PixelReader reader = scaled.getPixelReader();
                for (int y = 0; y < (int) savedTileSize; y++) {
                    for (int x = 0; x < (int) savedTileSize; x++) {
                        int sx = c * (int) savedTileSize + x;
                        int sy = r * (int) savedTileSize + y;
                        wi.getPixelWriter().setColor(x, y, reader.getColor(sx, sy));
                    }
                }
                ImageView iv = new ImageView(wi);
                iv.setFitWidth(savedTileSize);
                iv.setFitHeight(savedTileSize);
                Tile t = new Tile(r, c, idx, iv);
                tiles[r][c] = t;
                idx++;
            }
        }
        emptyR = rows - 1;
        emptyC = cols - 1;
        tiles[emptyR][emptyC].setEmpty(true);
    }

    private List<int[]> neighborsOf(int r, int c) {
        List<int[]> n = new ArrayList<>();
        if (r > 0)
            n.add(new int[] { r - 1, c });
        if (r < rows - 1)
            n.add(new int[] { r + 1, c });
        if (c > 0)
            n.add(new int[] { r, c - 1 });
        if (c < cols - 1)
            n.add(new int[] { r, c + 1 });
        return n;
    }

    private void restoreSolved() {
        buildSolvedTilesIfNeeded();
        moves = 0;
        secondsElapsed = 0;
        updateMovesLabel();
        updateTimeLabel();
        refreshGrid();
        timer.stop();
    }

    private boolean isSolved() {
        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Tile t = tiles[r][c];
                if (r == rows - 1 && c == cols - 1) {
                    if (!t.isEmpty())
                        return false;
                } else {
                    if (t.getOriginalIndex() != idx)
                        return false;
                }
                idx++;
            }
        }
        return true;
    }

    private void showWin() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Solved!");
        a.setHeaderText("Congratulations — puzzle solved!");
        a.setContentText(String.format("Moves: %d\nTime: %s", moves, formatTime(secondsElapsed)));
        a.showAndWait();
    }

    private void updateMovesLabel() {
        movesLabel.setText("Moves: " + moves);
    }

    private void updateTimeLabel() {
        int mm = secondsElapsed / 60;
        int ss = secondsElapsed % 60;
        timeLabel.setText(String.format("Time: %02d:%02d", mm, ss));
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.showAndWait();
    }

    // simple helper tile class
    private static class Tile {
        private int row, col;
        private int originalIndex; // solved ordering index
        private ImageView imageView;
        private boolean empty = false;

        Tile(int r, int c, int origIdx, ImageView iv) {
            row = r;
            col = c;
            originalIndex = origIdx;
            imageView = iv;
        }

        void setEmpty(boolean e) {
            empty = e;
            if (e) {
                imageView = null;
                originalIndex = -1;
            }
        }

        boolean isEmpty() {
            return empty;
        }

        ImageView getImageView() {
            return imageView;
        }

        int getOriginalIndex() {
            return originalIndex;
        }
    }

    private String formatTime(int s) {
        int mm = s / 60;
        int ss = s % 60;
        return String.format("%02d:%02d", mm, ss);
    }
}
