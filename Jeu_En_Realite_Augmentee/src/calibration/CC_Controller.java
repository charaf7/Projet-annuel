package calibration;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * cette classe contient les methodes du calibration 
 */

public class CC_Controller {
	// FXML buttons
		@FXML
		private Button cameraButton;
		@FXML
		private Button applyButton;
		@FXML
		private Button snapshotButton;
		// the FXML area for showing the current frame (before calibration)
		@FXML
		private ImageView originalFrame;
		// the FXML area for showing the current frame (after calibration)
		@FXML
		private ImageView calibratedFrame;
		// info related to the calibration process
		
		
		// a timer for acquiring the video stream
		private Timer timer;
		// the OpenCV object that performs the video capture
		private VideoCapture capture;
		// a flag to change the button behavior
		private boolean cameraActive;
		// the saved chessboard image
		private Mat savedImage;
		// the calibrated camera frame
		private Image undistoredImage,CamStream;
		// various variables needed for the calibration
		private List<Mat> imagePoints;
		private List<Mat> objectPoints;
		private MatOfPoint3f obj;
		private MatOfPoint2f imageCorners;
		private int boardsNumber;
		private int numCornersHor;
		private int numCornersVer;
		private int successes;
		private Mat intrinsic;
		private Mat distCoeffs;
		private boolean isCalibrated;
		
		//initialisation
		protected void init(){
			
			this.capture = new VideoCapture();
			this.cameraActive = false;
			this.obj = new MatOfPoint3f();
			this.imageCorners = new MatOfPoint2f();
			this.savedImage = new Mat();
			this.undistoredImage = null;
			this.imagePoints = new ArrayList<>();
			this.objectPoints = new ArrayList<>();
			this.intrinsic = new Mat(3, 3, CvType.CV_32FC1);
			this.distCoeffs = new Mat();
			this.successes = 0;
			this.isCalibrated = false;
			this.boardsNumber = 20;
			this.numCornersHor = 9;
			this.numCornersVer =6;
			this.cameraButton.setDisable(false);
		}
		
		//importation des valeurs dans le fichier fxml
		
		//activation du camera
		@FXML
		protected void startCamera(){
			
			if (!this.cameraActive){
				//demarer la video
				this.capture.open(0);
				if (this.capture.isOpened()){
					this.cameraActive = true;
					
					// generer un frame tout les 33 ms
					TimerTask frameGrabber = new TimerTask() {
						@Override
						public void run()
						{
							CamStream=grabFrame();
							// show the original frames
							Platform.runLater(new Runnable() {
								@Override
					            public void run() {
									originalFrame.setImage(CamStream);
									// set fixed width
									originalFrame.setFitWidth(380);
									// preserve image ratio
									originalFrame.setPreserveRatio(true);
									// show the original frames
									calibratedFrame.setImage(undistoredImage);
									// set fixed width
									calibratedFrame.setFitWidth(380);
									// preserve image ratio
									calibratedFrame.setPreserveRatio(true);
					            	}
								});
							
						}
					};
					
					this.timer = new Timer();
					this.timer.schedule(frameGrabber, 0, 33);
					
					// update the button content
					this.cameraButton.setText("Stop Camera");
				}
				else{
					
					// log the error
					System.err.println("Impossible d'ouvrir la connexion avec la camera...");
				}
			}
			else{
				
				// si la camera n'est pas active
				this.cameraActive = false;
				
				this.cameraButton.setText("Start Camera");
				
				if (this.timer != null){
					
					this.timer.cancel();
					this.timer = null;
				}
				
				this.capture.release();
				originalFrame.setImage(null);
				calibratedFrame.setImage(null);
			}
		}
		
		
		private Image grabFrame(){
			
			Image imageToShow = null;
			Mat frame = new Mat();
			
			
			if (this.capture.isOpened()){
				try
				{
					
					this.capture.read(frame);
					if (!frame.empty()){
						
						// show the chessboard pattern
						this.findAndDrawPoints(frame);
						
						if (this.isCalibrated){
							// si c'est calibrer 
							Mat undistored = new Mat();
							Imgproc.undistort(frame, undistored, intrinsic, distCoeffs);
							undistoredImage = mat2Image(undistored);
						}
						
						// convertir le Mat object (OpenCV) à une Image (JavaFX)
						imageToShow = mat2Image(frame);
					}
					
				}
				catch (Exception e)
				{
					// log the (full) error
					System.err.print("ERROR");
					e.printStackTrace();
				}
			}
			
			return imageToShow;
		}
		
		@FXML
		protected void takeSnapshot(){
			
			if (this.successes < this.boardsNumber){
				
				// enregistrer les valeurs 
				this.imagePoints.add(imageCorners);
				this.objectPoints.add(obj);
				this.successes++;
			}
			
			
			if (this.successes == this.boardsNumber){
				this.calibrateCamera();
			}
		}
		
		//trouver les points et les tracer pour le calibrage
		private void findAndDrawPoints(Mat frame){
	
			Mat grayImage = new Mat();
			
			
			if (this.successes < this.boardsNumber){
				
				// convert the frame in gray scale
				Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);
				// the size of the chessboard
				Size boardSize = new Size(this.numCornersHor, this.numCornersVer);
				// look for the inner chessboard corners
				boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners,
						Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);
				// all the required corners have been found...
				if (found){
					
					// optimization
					TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);
					Imgproc.cornerSubPix(grayImage, imageCorners, new Size(11, 11), new Size(-1, -1), term);
					// save the current frame for further elaborations
					grayImage.copyTo(this.savedImage);
					// show the chessboard inner corners on screen
					Calib3d.drawChessboardCorners(frame, boardSize, imageCorners, found);
					
					// enable the option for taking a snapshot
					this.snapshotButton.setDisable(false);
				}
				else{
					
					this.snapshotButton.setDisable(true);
				}
			}
		}
		
		
		private void calibrateCamera(){
			
			List<Mat> rvecs = new ArrayList<>();
			List<Mat> tvecs = new ArrayList<>();
			intrinsic.put(0, 0, 1);
			intrinsic.put(1, 1, 1);
			// calibrate!
			Calib3d.calibrateCamera(objectPoints, imagePoints, savedImage.size(), intrinsic, distCoeffs, rvecs, tvecs);
			this.isCalibrated = true;
			
			
			this.snapshotButton.setDisable(true);
		}
		
		// Convertir le Mat object (OpenCV) à une  Image pour JavaFX
		private Image mat2Image(Mat frame){
			
			MatOfByte buffer = new MatOfByte();
			Highgui.imencode(".png", frame, buffer);
			return new Image(new ByteArrayInputStream(buffer.toArray()));
		}
}
