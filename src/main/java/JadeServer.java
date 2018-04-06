import de.neuland.jade4j.Jade4J;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class JadeServer extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest  request,
                    HttpServletResponse response) throws IOException {
    Map<String, Object> model = new HashMap<String, Object>();
    Map<String, Object> config = new HashMap<String, Object>();
    for (Config option : Config.values()) {
      config.put(option.name(), option.getValue());
    }
    model.put("Config", config);
    model.put("Env", Environment.get());

    String html = Jade4J.render("../static/config.jade", model);

    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentLengthLong(html.length());
    response.getWriter().print(html);
  }
}
