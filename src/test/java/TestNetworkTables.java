import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.text.DecimalFormat;
import java.text.ParseException;

import org.junit.Test;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;

public class TestNetworkTables implements ITableListener {

  /**
   *  This test case attempts to interact directly with the Raspberry Pi,
   *  without a connection to the roboRIO.
   */
  @Test
  public void testMain() {
    System.out.println("Starting test");
    NetworkTable.setClientMode();
    NetworkTable.setIPAddress("raspberrypi.local");
    NetworkTable.initialize();

    NetworkTable data = NetworkTable.getTable("navigation");
    assumeTrue("Server not detected, skipping test", data.isConnected());
    data.addTableListener(this);

    System.out.println("Wait 5 seconds for valueChanged() to respond");
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      fail("Unexpected error while waiting for NetworkTables values");
    }
    System.out.println("Keys:" + data.getKeys().toString());

    assertTrue("Server did not report an angle to the target",
        data.getKeys().contains("angle"));
    assertTrue("Server did not report closeness to the target",
        data.getKeys().contains("closeness"));
    assertNotNull("Angle value reported as null",
        data.getValue("angle", null));
    assertNotNull("Closeness value reported as null",
        data.getValue("closeness", null));

    int angle = this.getValue(data, "angle", -999);
    int closeness = this.getValue(data, "closeness", -1);

    System.out.println("Angle: " + angle);
    System.out.println("Closeness: " + closeness);
  }

  /**
   * Bundle the code to parse NetworkTables values in a safe wrapper.
   * Methods to directly output integers are available, but are deprecated.
   *
   * @param source Table containing shared data
   * @param key Name of the data element
   * @param defaultValue Value to be used to indicate no data was found
   * @return The shared data value, or the contents of defaultValue
   */
  public int getValue(NetworkTable source, String key, int defaultValue) {
    String data = source.getValue(key, defaultValue).toString();
    try {
      return new DecimalFormat("#").parse(data).intValue();
    } catch (ParseException e) {
      System.out.println(
          String.format("WARNING: unable to parse integer value from '%s'", data));
      return defaultValue;
    }
  }

  @Override
  public void valueChanged(ITable source, String key, Object value, boolean isNew) {
    System.out.println("Source: " + source.toString()
            + "; Key: " + key + "; Value: " + value.toString()
            + "; isNew: " + new Boolean(isNew).toString());
  }
}
