
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import java.io.IOException;
import java.io.PrintWriter;

public class VisionTarget extends AbstractHandler {

  @Override
  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response) throws IOException {

    response.setContentType("application/json; charset=utf-8");
    response.setStatus(HttpServletResponse.SC_OK);
    PrintWriter out = response.getWriter();

    out.println("{ \"angle\": \"0\", \"distance\": \"0\"}");
    baseRequest.setHandled(true);   
  }
}
