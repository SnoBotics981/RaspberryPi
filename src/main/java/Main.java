import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.cscore.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import net.engio.mbassy.listener.Handler;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

public class Main {
  private static VisionProcessor vision;
  public static final NetworkTableInstance netTable = NetworkTableInstance.getDefault();
  private static NetworkTable data;
  private static CameraServer manager = CameraServer.getInstance();

  public static void main(String[] args) {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");
    System.loadLibrary("ntcore");

    // NetworkManager handles boilerplate code to detect the runtime environment
    NetworkManager scanner = new NetworkManager();

    scanner.scanInterfaces();

    int teamNumber = scanner.getTeamNumber();
    System.out.println("Team number detected: " + teamNumber);

    // If the teamNumber code detects the roboRIO, connect using the computed team number
    // If no roboRIO was detected, run the code in server mode
    if (0 == teamNumber) {
      netTable.startServer();
    } else {
      netTable.startClientTeam(teamNumber);
    }
    netTable.addLogger(log -> {
      System.out.println("netTable(" + log.level + "): " + log.message);
      netTable.flush();
    }, 0, 9);

    Config.initialize();
    int frameRate = Config.VIDEO_RATE.intValue();
    System.out.println("Framerate from config: " + frameRate);
    data = netTable.getTable("navigation");

    /******************************************************
     * Configure USB Camera (device links are /dev/video#)
     *
     * Standardizing on 320x240 as the video resolution
     ******************************************************/

    UsbCameraInfo[] stats = UsbCamera.enumerateUsbCameras();
    for (int i=0; i < stats.length; i++) {
      System.out.println("Camera #" + stats[i].dev + ": " + stats[i].name +
          " => " + stats[i].path);
    }

    // Consider storing the cameras in an array
    UsbCameraManager camera = new UsbCameraManager("Vision Camera", 0);
    UsbCameraManager rearView = new UsbCameraManager("Rear-view", 1);

    // If the second USB camera is present, run an isolated video feed for the driver
    // Note that this is the "Front" camera per physical layout
    MjpegServer rvStream = manager.addServer("Rear-view Server", 1188);
    rvStream.setSource(rearView);

    // This image feed displays the debug log (whatever the filters computed)
    VideoStream debugStream = new VideoStream("CV Image Source", "Debug Stream", 1186);
    VideoStream rawView = new VideoStream("Unprocessed Video Feed", "CV Image Stream", 1187);

    VideoWriter logDebugStream = new VideoWriter(
        "debugLog.mjpeg", VideoWriter.fourcc('M','J','P','G'), 15.0, new Size(320, 240), true);

    debugStream.setToggleFlag(Config.CAMERA_DEBUG.id);

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

    // Prepare the image capture for video debugging
    initVideoLog();

    System.out.println("Server ready, starting the camera feeds");
    // Debug sample - force the angle to non-zero while testing the servlet
    VisionTarget.setAngle(23);

    int frameCounter = 0;
    // Infinitely process camera feeds
    while (true) {
      // Allow the robot/dashboard/other to select a camera for vision processing
      String sourceCamera = data.getEntry("visionCamera").getString("0");
      // Grab a frame. If it has a frame time of 0, there was an error.
      // If so, skip and continue
      long frameTime = imageSink.grabFrame(inputImage);
      if (frameTime == 0) continue;

      vision.findTargets(inputImage,debugStream.source());

      // Capture approximately one frame per second to the frame log
      if (frameCounter++ == Config.VIDEO_RATE.intValue()) {
      }

      // Display the raw camera feed in a separate filter
      Imgproc.line(inputImage, new Point(100,20), new Point(100,220), new Scalar(0, 255, 0), 5);
      Imgproc.line(inputImage, new Point(220,20), new Point(220,220), new Scalar(0, 255, 0), 5);
      rawView.source().putFrame(inputImage);
//      rawVideoFeed.putFrame(inputImage);
      inputImage.release();
      System.gc();
    }
  }

  // By default, use a numbering system for each test run.  Config option
  // 'debug.matchname' will be used as a folder name, so if the name is changed
  // at runtime the logging code should switch to the new folder immediately
  private static void initVideoLog() {
    File[] logHistory = new File("../video-logs").listFiles();
    int logNumber = 0;

    for(File record: logHistory) {
      if (record.isDirectory()) { ++logNumber; }
    }
    while (Files.exists(Paths.get(Integer.toString(logNumber)))) {
      ++logNumber;
    }
    Config.DEBUG_MATCHNAME.update(logNumber);
    try {
      System.out.println("Attempting to create folder numbered '" + logNumber + "'");
      Files.createDirectory(Paths.get("../video-logs", Integer.toString(logNumber)));
    } catch (IOException error) {
      System.err.println("WARNING: unable to create log directory '../video-logs/" + logNumber + "'");
    }
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
      System.out.println("Event handler process change event: " + option.id +
          "=>" + option.getValue());
    }

    // Limited handler, will only respond when Config.VIDEO_RATE is modified
    @Handler(condition = "msg == 'VIDEO_RATE'")
    public void onVideoRate(Config msg) {
      System.out.println("Video Rate event detected: '" + msg.getValue() + "'");
      this.setFPS(msg.intValue());
    }
  }
}
