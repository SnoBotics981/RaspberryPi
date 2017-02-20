import java.util.ArrayList;

import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.cscore.*;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

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
    Mat inputImage = new Mat();   // Get frame from camera
//    Mat sharpen = new Mat();      // Sharpen an image using GaussianBlur
    initMats();

    // Embed a Jetty server for non-video content
    new HttpManager().runServer();
    System.out.println("Server ready, starting the camera feeds");
    VisionTarget.setAngle(23);

    // Infinitely process camera feeds
    while (true) {
      // Grab a frame. If it has a frame time of 0, there was an error.
      // Just skip and continue
      long frameTime = imageSink.grabFrame(inputImage);
      if (frameTime == 0) continue;

      findTargets(inputImage.clone(), imageSource);

      // Display the raw camera feed in a separate filter
//      Imgproc.line(inputImage, new Point(200,50), new Point(200,430), new Scalar(0, 255, 0), 10);
//      Imgproc.line(inputImage, new Point(440,50), new Point(440,430), new Scalar(0, 255, 0), 10);
      rawVideoFeed.putFrame(inputImage);
      inputImage.release();
      System.gc();
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

  private static Mat hsv;             // Convert frame to HSV
  private static List<MatOfPoint> targets; // findContours() returns a list of shapes
  private static Mat targetHierarchy; // OpenCV generates a heirchical sorting of targets
  private static Moments polygon;
  private static Point center;

  private static void initMats() {
    hsv = new Mat();
    targets = new ArrayList<MatOfPoint>();
    targetHierarchy = new Mat();
    center = new Point();
  }

  private static void findTargets(Mat frame, CvSource outStream) {
      // Below is where you would do your OpenCV operations on the provided image
      // The sample below just changes color source to HSV
      Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);
      Imgproc.blur(hsv.clone(), hsv, new Size(25,25));

      // Sharpen the image before processing it
      // Disabling this for now because it seems to make the streams unstable
//      Imgproc.GaussianBlur(hsv, sharpen, new Size(0, 0), 3);
//      Core.addWeighted(hsv.clone(), 1.5, sharpen, -0.5, 0, hsv);

      // The light ring is green, but the reflected color is kinda bluish
      Core.inRange(hsv.clone(), new Scalar(50, 128, 200), new Scalar(180, 220, 255), hsv);
      Imgproc.findContours(hsv.clone(), targets, targetHierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

      VisionTarget.setAngle(targets.size());
      for(MatOfPoint target : targets) {
        polygon = Imgproc.moments(target);
        center.x = polygon.get_m10() / polygon.get_m00();
        center.y = polygon.get_m01() / polygon.get_m00();
        int size = new Double(Math.sqrt(Imgproc.contourArea(target))).intValue();
        // The colorFilter mat only accepts black or white coloring
        Imgproc.circle(hsv, center, size, new Scalar(255, 255, 255), 3);
      }
      targets.clear();

      // Stream the filtered/processed data to the first source (for debugging the target detection)
      outStream.putFrame(hsv);

      // OpenCV needs help with memory management
      hsv.release();
      frame.release();
  }
}
