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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.gitlab4j.api.GitLabApiException;
import org.oscm.bugzilla.model.BugObject;

import b4j.core.DefaultIssue;
import b4j.core.DefaultSearchData;
import b4j.core.Issue;
import b4j.core.session.BugzillaHttpSession;
import b4j.util.HttpSessionParams;
import rs.baselib.security.AuthorizationCallback;
import rs.baselib.security.SimpleAuthorizationCallback;

/** @author goebel */
public class Migration {

  public static Config CONFIG;

  static {
    try {
      CONFIG = Config.getInstance();
    } catch (Exception e) {
      Logger.logError(e);
      System.exit(1);
    }
  }

  public static final DateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private String[] credentials = new String[2];
  Map<String, BugObject> map = Collections.synchronizedMap(new HashMap<String, BugObject>());

  public void run(String[] args) throws ConfigurationException, GitLabApiException, IOException {
    readCredentials(args);

    readLogFile();

    BugzillaHttpSession session = getSession();
    if (session.open()) {
      BugImporter gitLab = new BugImporter(session);

      gitLab.connect(CONFIG.GITLAB_BASEURL, credentials, CONFIG.TARGET_PROJECT_ID);
      handleDelete(args, gitLab);

      DefaultSearchData search = new DefaultSearchData();
      search.add("product", CONFIG.SOURCE_PROJECT);
      search.add("limit", "0");

      Iterable<Issue> i = session.searchBugs(search, null);
      for (Issue bug : i) {
        if (null == map.get(bug.getId())) {
          if (!gitLab.importIssue(bug, map)) {
            continue;
          }
        }
      }
      session.close();
    }
  }

  private void handleDelete(String[] args, BugImporter gitLab)
      throws GitLabApiException {
    String delete = CmdLine.parseArguments(args).get("-d");
    if (delete != null) {
      gitLab.deleteAllIssues();
    }
  }

  private void readLogFile() throws IOException {
    map = Logger.instance().read();
  }

  void readCredentials(String[] args) throws IOException {
    if (args.length < 1) {

      credentials[0] = CmdLine.readLine("Username: ");
    } else {

      Map<String, String> opts = CmdLine.parseArguments(args, "-u");

      credentials[0] = opts.get("-u");
    }

    credentials[1] = Credentials.ask(credentials[0]);
    if (credentials[1] == null) {
      System.out.println("Canceled by user.");
      System.exit(0);
    }
  }

  public BugzillaHttpSession getSession() throws MalformedURLException, ConfigurationException {

    BugzillaHttpSession se = new BugzillaHttpSession();
    HttpSessionParams sp = new HttpSessionParams();
    AuthorizationCallback ac = new SimpleAuthorizationCallback(credentials[0], credentials[1]);
    sp.setAuthorizationCallback(ac);
    // sp.setProxyHost("proxy.intern.est.fujitsu.com");
    // sp.setProxyPort(8080);
    System.out.println(String.format("Connecting: %s", CONFIG.BUGZILLA_BASEURL));
    se.setHttpSessionParams(sp);
    se.setBaseUrl(new URL(CONFIG.BUGZILLA_BASEURL));
    se.setBugzillaBugClass(DefaultIssue.class);
    return se;
  }

  public static void main(String[] args) {
    try {
      new Migration().run(args);
    } catch (Exception e) { // TODO Auto-generated catch block
      Logger.logError(e);
    } finally {
      if (Logger.instance != null) Logger.instance.release();
    }
  }
}
