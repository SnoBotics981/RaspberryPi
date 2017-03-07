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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
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

    int teamNumber = 0;
    try {
      System.out.println("Use avahi to detect the configured team address");
      Process findTeamNumber = Runtime.getRuntime().exec(new String[]{
        "bash","-c","avahi-browse -larpt | grep \"=;eth0;IPv4\" | grep \";Workstation;\""
      });
      findTeamNumber.waitFor();
      BufferedReader data = new BufferedReader(
        new InputStreamReader(findTeamNumber.getInputStream()));
      String line;
      while((line = data.readLine()) != null) {
	String hostname = line.split(";")[6];
	String avahiID = hostname.split("-")[1];
	teamNumber = Integer.parseInt(avahiID);
      }
      data.close();
    } catch (IOException | InterruptedException e) {
      System.out.println("Exception while scanning robot network");
      e.printStackTrace();
    }
    System.out.println("Team number detected: " + teamNumber);

    // If the teamNumber code detects the roboRIO, connect using the computed team number
    // If no roboRIO was detected, run the code in server mode
    if (0 == teamNumber) {
      NetworkTable.setServerMode();
    } else {
      NetworkTable.setClientMode();
      NetworkTable.setTeam(982);
    }

    NetworkTable.initialize();
    data = NetworkTable.getTable("navigation");

    // This is the network port you want to stream the raw received image to
    // By rules, this has to be between 1180 and 1190, so 1185 is a good choice
    int streamPort = 1185;

    /******************************************************
     * Configure USB Camera (device links are /dev/video#)
     *
     * Standardizing on 640x480 as the video resolution
     ******************************************************/

    MjpegServer inputStream = new MjpegServer("MJPEG Server", streamPort);
    UsbCamera camera = setUsbCamera(0, inputStream);
    // Set the resolution for our camera, since this is over USB
    camera.setResolution(320,240);
    camera.setFPS(20);

    // If the second USB camera is present, run an isolated video feed for the driver
    MjpegServer rvStream = new MjpegServer("Rear-view Server", 1188);
    UsbCamera rearView = setUsbCamera(1, rvStream);
    rearView.setResolution(320, 240);
    rearView.setFPS(20);

    // This creates a CvSource to use. This will take in a Mat image that has had OpenCV operations
    CvSource imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, 640, 480, 30);
    MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
    cvStream.setSource(imageSource);

    CvSource rawVideoFeed = new CvSource("Unprocessed Video Feed", VideoMode.PixelFormat.kMJPEG, 640, 480, 30);
    MjpegServer rawView   = new MjpegServer("CV Image Stream", 1187);
    rawView.setSource(rawVideoFeed);

    // This creates a CvSink for us to use. This grabs images from our selected camera,
    // and will allow us to use those images in OpenCV.  To toggle processing
    // feeds (below), set the source to use a different device.
    CvSink imageSink = new CvSink("CV Image Grabber");
    imageSink.setSource(camera);

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
      // Allow the robot/dashboard/other to select a camera for vision processing
      String sourceCamera = data.getString("visionCamera", "0");
      // Grab a frame. If it has a frame time of 0, there was an error.
      // If so, skip and continue
      long frameTime = imageSink.grabFrame(inputImage);
      if (frameTime == 0) continue;

      findTargets(inputImage.clone(), imageSource);

      // Display the raw camera feed in a separate filter
      Imgproc.line(inputImage, new Point(100,20), new Point(100,220), new Scalar(0, 255, 0), 7);
      Imgproc.line(inputImage, new Point(220,20), new Point(220,220), new Scalar(0, 255, 0), 7);
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
  private static Scalar[] coords = new Scalar[2];
  private static NetworkTable data;

  private static void initMats() {
    hsv = new Mat();
    targets = new ArrayList<MatOfPoint>();
    targetHierarchy = new Mat();
    center = new Point();
    coords[0] = new Scalar(0, 0, 0);
    coords[1] = new Scalar(0, 0, 0);
  }

  private static void findTargets(Mat frame, CvSource outStream) {
      // Below is where you would do your OpenCV operations on the provided image
      // The sample below just changes color source to HSV
      Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);
      Imgproc.blur(hsv.clone(), hsv, new Size(27,27));

      // Sharpen the image before processing it
      // Disabling this for now because it seems to make the streams unstable
//      Imgproc.GaussianBlur(hsv, sharpen, new Size(0, 0), 3);
//      Core.addWeighted(hsv.clone(), 1.5, sharpen, -0.5, 0, hsv);

      // The light ring is green, but the reflected color is kinda bluish
      Core.inRange(hsv.clone(), new Scalar(50, 8, 200), new Scalar(180, 230, 255), hsv);
      Imgproc.findContours(hsv.clone(), targets, targetHierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

      for(MatOfPoint target : targets) {
        polygon = Imgproc.moments(target);
        center.x = polygon.get_m10() / polygon.get_m00();
        center.y = polygon.get_m01() / polygon.get_m00();
        double size = Imgproc.contourArea(target);
        int debugSize = new Double(Math.sqrt(size)).intValue();
        // The colorFilter mat only accepts black or white coloring
        if (size < 50.0) continue;
        if (size >= coords[0].val[2]) {
          coords[1].set(coords[0].val);
          coords[0].set(new double[]{center.x, center.y, size});
        } else {
          if (size >= coords[1].val[2]) {
            coords[1].set(new double[]{center.x, center.y, size});
          }
        }
      }
      int coordSize = new Double(Math.sqrt(coords[0].val[2])).intValue();
      Imgproc.circle(hsv, new Point(coords[0].val[0], coords[0].val[1]), coordSize, new Scalar(255, 255, 255), 3);
      coordSize = new Double(Math.sqrt(coords[1].val[2])).intValue();
      Imgproc.circle(hsv, new Point(coords[1].val[0], coords[1].val[1]), coordSize, new Scalar(255, 255, 255), 3);

      double offset = ( (coords[0].val[0] + coords[1].val[0]) / 2 ) - 160;
      VisionTarget.setAngle(new Double(offset).intValue());

      double spacing = Math.abs(coords[0].val[0] - coords[1].val[0]);
      double targetArea = coords[0].val[2] + coords[1].val[2];
      double closeness = Math.sqrt(spacing * targetArea);
      VisionTarget.setCloseness(new Double(closeness).intValue());

      data.putNumber("angle", new Double(offset).intValue());
      data.putNumber("closeness", new Double(closeness).intValue());

      // Reseet target detectors after each frame
      coords[0].set(new double[]{0, 0, 0});
      coords[1].set(new double[]{0, 0, 0});
      targets.clear();

      // Stream the filtered/processed data to the first source (for debugging the target detection)
      outStream.putFrame(hsv);

      // OpenCV needs help with memory management
      hsv.release();
      frame.release();
  }
}
