import java.util.ArrayList;

import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.cscore.*;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.eclipse.jetty.server.Server;

public class Main {
  public static void main(String[] args) {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");

    // Testing network autoconfigure options
    Enumeration<NetworkInterface> interfaces = null;
    try {
        interfaces = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
        System.out.println("SocketException error when attempting to load network devices");
	e.printStackTrace();
	return;
    }

    if (interfaces != null) {
        while ( interfaces.hasMoreElements() ) {
            NetworkInterface i = interfaces.nextElement();
            Enumeration<InetAddress> addresses = i.getInetAddresses();
            while ( addresses.hasMoreElements() ) {
                InetAddress address = addresses.nextElement();
                if (!address.isLoopbackAddress() && address.isSiteLocalAddress()) {
                    System.out.println("Network address: " + address.getHostAddress());
                }
            }
        }
    }

/* Use this code block when operating as a client; the roboRIO should run server code
    // Connect NetworkTables, and get access to the publishing table
    NetworkTable.setClientMode();
    // Set your team number here
    NetworkTable.setTeam(982);
*/
    NetworkTable.setServerMode();

    NetworkTable.initialize();


    // This is the network port you want to stream the raw received image to
    // By rules, this has to be between 1180 and 1190, so 1185 is a good choice
    int streamPort = 1185;

    // This stores our reference to our mjpeg server for streaming the input image
    MjpegServer inputStream = new MjpegServer("MJPEG Server", streamPort);

    // Selecting a Camera

    /**************************************************
     * Configure USB Camera (hardwired to /dev/video0)
     **************************************************/

    UsbCamera camera = setUsbCamera(0, inputStream);
    // Set the resolution for our camera, since this is over USB
    camera.setResolution(640,480);

    // This creates a CvSink for us to use. This grabs images from our selected camera, 
    // and will allow us to use those images in opencv
    CvSink imageSink = new CvSink("CV Image Grabber");
    imageSink.setSource(camera);

    // This creates a CvSource to use. This will take in a Mat image that has had OpenCV operations
    CvSource imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, 640, 480, 30);
    MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
    cvStream.setSource(imageSource);

    CvSource rawVideoFeed = new CvSource("Unprocessed Video Feed", VideoMode.PixelFormat.kMJPEG, 640, 480, 30);
    MjpegServer rawView   = new MjpegServer("CV Image Stream", 1187);
    rawView.setSource(rawVideoFeed);

    // All Mats and Lists should be stored outside the loop to avoid allocations
    // as they are expensive to create
    Mat inputImage = new Mat(); // Get frame from camera
    Mat hsv = new Mat();	// Convert frame to HSV
    Mat colorFilter = new Mat();

    // Embed a Jetty server for non-video content
    Server manager = new Server(1181);
    VisionTarget targetCoords = new VisionTarget();
    manager.setHandler(targetCoords);
    Thread server = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          manager.start();
          manager.join();
        } catch (Exception e) {
          System.out.println("Unable to start the Vision Target service: " + e.toString() );
        }
      }
    });
    server.start();
    System.out.println("Server ready, starting the camera feeds");
    targetCoords.setAngle(23);

    // Infinitely process camera feeds
    while (true) {
      // Grab a frame. If it has a frame time of 0, there was an error.
      // Just skip and continue
      long frameTime = imageSink.grabFrame(inputImage);
      if (frameTime == 0) continue;

      // Below is where you would do your OpenCV operations on the provided image
      // The sample below just changes color source to HSV
      Imgproc.cvtColor(inputImage, hsv, Imgproc.COLOR_BGR2HSV);
      Core.inRange(hsv, new Scalar(20, 100, 100), new Scalar(30, 255, 255), colorFilter);

      // Stream the filtered/processed data to the first source (for debugging the target detection)
      imageSource.putFrame(colorFilter);
      // Display the raw camera feed in a separate filter
      rawVideoFeed.putFrame(inputImage);
    }
  }

  private static UsbCamera setUsbCamera(int cameraId, MjpegServer server) {
    // This gets the image from a USB camera 
    // Usually this will be on device 0, but there are other overloads
    // that can be used
    UsbCamera camera = new UsbCamera("CoprocessorCamera", cameraId);
    server.setSource(camera);
    return camera;
  }
}
