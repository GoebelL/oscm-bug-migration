/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 20.07.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.bugzilla;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** @author goebel */
public class CmdLine {
  private static List<String> SINGLEOPTIONS = Arrays.asList(new String[]{"-d", "-do", "-alo"});
  static String readLine(String format, Object... args) throws IOException {
    if (System.console() != null) {
      return System.console().readLine(format, args);
    }
    System.out.print(String.format(format, args));
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    return reader.readLine();
  }

  static Map<String, String> parseArguments(String[] args, String... mandatory) {
      Map<String, String> opts = new HashMap<String, String>();
      Iterator<String> arg = Arrays.asList(args).iterator();
      while (arg.hasNext()) {
        String key = arg.next();
        if (!key.startsWith("-")) {
            throw new RuntimeException(String.format("Unknown argument %s. Use -u username -p password ..", key));
        }
        if (SINGLEOPTIONS.contains(key)) {
            opts.put(key, "true");
            continue;
        }
        if (arg.hasNext()) {
          opts.put(key, arg.next());
        } else {
          throw new RuntimeException(String.format("Missing value for %s", key));
        }
      }
      for (String m : mandatory) {
          if (null == opts.get(m)) {
              throw new RuntimeException(String.format("Missing option %s", m));
          }
      }
      return opts;
  }
  static char[] readPassword(String format, Object... args) throws IOException {
    if (System.console() != null) return System.console().readPassword(format, args);
    return readLine(format, args).toCharArray();
  }
  
  
}
