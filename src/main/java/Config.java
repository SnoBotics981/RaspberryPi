import java.io.File;
import java.util.Map;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

public enum Config {
  VIDEO_WIDTH ("video.width",  "320"),
  VIDEO_HEIGHT("video.height", "240"),
  VIDEO_RATE  ("video.rate",   "15");

  public final String id;
  private String value;

  private static File configFile = new File("./config.json");
  private static JSONObject config = new JSONObject();
  private static boolean dirty = false;

  Config(String id, String value) {
    this.id = id;
    this.value = value;
  }

  public String getValue() { return value; }

  public int intValue() { return Integer.parseInt(this.value); }

  public void update(String newValue) {
    // Might not really care about this, unless the file gets large
    if (!this.value.equals(newValue)) { dirty = true; }
    this.value = newValue;
    config.put(this.id, this.value);
  }

  public static Config find(String id) {
    for(Config elem : values()) {
      if(elem.id.equals(id)) return elem;
    }
    return null;
  }

  public static void initialize() {
    // Attempt to read the config file, overriding defaults if present
    try {
      String data = FileUtils.readFileToString(configFile, "utf-8");
      config = new JSONObject(data);
    } catch (Exception e) {
      // In case of any exceptions, assume the file cannot be read
      System.out.println("Error reading file: " + e.toString());
    }
    System.out.println("Configuration from file: " + config.toString());

    // For each enumeration element - pull override value from JSON (if any)
    // If the element is missing from the JSON, add it in
    for(Config elem : values()) {
      String newValue = config.optString(elem.id, elem.value);
      if (!config.has(elem.id)) dirty = true;
        elem.update(newValue);
    }

    System.out.println("Active configuration: " + config.toString());
    saveConfig();
  }

  // TODO: Need a mechanism to save changes to the configuration
  public static void saveConfig() {
    try {
      FileUtils.writeStringToFile(configFile, config.toString(), "utf-8");
      dirty = false;
    } catch (Exception e) {
      System.out.println("Error detected: " + e.toString());
    }
  }
}
