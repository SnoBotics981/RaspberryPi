import org.opencv.core.Point;

public class Circle implements Comparable<Circle> {
  private double x, y, size;

  // Default constructor - zero out all values
  public Circle() { reset(0, 0, 0); }

  public Circle(double x, double y, double size) { reset(x, y, size); }

  public void reset() { reset(0, 0, 0); }

  public void reset(double x, double y, double size) {
    this.x = x; this.y = y; this.size = size;
  }

  public void reset(Circle source) { reset(source.x, source.y, source.size); }

  public Point getPoint() { return new Point(x, y); }

  public double getSize() { return size; }

  public double getX() { return x; }

  public int getRadius() { return new Double(Math.sqrt(size)).intValue(); }

  @Override
  public int compareTo(Circle that) {
    return Double.compare(that.size, this.size);
  }
}

