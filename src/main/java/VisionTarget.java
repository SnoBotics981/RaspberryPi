
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import java.io.IOException;
import java.io.PrintWriter;

public class VisionTarget extends HttpServlet {

  static int angle = 0;
  static int closeness = 0;

  @Override
  public void doGet(HttpServletRequest request,
                    HttpServletResponse response) throws IOException {

    response.setContentType("application/json; charset=utf-8");
    response.setStatus(HttpServletResponse.SC_OK);
    PrintWriter out = response.getWriter();

    out.println("{ \"angle\": \"" + Integer.toString(angle) + "\", \"closeness\": \""
                                  + Integer.toString(closeness) + "\"}");
  }

  public static void setAngle(int value) {
    angle = value;
  }

  public static void setCloseness(int value) {
    closeness = value;
  }
}
