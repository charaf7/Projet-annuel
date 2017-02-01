package calibration;
	
import org.opencv.core.Core;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;

/**
 * cette class contient le main du calibration et l'affichage utilisant le JavaFX en implementant un fichier fxml
 * 
 * CameraCalibration lance un stream qui calibre un tableau de jeu utilisant openCV
 * 
 */
public class CameraCalibration extends Application {
	
	@Override
	public void start(Stage primaryStage) {
		try {
			// load du fichier FXML 
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/calibration/CC_FX.fxml"));
			
			BorderPane rootElement = (BorderPane) loader.load();
			rootElement.setStyle("-fx-background-color: whitesmoke;");
			// mettre en place une style scene
			Scene scene = new Scene(rootElement, 800, 600);
			scene.getStylesheets().add(getClass().getResource("/calibration/application.css").toExternalForm());
			
			primaryStage.setTitle("Camera Calibration");
			primaryStage.setScene(scene);
			// initialiser les variables du controlleur
			CC_Controller controller = loader.getController();
			controller.init();
			// affichage
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		launch(args);
	}
}
