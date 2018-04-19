package application;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.io.File;
import java.util.Vector;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.CharucoBoard;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class MainWindowController {

	private class ExitHandler implements EventHandler<WindowEvent>{

		@Override
		public void handle(WindowEvent event) {
			// TODO Auto-generated method stub
			MainWindowController.this.rawCam.streaming(false);
			StreamingView.videoCapture.release();
			System.exit(0);
		}
		
	}
	
	
	@FXML
	public void initialize(){
		this.scene = new ReadOnlyObjectWrapper<Scene>(new Scene(this.vBoxMain));
		this.getScene().windowProperty().addListener(new ChangeListener<Window>() {

			@Override
			public void changed(ObservableValue<? extends Window> observable, Window oldValue, Window newValue) {
				// TODO Auto-generated method stub
				newValue.setOnCloseRequest(new ExitHandler());
				
			}});
		
		
		this.rawCam = new StreamingView();
		this.rawCam.setFitHeight(400);
		this.rawCam.setFitWidth(400);
		this.vBoxView.getChildren().add(0, this.rawCam);
		this.afterCam = new StreamingView();
		this.afterCam.setFitHeight(400);
		this.afterCam.setFitWidth(400);
		this.vBoxView.getChildren().add(2, afterCam);
		Mat marker = new Mat();
		MainWindowController.charucoBoard.draw(new Size(800, 800), marker);
		this.markerView.setImage(StreamingView.mat2Image(marker));
		
	}
	
	@FXML
	public void handleDrawMarker() {
		Mat img = new Mat();
		Aruco.drawMarker(dictionary, 0, 500, img);
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter("PNG file", "*.png"));
		File file = fileChooser.showSaveDialog(this.vBoxMain.getScene().getWindow());
		if(file == null) {
			return;
		}
		Imgcodecs.imwrite(file.getAbsolutePath(), img);
	}

	@FXML
	public void handleChArUcoBoardCapture() {
		this.allImgs.add(new Mat());
		StreamingView.videoCapture.read(this.allImgs.lastElement());
		this.textFieldNumOfImages.setText(String.valueOf(this.allImgs.size()));
	}
	
	@FXML
	public void handleChArUcoBoardCaptureClear() {
		this.allImgs.clear();
		this.textFieldNumOfImages.setText("0");
	}

	@FXML
	public void handleCalibrationWithChArUcoBoards() {
		int nFrams = allImgs.size();
		if (nFrams < 1) {
			return;
		}

		// copy from examples.
		// prepare data for calibration
		Vector<Vector<Mat>> allCorners = new Vector<>();

		Vector<Mat> allIds = new Vector<>();
		Size imageSize = this.allImgs.get(0).size();
		Vector<Mat> allCornersConcatenated = new Vector<>();
		Mat allIdsConcatenated = new Mat();
		Mat markerCounterPerFrame = new Mat();
		for (int i = 0; i < nFrams; ++i) {

			allCorners.add(new Vector<Mat>());
			allIds.add(new Mat());
			Aruco.detectMarkers(this.allImgs.get(i), dictionary, allCorners.get(i), allIds.get(i));
			Aruco.refineDetectedMarkers(this.allImgs.get(i), charucoBoard, allCorners.get(i), allIds.get(i), new Vector<Mat>());
//			Aruco.interpolateCornersCharuco(allCorners.get(i), allIds.get(i), this.allImgs.get(i), charucoBoard, charucoCorners, charucoIds)
			markerCounterPerFrame.push_back(new MatOfInt(allCorners.get(i).size()));
			allCornersConcatenated.addAll(allCorners.get(i));
			allIdsConcatenated.push_back(allIds.get(i));
		}
		Mat _camMatrix = new Mat();
		Mat _distCoeffs = new Mat();
		double error = Aruco.calibrateCameraAruco(allCornersConcatenated, allIdsConcatenated, markerCounterPerFrame,
				charucoBoard, imageSize, _camMatrix, _distCoeffs);
		this.textFieldAruco.setText(String.valueOf(error));

		Vector<Mat> allCharucoCorners = new Vector<>(nFrams);
		Vector<Mat> allCharucoIds = new Vector<>(nFrams);
//		Vector<Mat> filteredImages = new Vector<>(nFrams);
		
		for(int i = 0; i < nFrams; ++i) {
			allCharucoCorners.add(new Mat());
			allCharucoIds.add(new Mat());
//			filteredImages.add(new Mat());
			
			Aruco.interpolateCornersCharuco(
					allCorners.get(i), 
					allIds.get(i), 
					allImgs.get(i), 
					charucoBoard, 
					allCharucoCorners.get(i), 
					allCharucoIds.get(i), 
					_camMatrix, _distCoeffs, 2);
			
		}
		
		double repError = Aruco.calibrateCameraCharuco(allCharucoCorners, allCharucoIds, charucoBoard, imageSize, camMatrix, distCoeffs);
		this.textFieldCharuco.setText(String.valueOf(repError));
		_camMatrix.copyTo(this.camMatrix);
		_distCoeffs.copyTo(this.distCoeffs);
	}

	@FXML
	public void handleCapture() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save an image.");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("PNG file", "*.png"));
		File file = fileChooser.showSaveDialog(this.vBoxMain.getScene().getWindow());
		if(file == null) {
			return;
		}
		Mat img = new Mat();
		StreamingView.videoCapture.read(img);
		Imgcodecs.imwrite(file.getAbsolutePath(), img);

	}

	@FXML
	public void handleRead() {

	}

	@FXML
	public void handleSave() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save intrinsic parameters and distortion parameters. ");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("XML file", "*.xml"));
		File file = fileChooser.showSaveDialog(this.vBoxMain.getScene().getWindow());
		
		if(file == null) {
			return;
		}
	
		
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

			Document document = documentBuilder.newDocument();
			Element rootElement = document.createElement("CameraParameters");
			document.appendChild(rootElement);
			
			
			Element intrinsic = document.createElement("IntrinsicParameters");
			rootElement.appendChild(intrinsic);
			int row;
			int col;
			row = this.camMatrix.rows();
			col = this.camMatrix.cols();
			for(int i = 0; i < row; ++i) {
				for(int j = 0; j < col; ++j) {
					String value = String.valueOf(this.camMatrix.get(i, j)[0]);
					String name = String.valueOf("E" + String.valueOf(i) + "_" + String.valueOf(j));

					intrinsic.setAttribute(name, value);
				}
			}
			
			Element distortion = document.createElement("DistortionParameters");
			rootElement.appendChild(distortion);
			row = this.distCoeffs.rows();
			col = this.distCoeffs.cols();
			for(int i = 0; i < row; ++i) {
				for(int j = 0; j < col; ++j) {
					String value = String.valueOf(this.distCoeffs.get(i, j)[0]);
					String name = String.valueOf("E" + String.valueOf(i) + "_" + String.valueOf(j));

					distortion.setAttribute(name, value);
				}
			}
			
	         // write the content into xml file
	         TransformerFactory transformerFactory = TransformerFactory.newInstance();
	         Transformer transformer = null;
			try {
				transformer = transformerFactory.newTransformer();
			} catch (TransformerConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	         DOMSource source = new DOMSource(document);
	         StreamResult result = new StreamResult(file);
	         try {
				transformer.transform(source, result);
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	}

	public final Scene getScene() {
		return scene.getReadOnlyProperty().getValue();
	}
	
	public final ReadOnlyObjectProperty<Scene> getReadOnlyScene(){
		return this.scene.getReadOnlyProperty();
	}

	static final private Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_1000);
	static final private CharucoBoard charucoBoard = CharucoBoard.create(4, 4, 2f, 1f, dictionary);	
	@FXML
	private VBox vBoxMain;
	@FXML 
	private VBox vBoxView;
	@FXML
	private ImageView imageViewAfter;
	@FXML
	private ImageView markerView;
	@FXML
	private TextField textFieldNumOfImages;
	@FXML
	private TextField textFieldTranslation0;
	@FXML
	private TextField textFieldTranslation1;
	@FXML
	private TextField textFieldTranslation2;
	@FXML
	private TextField textFieldRotation0;
	@FXML
	private TextField textFieldRotation1;
	@FXML
	private TextField textFieldRotation2;
	@FXML
	private TextField textFieldAruco;
	@FXML
	private TextField textFieldCharuco;
	private ReadOnlyObjectWrapper<Scene> scene;
	private StreamingView rawCam;
	private StreamingView afterCam;
	private Vector<Mat> allImgs = new Vector<Mat>();
	private Mat camMatrix = new Mat();
	private Mat distCoeffs = new Mat();

	
}
