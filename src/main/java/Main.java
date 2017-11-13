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
import org.opencv.videoio.VideoWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

public class Main {
  private static VisionProcessor vision;
  private static NetworkTable data;

  public static void main(String[] args) {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");

    // NetworkManager handles boilerplate code to detect the runtime environment
    NetworkManager scanner = new NetworkManager();

    scanner.scanInterfaces();

    int teamNumber = scanner.getTeamNumber();
    System.out.println("Team number detected: " + teamNumber);

    // If the teamNumber code detects the roboRIO, connect using the computed team number
    // If no roboRIO was detected, run the code in server mode
    if (0 == teamNumber) {
      NetworkTable.setServerMode();
    } else {
      NetworkTable.setClientMode();
      NetworkTable.setTeam(teamNumber);
    }

    NetworkTable.initialize();
    data = NetworkTable.getTable("navigation");

    /******************************************************
     * Configure USB Camera (device links are /dev/video#)
     *
     * Standardizing on 320x240 as the video resolution
     ******************************************************/

    UsbCamera camera = new UsbCamera("Vision Camera", 0);
    // Set the resolution for our camera, since this is over USB
    camera.setResolution(320,240);
    camera.setFPS(15);

    // If the second USB camera is present, run an isolated video feed for the driver
    // Note that this is the "Front" camera per physical layout
    MjpegServer rvStream = new MjpegServer("Rear-view Server", 1188);
    UsbCamera rearView = setUsbCamera(1, rvStream);
    rearView.setResolution(320,240);
    rearView.setFPS(15);

    // This image feed displays the debug log (whatever the filters computed)
    CvSource imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, 320, 240, 15);
    MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
    cvStream.setSource(imageSource);
    VideoWriter logDebugStream = new VideoWriter("debugLog.mpg", VideoWriter.fourcc('M','P','4','V'), 15.0, new Size(320, 240), true);

    CvSource rawVideoFeed = new CvSource("Unprocessed Video Feed", VideoMode.PixelFormat.kMJPEG, 320, 240, 15);
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
    vision = new VisionProcessor();

    // Embed a Jetty server for non-video content
    new HttpManager().runServer();
    System.out.println("Server ready, starting the camera feeds");
    // Debug sample - force the angle to non-zero while testing the servlet
    VisionTarget.setAngle(23);

    // Infinitely process camera feeds
    while (true) {
      // Allow the robot/dashboard/other to select a camera for vision processing
      String sourceCamera = data.getString("visionCamera", "0");
      // Grab a frame. If it has a frame time of 0, there was an error.
      // If so, skip and continue
      long frameTime = imageSink.grabFrame(inputImage);
      if (frameTime == 0) continue;

      vision.findTargets(inputImage.clone(), imageSource);

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

}
