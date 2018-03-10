import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.CameraServer;
import java.util.HashMap;
import java.util.Map;
import net.engio.mbassy.listener.Filter;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.IMessageFilter;
import net.engio.mbassy.subscription.SubscriptionContext;

public class VideoStream {
  private static Map<VideoStream, String> filterTags = new HashMap<VideoStream, String>();
  private static CameraServer manager = CameraServer.getInstance();

  private MjpegServer stream;
  private CvSource source;
  private String title, sourceName;
  private int port;

  public VideoStream(String inputSource, String title, int port) {
    this.title = title;
    this.port = port;
    this.sourceName = inputSource;
    stream = enable();
    updateSource();
    Config.bus.subscribe(this);
  }

  public void disable() { manager.removeServer(title); }
  public MjpegServer enable()  { return manager.addServer(title, port); }
  public CvSource source() { return source; }

  private void updateSource() {
    this.source = new CvSource(
      sourceName,
      VideoMode.PixelFormat.kMJPEG,
      Config.VIDEO_WIDTH.intValue(),
      Config.VIDEO_HEIGHT.intValue(),
      Config.VIDEO_RATE.intValue()
    );
    stream.setSource(source);
  }

  @Handler(condition = "msg == 'VIDEO_RATE'")
  public void onVideoRate(Config msg) {
    System.out.println("Update VideoStream fps: '" + msg.getValue() + "'");
    updateSource();
  }

  public void setToggleFlag(String keyName) {
    filterTags.put(this, keyName);
  }

  @Handler(filters = {@Filter(VideoFilter.class)})
  public void toggleStream(Config msg) {
    System.out.println("Preparing to process VideoStream event: " + msg.id + "=>" + msg.getValue());
    if (!filterTags.containsKey(this)) { return; }
    String myFilter = filterTags.get(this);
    if (!msg.id.equals(myFilter)) { return; }
    System.out.println("Parse/update: " + msg.id);
    switch (msg.getValue()) {
      case "true":
        stream = enable();
        updateSource();
        break;
      case "false":
        stream.free();
        break;
      default:
        System.out.println("WARNING: Invalid boolean value stored in config (" + msg.id + "=>" + msg.getValue() + ")");
    }
  }

  public static class VideoFilter implements IMessageFilter<Config> {
    @Override
    public boolean accepts(Config msg, SubscriptionContext context) {
      return filterTags.containsValue(msg.id);
    }
  }
}
