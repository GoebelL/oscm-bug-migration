/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 23.07.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.bugzilla;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
/** @author goebel */
public class Config {

  String TARGET_PROJECT_ID = "538";
  String GITLAB_BASEURL = "http://10.140.19.25";
  String BUGZILLA_BASEURL = "http://wwwi.est.fujitsu.com/bug01";
  String SOURCE_PROJECT = "Business Enabling Services";
  String TARGET_LABEL_ENHANCEMENT = "Feature Request";
  String TARGET_LABEL_BUG = "Bug";
  String PROJECT_LABEL = "";

  static String rootPath;
  private Properties props;
  static Config instance;

  static {
    String rootDir = System.getProperty("user.dir");
    System.err.println("[DEBUG] rootdir " + rootDir);
    rootPath = rootDir;
  }

  private Config(Properties props) {
    this.props = props;
  }

  public static Config getInstance() {
    if (instance == null) {
      try {
        instance = load("/config/config.properties");
        instance.init();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return instance;
  }

  public static Config load(String path) throws IOException {

    Properties props = new Properties();
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(new File(rootPath + "/" + path));
      props.load(fis);

      return new Config(props);

    } finally {
      if (fis != null)
        try {
          fis.close();
        } catch (IOException e) {
        }
    }
  }

  private void init() {
    GITLAB_BASEURL = props.getProperty("gitlab.baseurl", GITLAB_BASEURL);
    BUGZILLA_BASEURL = props.getProperty("bugzilla.baseurl", BUGZILLA_BASEURL);

    TARGET_PROJECT_ID = ensure(props, "gitlab.projectid");

    TARGET_LABEL_ENHANCEMENT =
        props.getProperty("gitlab.label.enhancement", TARGET_LABEL_ENHANCEMENT);
    TARGET_LABEL_BUG = props.getProperty("gitlab.label.bug", TARGET_LABEL_BUG);

    SOURCE_PROJECT = ensure(props, "bugzilla.project");
    PROJECT_LABEL = ensure(props, "gitlab.label.project");
  }

  public boolean isProductiveRun() {
    return Boolean.valueOf(props.getProperty("migration.productive", "false")).booleanValue();
  }

  private String ensure(Properties p, String key) { // TODO Auto-generated method stub
    String value = p.getProperty(key);
    if (value == null || value.trim().isEmpty()) {
      throw new RuntimeException(String.format("Missing value for '%s' in config.properties", key));
    }
    return value;
  }
}
