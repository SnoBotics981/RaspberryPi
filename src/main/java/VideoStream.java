import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.CameraServer;
import net.engio.mbassy.listener.Handler;

public class VideoStream {
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
}
