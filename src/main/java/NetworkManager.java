import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkManager {

  // Attempt to use a NetworkInterface object to autodetect the runtime environment
  Enumeration<NetworkInterface> interfaces = null;
  List<String> networkAddresses = new ArrayList<String>();

  public NetworkManager() {
    try {
      interfaces = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      // Need a good way to track init failures
      System.out.println("SocketException error when attempting to load network devices");
      e.printStackTrace();
      }
    }

  public void scanInterfaces() {
    if (interfaces != null) {
      while ( interfaces.hasMoreElements() ) {
        NetworkInterface i = interfaces.nextElement();
        Enumeration<InetAddress> addresses = i.getInetAddresses();
        while ( addresses.hasMoreElements() ) {
          InetAddress address = addresses.nextElement();
          if (!address.isLoopbackAddress() && address.isSiteLocalAddress()) {
            System.out.println("Network address: " + address.getHostAddress());
            networkAddresses.add(address.getHostAddress());
          }
        }
      }
    }
  }

  public int getTeamNumber() {
    int teamNumber = 0;
    try {
      System.out.println("Use avahi to detect the configured team address");
      Process findTeamNumber = Runtime.getRuntime().exec(new String[]{
        "bash","-c","avahi-browse -larpt | grep \"=;eth0;IPv4\" | grep \";SSH Remote Terminal;\""
      });
      findTeamNumber.waitFor();
      BufferedReader data = new BufferedReader(
        new InputStreamReader(findTeamNumber.getInputStream()));
      String line;
      while((line = data.readLine()) != null) {
        String hostname = line.split(";")[6];
        String avahiID = hostname.split("-")[1];
        teamNumber = Integer.parseInt(avahiID);
      }
      data.close();
    } catch (IOException | InterruptedException e) {
      System.out.println("Exception while scanning robot network");
      e.printStackTrace();
    }
    return teamNumber;
  }
}
