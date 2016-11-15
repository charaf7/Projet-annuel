package test;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

public class Hello {
	public static void main(String[] args) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		VideoCapture test;
		test = new VideoCapture(0);
		System.out.println("VidéoCapture créée");
		if (!test.isOpened()) {
			System.out.println("Caméra non ouverte");
			System.out.println("Tentative ouverture");
			test.open(0);
			
		}

		//for (;;) {
			Mat frame = new Mat();
			test.retrieve(frame); // get a new frame from camera

			Highgui.imwrite("me1.jpeg", frame);

			test.release();
		//}
	}
}