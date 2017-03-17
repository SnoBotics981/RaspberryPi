import static org.junit.Assert.*;
import static org.junit.Assume.*;

import edu.wpi.first.wpilibj.networktables.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;

public class TestCaptureVideo {

  // This test needs to be skipped until the server code has been written
  @Ignore
  @Test
  public void recordVideoDuringAutonomousMode() {
    System.out.println("Starting test");
    NetworkTable.setClientMode();
    NetworkTable.setIPAddress("raspberrypi.local");
    NetworkTable.initialize();

    // Test should not run if the server is not ready
    NetworkTable data = NetworkTable.getTable("navigation");
    assumeTrue("Server not detected, skipping test", data.isConnected());

    try {
    Process getLocalhost = Runtime.getRuntime().exec(new String[]{"hostname"});
    getLocalhost.waitFor();

    BufferedReader readOutput = new BufferedReader(
      new InputStreamReader(getLocalhost.getInputStream()));
    String hostname = readOutput.readLine();
    assumeNotNull("Unable to evaluate hostname, skipping test", hostname);
    assumeTrue(
      "Test is not running on the Raspberry Pi, skipping test",
      hostname.equals("raspberrypi"));
    } catch(IOException | InterruptedException  e) {
      System.out.println("Exception while scanning hostname");
      e.printStackTrace();
      assumeNoException(e);
    }

    // Ready to capture video
    data.putString("roboState", "autonomous");
    // Wait 15 seconds; the vision software should generate video files
    try {
      Thread.sleep(15000);
    } catch (InterruptedException e) {
      fail("Unexpected error while waiting for NetworkTables values");
    }
    // After the robot transitions into teleop mode, the robot should save
    // the captured video to disk
    data.putString("roboState", "teleop");

    // Verify that the files were generated
    // TODO: Inspect the video files for valid attributes
  }
}
