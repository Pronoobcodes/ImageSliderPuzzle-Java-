import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import java.net.URL;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImageSlidePuzzleController {
    // FXML elements
    @FXML
    private GridPane gridPane;
    @FXML
    private ImageView originalPreview;
    @FXML
    private ComboBox<Integer> levelBox;
    @FXML
    private Label movesLabel, timeLabel, noteLabel;
    @FXML
    private Button loadBtn, shuffleBtn, resetBtn, prevBtn, nextBtn;
    @FXML
    private ToggleButton themeToggle;

    // Game state
    private Image sourceImage;
    private int cols = 5, rows = 4;
    private int level = 1;
    private ImageView[][] tiles;
    private int emptyRow, emptyCol;
    private Timeline timer;
    private int secondsElapsed = 0;
    private AtomicBoolean isShuffling = new AtomicBoolean(false);
    private double tileSize = 80;
    private Scene scene;
    int moves;

    public void initialize(Scene scene) {
        this.scene = scene;
        setupLevelBox();
        setupListeners();
        setupTheme();
    }

    private void setupLevelBox() {
        for (int i = 1; i <= 5; i++)
            levelBox.getItems().add(i);
        levelBox.setValue(1);
        levelBox.setOnAction(e -> updateLevel());
    }

    private void setupListeners() {
        scene.widthProperty().addListener((obs, oldV, newV) -> rebuildBoard());
        scene.heightProperty().addListener((obs, oldV, newV) -> rebuildBoard());

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            timeLabel.setText(formatTime(secondsElapsed));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);

        loadBtn.setOnAction(e -> handleLoadImage());
        shuffleBtn.setOnAction(e -> shuffleBoard());
        resetBtn.setOnAction(e -> resetBoard());
    }

    private void setupTheme() {
        // Load default light theme from classpath if available
        try {
            URL light = getClass().getResource("/light.css");
            URL dark = getClass().getResource("/dark.css");
            if (light != null) {
                scene.getStylesheets().add(light.toExternalForm());
            }

            if (themeToggle != null) {
                themeToggle.setOnAction(e -> {
                    boolean darkMode = themeToggle.isSelected();
                    applyTheme(darkMode, light, dark);
                });
            }
        } catch (Exception ex) {
            // ignore theme loading errors and continue
            System.err.println("Theme load error: " + ex.getMessage());
        }
    }

    private void applyTheme(boolean darkMode, URL light, URL dark) {
        scene.getStylesheets().clear();
        if (darkMode && dark != null) {
            scene.getStylesheets().add(dark.toExternalForm());
            if (themeToggle != null) themeToggle.setText("Light Mode");
        } else if (light != null) {
            scene.getStylesheets().add(light.toExternalForm());
            if (themeToggle != null) themeToggle.setText("Dark Mode");
        }
    }

    private void rebuildBoard() {
        if (sourceImage != null) {
            buildBoard();
        }
    }

    private void updateLevel() {
        level = levelBox.getValue() != null ? levelBox.getValue() : 1;
        if (sourceImage != null) {
            calculateGridSize();
            buildBoard();
        }
    }

    private void resetBoard() {
        if (sourceImage != null) {
            resetGameState();
            buildBoard();
            timer.stop();
        }
    }

    @FXML
    private void handleLoadImage() {
        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(scene.getWindow());
        if (file != null)
            loadImage(file);
    }

    private void loadImage(File file) {
        try {
            sourceImage = new Image(file.toURI().toString());
            originalPreview.setImage(sourceImage);
            calculateGridSize();
            buildBoard();
        } catch (Exception e) {
            showAlert("Error loading image: " + e.getMessage());
        }
    }

    private void calculateGridSize() {
        cols = Math.min(5 + (level - 1) * 2, 14);
        rows = Math.min(4 + (level - 1) * 2, 12);
    }

    private void buildBoard() {
        isShuffling.set(true);
        timer.stop();
        resetGameState();

        double availableWidth = scene.getWidth() - 300;
        double availableHeight = scene.getHeight() - 150;
        tileSize = Math.min(availableWidth / cols, availableHeight / rows);

        Image scaled = scaleImage(sourceImage, cols * (int) tileSize, rows * (int) tileSize);
        createTiles(scaled);
        setupEmptyTile();
        refreshGrid();
        isShuffling.set(false);
    }

    private Image scaleImage(Image source, int width, int height) {
        ImageView iv = new ImageView(source);
        iv.setFitWidth(width);
        iv.setFitHeight(height);
        return iv.snapshot(null, null);
    }

    private void createTiles(Image scaled) {
        tiles = new ImageView[rows][cols];
        PixelReader reader = scaled.getPixelReader();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                WritableImage tileImage = new WritableImage((int) tileSize, (int) tileSize);
                tileImage.getPixelWriter().setPixels(0, 0,
                        (int) tileSize, (int) tileSize,
                        reader,
                        c * (int) tileSize,
                        r * (int) tileSize);

                ImageView tile = new ImageView(tileImage);
                tile.setUserData(new Point2D(r, c));
                tile.setOnMouseClicked(this::handleTileClick);
                tiles[r][c] = tile;
            }
        }
    }

    private void setupEmptyTile() {
        emptyRow = rows - 1;
        emptyCol = cols - 1;
        tiles[emptyRow][emptyCol] = null;
    }

    private void handleTileClick(MouseEvent event) {
        if (isShuffling.get())
            return;

        ImageView tile = (ImageView) event.getSource();
        Integer row = GridPane.getRowIndex(tile);
        Integer col = GridPane.getColumnIndex(tile);

        if (isValidMove(row, col)) {
            moveTile(row, col);
            movesLabel.setText("Moves: " + moves);
            if (moves == 1)
                timer.play();
            if (isSolved())
                handleWin();
        }
    }

    private boolean isValidMove(int row, int col) {
        return (Math.abs(row - emptyRow) + Math.abs(col - emptyCol)) == 1;
    }

    private void moveTile(int row, int col) {
        tiles[emptyRow][emptyCol] = tiles[row][col];
        tiles[row][col] = null;
        emptyRow = row;
        emptyCol = col;
    }

    private boolean isSolved() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (tiles[r][c] == null) {
                    if (!(r == rows - 1 && c == cols - 1))
                        return false;
                } else {
                    Point2D pos = (Point2D) tiles[r][c].getUserData();
                    if (pos.getX() != r || pos.getY() != c)
                        return false;
                }
            }
        }
        return true;
    }

    private void refreshGrid() {
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();
        gridPane.getRowConstraints().clear();

        for (int i = 0; i < cols; i++) {
            gridPane.getColumnConstraints().add(new ColumnConstraints(tileSize));
        }
        for (int i = 0; i < rows; i++) {
            gridPane.getRowConstraints().add(new RowConstraints(tileSize));
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (tiles[r][c] != null) {
                    gridPane.add(tiles[r][c], c, r);
                }
            }
        }
    }

    private void handleWin() {
        timer.stop();
        new Alert(Alert.AlertType.INFORMATION,
                "Congratulations! Solved in " + moves + " moves\nTime: " + formatTime(secondsElapsed))
                .showAndWait();
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private void showAlert(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    @FXML
    private void shuffleBoard() {
        isShuffling.set(true);
        buildBoard();
        Random random = new Random();

        for (int i = 0; i < 1000; i++) {
            List<int[]> moves = getPossibleMoves();
            int[] move = moves.get(random.nextInt(moves.size()));
            moveTile(move[0], move[1]);
        }

        resetGameState();
        refreshGrid();
        isShuffling.set(false);
    }

    private List<int[]> getPossibleMoves() {
        List<int[]> moves = new ArrayList<>();
        if (emptyRow > 0)
            moves.add(new int[] { emptyRow - 1, emptyCol });
        if (emptyRow < rows - 1)
            moves.add(new int[] { emptyRow + 1, emptyCol });
        if (emptyCol > 0)
            moves.add(new int[] { emptyRow, emptyCol - 1 });
        if (emptyCol < cols - 1)
            moves.add(new int[] { emptyRow, emptyCol + 1 });
        return moves;
    }

    private void resetGameState() {
        moves = 0;
        secondsElapsed = 0;
        movesLabel.setText("Moves: 0");
        timeLabel.setText("Time: 00:00");
    }
}
