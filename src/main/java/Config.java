import java.io.File;
import java.util.Map;
import java.util.HashMap;
import net.engio.mbassy.bus.MBassador;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

public enum Config {
  // Default values set here, runtime values stored in config.json
  VIDEO_WIDTH ("video.width",  "320"),
  VIDEO_HEIGHT("video.height", "240"),
  VIDEO_RATE  ("video.rate",   "15"),
  // For this approach, the HSV object should split the values separate integers
  COLOR_TARGET_LOWER ("target.color.lower", "50,8,200"),
  COLOR_TARGET_UPPER ("target.color.upper", "180,230,255"),
  // Camera config options
  CAMERA_DEBUG("camera.debug", "true"),
  DEBUG_MATCHNAME("debug.matchname", "");

  public final String id;
  private volatile String value;

  public static final MBassador bus = new MBassador();
  private static File configFile = new File("./config.json");
  private static JSONObject config = new JSONObject();

  Config(String id, String value) {
    this.id = id;
    this.value = value;
  }

  public String getValue() { return value; }

  public int intValue() { return Integer.parseInt(this.value); }

  public double[] doubleArray(int length) {
    String[] array = this.value.split(",");
    System.out.println("Setting to array: " + id + "->" + value);
    if (array.length != length) {
      System.out.println("Invalid array length: " + value);
      return null;
    }
    double[] data = new double[array.length];
    for (int i=0; i<array.length; i++) {
      data[i] = Double.parseDouble(array[i]);
    }
    return data;
  }

  public void update(int intValue) {
    this.update(Integer.toString(intValue));
  }

  public synchronized void update(String newValue) {
    // Might not really care about this, unless the file gets large
    System.out.println("Updating value of '" + this.id + "' to '" + newValue + "'");
    if (this.value.equals(newValue)) {
      System.out.println("No change, skipping update activity");
      return;
    }
    this.value = newValue;
    config.put(this.id, this.value);
    // Mutli-threaded event handlers should use 'bus.publishAsync()' to avoid deadlocks
    bus.publish(this);
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
        elem.update(newValue);
    }

    System.out.println("Active configuration: " + config.toString());
    saveConfig();
  }

  // TODO: Need a mechanism to save changes to the configuration
  public static void saveConfig() {
    try {
      FileUtils.writeStringToFile(configFile, config.toString(), "utf-8");
    } catch (Exception e) {
      System.out.println("Error detected: " + e.toString());
    }
  }
}
