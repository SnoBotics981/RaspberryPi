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
  private Circle[] coords = new Circle[2];
  private double filteredAngle = 0;
  private double filteredDistance = 0;
  private Color.HSV lowerBound = new Color.HSV(50, 8, 200, Config.COLOR_TARGET_LOWER);
  private Color.HSV upperBound = new Color.HSV(180, 230, 255, Config.COLOR_TARGET_UPPER);

  public VisionProcessor() {
    coords[0] = new Circle();
    coords[1] = new Circle();
    data = NetworkTable.getTable("navigation");
  }

  public void findTargets(Mat frame, CvSource outStream) {
    Mat inputFrame = frame.clone();
    Imgproc.cvtColor(inputFrame, hsv, Imgproc.COLOR_BGR2HSV);
    Imgproc.blur(hsv.clone(), hsv, new Size(27,27));

    // Sharpen the image before processing it
    // Disabling this for now because it seems to make the streams unstable
//    Imgproc.GaussianBlur(hsv, sharpen, new Size(0, 0), 3);
//    Core.addWeighted(hsv.clone(), 1.5, sharpen, -0.5, 0, hsv);

    // The light ring is green, but the reflected color is kinda bluish
    Core.inRange(hsv.clone(), lowerBound, upperBound, hsv);
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
      if (size >= coords[0].getSize()) {
        coords[1].reset(coords[0]);
        coords[0].reset(center.x, center.y, size);
      } else {
        if (size >= coords[1].getSize()) {
          coords[1].reset(center.x, center.y, size);
        }
      }
    }
    int coordSize = coords[0].getRadius();
    Imgproc.circle(hsv, coords[0].getPoint(), coordSize, Color.BGR.Const.WHITE.color, 3);
    coordSize = coords[1].getRadius();
    Imgproc.circle(hsv, coords[1].getPoint(), coordSize, Color.BGR.Const.WHITE.color, 3);

    double offset = ( (coords[0].getX() + coords[1].getX()) / 2 ) - 160;
    filteredAngle = (filteredAngle + offset) / 2;

    double closeness = -1;
    double range = -1;
    // The vision filter should only match a couple of spots, so if the count
    // is unreasonable we don't have a real target
    if (targetCount > 0 && targetCount <= 5) {
      double spacing = Math.abs(coords[0].getX() - coords[1].getX());
      double targetArea = coords[0].getSize() + coords[1].getSize();
      closeness = Math.cbrt(spacing * targetArea);
      range = filteredDistance = (filteredDistance + closeness) / 2;
    }
    VisionTarget.setAngle(new Double(filteredAngle).intValue());
    VisionTarget.setCloseness(new Double(range).intValue());

    data.putNumber("angle", new Double(filteredAngle).intValue());
    data.putNumber("closeness", new Double(range).intValue());

    // Reseet target detectors after each frame
    coords[0].reset();
    coords[1].reset();
    targets.clear();

    // Stream the filtered/processed data to the first source (for debugging the target detection)
    outStream.putFrame(hsv);

    // OpenCV needs help with memory management
    hsv.release();
    inputFrame.release();
  }
}
