import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

public class Environment {
  private final static Environment self = new Environment();
  private ScheduledExecutorService threadManager =
      Executors.newScheduledThreadPool(1);
  public List<String> matches = new ArrayList<String>();

  private Environment() {
    matches.add("test1");
    matches.add("test2");
    threadManager.scheduleAtFixedRate(updateEnv, 2, 10, TimeUnit.SECONDS);
  }

  public static Environment get() {
    return self;
  }

  public String activeMatch() {
    return "test3";
  }

  private Runnable updateEnv = () -> {
    List<String> searchMatches = new ArrayList<String>();
    System.out.println("Running updateEnv() task");
    try {
      if (Files.notExists(Paths.get(Main.imageLogPrefix))) {
        Files.createDirectory(Paths.get(Main.imageLogPrefix));
      }
    } catch (IOException error) {
      System.err.println("WARNING: unable to read from image log");
    }
  };
}
