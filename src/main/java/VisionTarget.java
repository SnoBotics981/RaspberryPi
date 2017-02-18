
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import java.io.IOException;
import java.io.PrintWriter;

public class VisionTarget extends AbstractHandler {

  int angle = 0;
  int distance = 0;

  @Override
  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response) throws IOException {

    response.setContentType("application/json; charset=utf-8");
    response.setStatus(HttpServletResponse.SC_OK);
    PrintWriter out = response.getWriter();

    out.println("{ \"angle\": \"" + Integer.toString(angle) + "\", \"distance\": \"0\"}");
    baseRequest.setHandled(true);   
  }

  public void setAngle(int value) {
    angle = value;
  }
}
