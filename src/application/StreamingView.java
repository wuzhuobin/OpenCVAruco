package application;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.net.URL;
import java.util.Locale.FilteringMode;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;


public class StreamingView extends javafx.scene.image.ImageView {

	static {
		System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
		StreamingView.videoCapture = new VideoCapture();
		StreamingView.videoCapture.open(0);
	}
	
	static public BufferedImage mat2BufferedImage(Mat image) {
		
		byte[] src = new byte[image.height()*image.width()*image.channels()];
		image.get(0, 0, src);
		BufferedImage bufferedImage = null;
		if(image.channels() == 1) {
			bufferedImage = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_BYTE_GRAY);
		}
		else if(image.channels() == 3) {
			bufferedImage = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_3BYTE_BGR);
		}
		byte[] dest = ((DataBufferByte)bufferedImage.getRaster().getDataBuffer()).getData();
		System.arraycopy(src, 0, dest, 0, image.height() * image.width() * image.channels());
		
		return bufferedImage;
	}
	
	static public Image mat2Image(Mat image) {
		return SwingFXUtils.toFXImage(mat2BufferedImage(image), null);
	}
	
	public interface MatProcess{
		default Mat filtering(Mat input) {
			return input;
		}
	}
	
	private class FrameGrabber implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Mat image = new Mat();
			StreamingView.videoCapture.read(image);
			final Mat out = StreamingView.this.filter.filtering(image);
			Platform.runLater(()->{
				setImage(mat2Image(out));
			});
		}
	}
	
	public StreamingView() {
		// TODO Auto-generated constructor stub
		super();
		this.filter = new MatProcess() {};
		this.streaming(true);
//		this.setImage(new WritableImage(0, 0));
	}
	
	protected void streaming(boolean b) {
		if(b) {
			this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
			this.scheduledExecutorService.scheduleAtFixedRate(new FrameGrabber(), 0, 33, TimeUnit.MILLISECONDS);
		}
		else {
			this.scheduledExecutorService.shutdown();
			try {
				this.scheduledExecutorService.awaitTermination(34, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}	

	@Deprecated
	private StreamingView(Image image) {}
	
	@Deprecated
	private StreamingView(URL url) {}
	
	public MatProcess getFilter() {
		return filter;
	}

	static public VideoCapture videoCapture;
	private ScheduledExecutorService scheduledExecutorService;
	private MatProcess filter;
	

	
}
