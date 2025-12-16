import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ImageSlidePuzzleMain extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ImageSlidePuzzle.fxml"));
        BorderPane root = loader.load();

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.setTitle("Sliding Image Puzzle");
        stage.getIcons().add(new Image("slider.png"));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
