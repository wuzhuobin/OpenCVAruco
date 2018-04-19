package application;
	
import org.opencv.core.Core;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.event.EventHandler;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try
		{
			// load the FXML resource
			FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
			loader.load();
			// create and style a scene
			//scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			// scene
			primaryStage.setTitle("MainWindow");
			primaryStage.setScene(((MainWindowController)loader.getController()).getScene());
			// show the GUI
			primaryStage.show();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// Load the native library.
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		
		launch(args);

	}
	

	
}
