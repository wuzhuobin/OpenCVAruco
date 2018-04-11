package application;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class FirstJFXController {
	
	@FXML
	private Pane vBoxPane;

	@FXML
	private ImageView imageViewBefore;
	
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


	private VideoCapture videoCapture = new VideoCapture();

	private Vector<Mat> allImgs = new Vector<Mat>();

	
	private Mat camMatrix = new Mat();
	
	private Mat distCoeffs = new Mat();
	
	static final Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_1000);

	static final CharucoBoard charucoBoard = CharucoBoard.create(4, 4, 2f, 1f, dictionary);

	@FXML
	public void initialize() {
		this.streaming(true);

		Size outSize = new Size(800, 800);
		Mat img = new Mat();
		FirstJFXController.charucoBoard.draw(outSize, img);
		this.markerView.setImage(mat2Image(img));

	}

	public void streaming(Boolean b) {
		if (b) {
			videoCapture.open(0);
			// grab a frame every 33 ms (30 frames/sec)
			Runnable frameGrabber = new Runnable() {

				@Override
				public void run() {
					// effectively grab and process a single frame
					Mat frame = new Mat();
					videoCapture.read(frame);
					Platform.runLater(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							imageViewBefore.setImage(mat2Image(frame));
						}
					});
					// convert and show the frame

					Mat ids = new Mat(); 
					Vector<Mat> corners = new Vector<Mat>();
//					Vector<Mat> rejected = new Vector<>();
					Mat currentCharucoIds = new Mat();
					Mat currentCharucoCorners = new Mat();
					Aruco.detectMarkers(frame, FirstJFXController.dictionary, corners, ids);
//					Aruco.refineDetectedMarkers(frame, charucoBoard, corners, ids, new Vector<>());

					Aruco.drawDetectedMarkers(frame, corners, ids, new Scalar(0, 255, 0));
					if (!ids.empty()) {
						Aruco.interpolateCornersCharuco(corners, ids, frame, FirstJFXController.charucoBoard,
								currentCharucoCorners, currentCharucoIds);
						Aruco.drawDetectedCornersCharuco(frame, currentCharucoCorners, currentCharucoIds,
								new Scalar(255));

						
						if(!camMatrix.empty() && !distCoeffs.empty()) {
							Mat rvecs = new Mat();
							Mat tvecs = new Mat();

							if(corners.size() > 0) {								
								Aruco.estimatePoseSingleMarkers(corners, 0.066f, camMatrix, distCoeffs, rvecs, tvecs);
							}

							double[] translations = new double[3];
							translations[0] = tvecs.get(0, 0)[0];
							translations[1] = tvecs.get(0, 0)[1];
							translations[2] = tvecs.get(0, 0)[2];
							double[] rotations = new double[3];
							rotations[0] = rvecs.get(0, 0)[0];
							rotations[1] = rvecs.get(0, 0)[1];
							rotations[2] = rvecs.get(0, 0)[2];
							
							Platform.runLater(()->{
								textFieldTranslation0.setText(String.valueOf(translations[0]));
								textFieldTranslation1.setText(String.valueOf(translations[1]));
								textFieldTranslation2.setText(String.valueOf(translations[2]));
								textFieldRotation0.setText(String.valueOf(rotations[0]));
								textFieldRotation1.setText(String.valueOf(rotations[1]));
								textFieldRotation2.setText(String.valueOf(rotations[2]));
							});

							
						}
						

					}
			
					
					Platform.runLater(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							imageViewAfter.setImage(mat2Image(frame));
							
						}
					});
				}
			};

			this.timer = Executors.newSingleThreadScheduledExecutor();
			this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
		} else {

			try {
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
				this.videoCapture.release();
			} catch (InterruptedException e) {
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}

	}

	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;

	// util
	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 *
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	public static Image mat2Image(Mat frame) {
		try {
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		} catch (Exception e) {
			System.err.println("Cannot convert the Mat obejct: " + e);
			return null;
		}
	}

	/**
	 * Support for the {@link mat2image()} method
	 * 
	 * @param original
	 *            the {@link Mat} object in BGR or grayscale
	 * @return the corresponding {@link BufferedImage}
	 */
	private static BufferedImage matToBufferedImage(Mat original) {
		// init
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);

		if (original.channels() > 1) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		} else {
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

		return image;
	}

	@FXML
	public void handleDrawMarker() {
		Mat img = new Mat();
		Aruco.drawMarker(dictionary, 0, 500, img);
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter("PNG file", "*.png"));
		File file = fileChooser.showSaveDialog(this.vBoxPane.getScene().getWindow());
		if(file == null) {
			return;
		}
		Imgcodecs.imwrite(file.getAbsolutePath(), img);
	}

	@FXML
	public void handleChArUcoBoardCapture() {
		this.allImgs.add(new Mat());
		this.videoCapture.read(this.allImgs.lastElement());
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
		File file = fileChooser.showSaveDialog(this.vBoxPane.getScene().getWindow());
		if(file == null) {
			return;
		}
		Mat img = new Mat();
		this.videoCapture.read(img);
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
		File file = fileChooser.showSaveDialog(this.vBoxPane.getScene().getWindow());
		
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

}
