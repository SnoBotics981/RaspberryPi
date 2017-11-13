import edu.wpi.cscore.*;
import edu.wpi.first.wpilibj.networktables.*;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

public class VisionProcessor {
  private Moments polygon;
  private Point center = new Point();
  // findContours() returns a list of shapes
  private List<MatOfPoint> targets = new ArrayList<MatOfPoint>();
  private Mat hsv = new Mat(); // Convert frame to HSV
  // OpenCV generates a heirchical sorting of targets
  private Mat targetHierarchy = new Mat();
  private NetworkTable data;
  private Scalar[] coords = new Scalar[2];
  private double filteredAngle = 0;
  private double filteredDistance = 0;

  public VisionProcessor() {
    coords[0] = Color.BLACK.scalar;
    coords[1] = Color.BLACK.scalar;
    data = NetworkTable.getTable("navigation");
  }

  public enum Color {
    WHITE(255, 255, 255), BLACK(0, 0, 0);
    public final Scalar scalar;

    private Color(int R, int G, int B) {
      scalar = new Scalar(R, G, B);
    }
  }

  public void findTargets(Mat frame, CvSource outStream) {
    Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);
    Imgproc.blur(hsv.clone(), hsv, new Size(27,27));

    // Sharpen the image before processing it
    // Disabling this for now because it seems to make the streams unstable
//    Imgproc.GaussianBlur(hsv, sharpen, new Size(0, 0), 3);
//    Core.addWeighted(hsv.clone(), 1.5, sharpen, -0.5, 0, hsv);

    // The light ring is green, but the reflected color is kinda bluish
    Core.inRange(hsv.clone(), new Scalar(50, 8, 200), new Scalar(180, 230, 255), hsv);
    Imgproc.findContours(hsv.clone(), targets, targetHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

    int targetCount = 0;
    for(MatOfPoint target : targets) {
      polygon = Imgproc.moments(target);
      center.x = polygon.get_m10() / polygon.get_m00();
      center.y = polygon.get_m01() / polygon.get_m00();
      double size = Imgproc.contourArea(target);
      int debugSize = new Double(Math.sqrt(size)).intValue();
      // The colorFilter mat only accepts black or white coloring
      if (size < 50.0) continue;
      ++targetCount;
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
    Imgproc.circle(hsv, new Point(coords[0].val[0], coords[0].val[1]), coordSize, Color.WHITE.scalar, 3);
    coordSize = new Double(Math.sqrt(coords[1].val[2])).intValue();
    Imgproc.circle(hsv, new Point(coords[1].val[0], coords[1].val[1]), coordSize, Color.WHITE.scalar, 3);

    double offset = ( (coords[0].val[0] + coords[1].val[0]) / 2 ) - 160;
    filteredAngle = (filteredAngle + offset) / 2;

    double closeness = -1;
    double range = -1;
    // The vision filter should only match a couple of spots, so if the count
    // is unreasonable we don't have a real target
    if (targetCount > 0 && targetCount <= 5) {
      double spacing = Math.abs(coords[0].val[0] - coords[1].val[0]);
      double targetArea = coords[0].val[2] + coords[1].val[2];
      closeness = Math.cbrt(spacing * targetArea);
      range = filteredDistance = (filteredDistance + closeness) / 2;
    }
    VisionTarget.setAngle(new Double(filteredAngle).intValue());
    VisionTarget.setCloseness(new Double(range).intValue());

    data.putNumber("angle", new Double(filteredAngle).intValue());
    data.putNumber("closeness", new Double(range).intValue());

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