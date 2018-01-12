import org.opencv.core.Scalar;

// Some methods use BGR colorspace, others use HSV colorspace
public class Color {

  // Use the OpenCV scalar functionality, with a different semantical name
  public static class HSV extends Scalar {
    public HSV(int H, int S, int V) {
      super(H, S, V);
    }

    // Conveniency utility when typecasting, not required thus far
    public Scalar scalar() { return (Scalar)this; }

    // TODO: Add event handler support to respond to config updates
  }

  public static class BGR extends Scalar {
    public BGR(int B, int G, int R) {
      super(B, G, R);
    }

    public enum Const {
      WHITE(255, 255, 255), BLACK(0, 0, 0);
      public final BGR color;

      private Const(int B, int G, int R) {
        color = new BGR(B, G, R);
      }
    }
  }
}
