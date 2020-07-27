/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 22.07.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.bugzilla;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.oscm.bugzilla.model.BugObject;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/** @author goebel */
public class Logger {
  static final String LOGFILE = "./log.json";

  static Logger instance;
  JsonWriter json;

  static Logger instance() throws IOException {
    if (instance == null) {
      instance = new Logger();
    }
    return instance;
  }

  Map<String, BugObject> read() throws IOException {
    Map<String, BugObject> model = new HashMap<String, BugObject>();
    File file = new File(LOGFILE);
    if (!file.exists()) {
      file.createNewFile();
      return model;
    }
    byte[] bytes = makePrettyPrint(file);
    if (bytes.length > 0) {
      ObjectMapper mapper = new ObjectMapper();
      BugObject[] bug = mapper.readValue(file, BugObject[].class);
      Arrays.stream(bug).forEach(b -> model.put(b.getIssueId(), b));
    }
    return model;
  }

  void writeBug(BugObject bo) throws IOException {
    json.writeObject(bo);
  }

  static void makePrettyPrint() throws IOException {
    makePrettyPrint(new File(LOGFILE));
  }

  static byte[] makePrettyPrint(File out) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(out.getAbsolutePath()));
    if (bytes.length != 0) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      List<BugObject> bugs = mapper.reader()
              .forType(new TypeReference<List<BugObject>>() {})
              .readValue(bytes);
      
      String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bugs);
      Files.write(
          Paths.get(out.getAbsolutePath()),
          prettyJson.getBytes("UTF-8"),
          StandardOpenOption.TRUNCATE_EXISTING);
      bytes = prettyJson.getBytes("UTF-8");
    }
    return bytes;
  }

  private Logger() throws IOException {
    json = new JsonWriter().start();
  }

  public void release() {
    try {
      json.end();
      makePrettyPrint();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static String timeStamp() {
    return Migration.DATEFORMAT.format(new Date());
  }

  public static void logError(Throwable e) {
    final File f = new File("error.log");
    Path logFile;
    try {
      if (!f.exists()) {
        f.createNewFile();
        logFile = f.toPath();
        Files.write(logFile, "".getBytes("UTF-8"), StandardOpenOption.CREATE);
      } else {
        logFile = f.toPath();
      }

      String header =
          String.format("\n[%s] %s: %s\n", timeStamp(), e.getClass().getName(), e.getMessage());
      Files.write(logFile, header.getBytes("UTF-8"), StandardOpenOption.APPEND);
      Arrays.asList(e.getStackTrace())
          .stream()
          .forEach(
              s -> {
                String entry = "\t in " + s.toString() + "\n";

                try {
                  Files.write(logFile, entry.getBytes("UTF-8"), StandardOpenOption.APPEND);
                } catch (Exception e1) { 
                  e1.printStackTrace();
                }
              });
    } catch (IOException e2) { // TODO Auto-generated catch block
      System.err.println("File logging failed. Sending error output to console.");
      e.printStackTrace();
    }
  }

  class JsonWriter {
    private FileWriter fw;
    private ObjectWriter ow;
    private boolean start = true;

    JsonWriter() throws IOException {
      fw = new FileWriter(LOGFILE);
      ow = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false).writer();
    }

    JsonWriter start() throws IOException {
      fw.write("[\n");
      return this;
    }

    void startObject() throws IOException {
      if (!start) {
        fw.write(",");
      }
      start = false;
    }

    void endObject() throws IOException {
      fw.write("\n");
    }

    void writeObject(Object m) throws JsonGenerationException, JsonMappingException, IOException {
      startObject();
      ow.writeValue(fw, m);
      endObject();
    }

    void end() throws IOException {
      fw.write("]");
      fw.flush();
      ow = null;
      if (fw != null) fw.close();
    }
  }
}
