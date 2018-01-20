import java.util.List;
import java.util.ArrayList;
import net.engio.mbassy.listener.Filter;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.IMessageFilter;
import net.engio.mbassy.subscription.SubscriptionContext;
import org.opencv.core.Scalar;

public class Color extends Scalar {
  private static List<String> filterTags = new ArrayList<String>();
  private String filter = "";

  // Some methods use BGR colorspace, others use HSV colorspace
  public Color(double A, double B, double C) {
    super(A, B, C);
  }

  public Color(double A, double B, double C, Config event) {
    super(A, B, C);
    if (!filterTags.contains(event)) { filterTags.add(event.id); }
    filter = event.id;
    System.out.println("Listen for: '" + filter + "'");
    Config.bus.subscribe(this);
  }

  // TODO: Add event handler support to respond to config updates
  @Handler(filters = {@Filter(ColorFilter.class)})
  public void handleEvents(Config msg) {
    if (!msg.id.equals(filter)) { return; }
    System.out.println("Parse/update: " + msg.id);
    double[] data = msg.doubleArray(3);
    if (null == data) return;
    this.set(data);
  }

  public enum Const {
    WHITE(255, 255, 255), BLACK(0, 0, 0);
    public final Color color;

    private Const(double A, double B, double C) {
      color = new Color(A, B, C);
    }
  }

  public static class ColorFilter implements IMessageFilter<Config> {
    @Override
    public boolean accepts(Config msg, SubscriptionContext context) {
      return filterTags.contains(msg.id);
    }
  }
}
