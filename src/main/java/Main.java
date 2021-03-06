import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.cscore.*;
import java.util.Arrays;
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

    // Limit the code to one camera to avoid uncertainty in allocation order
    UsbCameraManager camera = new UsbCameraManager("Vision Camera", 0);

    // This image feed displays the debug log (whatever the filters computed)
    VideoStream debugStream = new VideoStream("CV Image Source", "Debug Stream", 1186);
    VideoStream rawView = new VideoStream("Unprocessed Video Feed", "Front View", 1187);

    debugStream.setToggleFlag(Config.CAMERA_DEBUG.id);

    // This creates a CvSink for us to use. This grabs images from our selected camera,
    // and will allow us to use those images in OpenCV.  To toggle processing
    // feeds (below), set the source to use a different device.
    CvSink imageSink = new CvSink("CV Image Grabber");
    imageSink.setSource(camera);

    // All Mats and Lists should be stored outside the loop to avoid allocations
    // as they are expensive to create
    Mat inputImage = new Mat();   // Get frame from camera
    vision = new VisionProcessor();

    // Embed a Jetty server for non-video content
    new HttpManager().runServer();
    System.out.println("Server ready, starting the camera feeds");

    // Infinitely process camera feeds
    while (true) {
      // Grab a frame. If it has a frame time of 0, there was an error.
      // If so, skip and continue
      long frameTime = imageSink.grabFrame(inputImage);
      if (frameTime == 0) continue;

      vision.findTargets(inputImage,debugStream.source());

      // Display the raw camera feed in a separate filter
      Imgproc.line(inputImage, new Point(100,20), new Point(100,220), new Scalar(0, 255, 0), 6);
      Imgproc.line(inputImage, new Point(220,20), new Point(220,220), new Scalar(0, 255, 0), 6);
      rawView.source().putFrame(inputImage);
      inputImage.release();
      System.gc();
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
