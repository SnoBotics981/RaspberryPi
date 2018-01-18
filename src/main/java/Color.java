import java.util.List;
import java.util.ArrayList;
import net.engio.mbassy.listener.Filter;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.IMessageFilter;
import net.engio.mbassy.subscription.SubscriptionContext;
import org.opencv.core.Scalar;

// Some methods use BGR colorspace, others use HSV colorspace
public class Color {
  private static List<String> filterTags = new ArrayList<String>();

  // Use the OpenCV scalar functionality, with a different semantical name
  public static class HSV extends Scalar {
    private String filter = "";

    public HSV(int H, int S, int V) {
      super(H, S, V);
    }

    public HSV(int H, int S, int V, Config event) {
      super(H, S, V);
      if (!filterTags.contains(event)) { filterTags.add(event.id); }
      filter = event.id;
      System.out.println("Listen for: '" + filter + "'");
      Config.bus.subscribe(this);
    }

    // Conveniency utility when typecasting, not required thus far
    public Scalar scalar() { return (Scalar)this; }

    // TODO: Add event handler support to respond to config updates
    @Handler(filters = {@Filter(ColorFilter.class)})
    public void handleEvents(Config msg) {
      if (!msg.id.equals(filter)) { return; }
      System.out.println("Parse/update: " + msg.id);
      double[] data = msg.doubleArray(3);
      if (null == data) return;
      this.set(data);
    }
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

  public static class ColorFilter implements IMessageFilter<Config> {
    @Override
    public boolean accepts(Config msg, SubscriptionContext context) {
      return filterTags.contains(msg.id);
    }
  }
}
