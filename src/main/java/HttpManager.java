import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

/**
 * General-purpose network manager for the vision software.
 * The Rasbperry Pi is not easily accessible while running the robot,
 * so any interaction with the Pi (outside of the video feeds) should go
 * through here.
 */
public class HttpManager {
  Server httpManager;
  ServletHandler router;

  public HttpManager() {
    httpManager = new Server(1181);
    router = new ServletHandler();
    httpManager.setHandler(router);
    router.addServletWithMapping(VisionTarget.class, "/*");
  }

  public void runServer() {
    Thread server = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          httpManager.start();
          httpManager.join();
        } catch (Exception e) {
          System.out.println("Unable to start the Vision Target service: " + e.toString() );
        }
      }
    });
    server.start();
  }
}
