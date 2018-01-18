import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ApiHandler extends HttpServlet {

  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request,
                    HttpServletResponse response) throws IOException {

    System.out.println("Running API handler");

    System.out.println("Input parameters: " + request.getParameterMap().size());

    Enumeration params = request.getParameterNames();
    while (params.hasMoreElements()) {
      String key = (String)params.nextElement();
      String[] values = request.getParameterValues(key);
      // The values array should only have a single config value
      // Log results for any debugging activities
      System.out.println(key + "=>" + values.length);
      String arrayRender = "[";
      for(int i=0; i<(values.length-2); i++) {
        arrayRender += values[i] + ",";
      }
      arrayRender += values[values.length-1] + "]";
      System.out.println(arrayRender);
      if (null != Config.find(key)) {
        // Operating assumption here is that each key should map to a single
	// value, stored as a String in the Config enum.
	// If any input fields are left blank, do not change that element
        if (!values[0].equals("")) Config.find(key).update(values[0]);
      } else {
        System.out.println("Unable to find setting option: " +  key);
      }
    }
    response.sendRedirect("/config.html");
  }
}
