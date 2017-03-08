import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;

public class FileServer extends HttpServlet {

  private static final long serialVersionUID = 1L;
  // Make this customizable
  private String basePath = "./static";

  @Override
  public void doGet(HttpServletRequest  request,
                    HttpServletResponse response) throws IOException {

    String requestedFile = request.getPathInfo();

    // Basic 404 error if the path cannot be parsed
    if (requestedFile == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // URL-decode the file name (might contain spaces and on) and prepare file object.
    File page = new File(basePath, URLDecoder.decode(requestedFile, "UTF-8"));
    // 404 error if the source is missing
    if (!page.exists()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentLengthLong(page.length());
    PrintWriter out = response.getWriter();
    String contents = FileUtils.readFileToString(page, StandardCharsets.UTF_8);
    out.print(contents);
  }
}
