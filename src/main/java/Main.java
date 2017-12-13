import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.cscore.*;
import net.engio.mbassy.listener.Handler;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

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
    Config.initialize();
    int frameRate = Config.VIDEO_RATE.intValue();
    System.out.println("Framerate from config: " + frameRate);
    data = NetworkTable.getTable("navigation");

    /******************************************************
     * Configure USB Camera (device links are /dev/video#)
     *
     * Standardizing on 320x240 as the video resolution
     ******************************************************/

    UsbCameraManager camera = new UsbCameraManager("Vision Camera", 0);

    // If the second USB camera is present, run an isolated video feed for the driver
    // Note that this is the "Front" camera per physical layout
    MjpegServer rvStream = new MjpegServer("Rear-view Server", 1188);
    UsbCamera rearView = setUsbCamera(1, rvStream);
    rearView.setResolution(320,240);
    rearView.setFPS(frameRate);

    // This image feed displays the debug log (whatever the filters computed)
    CvSource imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, 320, 240, 15);
    MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
    cvStream.setSource(imageSource);
    VideoWriter logDebugStream = new VideoWriter(
        "debugLog.mjpeg", VideoWriter.fourcc('M','J','P','G'), 15.0, new Size(320, 240), true);

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

      vision.findTargets(inputImage, imageSource);

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

  public static class UsbCameraManager extends UsbCamera {
    public UsbCameraManager(String label, int device) {
      super(label, device);
      this.setResolution(
          Config.VIDEO_WIDTH.intValue(),
          Config.VIDEO_HEIGHT.intValue());
      this.setFPS(Config.VIDEO_RATE.intValue());
      System.out.println("Subscribe to the configuration bus");
      Config.bus.subscribe(this);
    }

    // Generic handler, will process any config event
    @Handler
    public void onChange(Config option) {
      System.out.println("Event handler process change event: " + option.id + "=>" + option.getValue());
    }

    // Limited handler, will only respond when Config.VIDEO_RATE is modified
    @Handler(condition = "msg == 'VIDEO_RATE'")
    public void onVideoRate(Config msg) {
      System.out.println("Video Rate event detected: '" + msg.getValue() + "'");
      this.setFPS(msg.intValue());
    }
  }
}
