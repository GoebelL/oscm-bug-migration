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
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.ConfigurationException;
import org.gitlab4j.api.GitLabApiException;
import org.oscm.bugzilla.model.BugObject;
import org.oscm.bugzilla.model.CommentObject;

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

  @SuppressWarnings("boxing")
  public void run(String[] args) throws ConfigurationException, GitLabApiException, IOException {
    readCredentials(args);

    Map<String, BugObject> map = readLogFile();

    BugzillaHttpSession session = getSession();
    if (session.open()) {
      BugImporter gitLab = new BugImporter(session);

      gitLab.connect(CONFIG.GITLAB_BASEURL, credentials, CONFIG.TARGET_PROJECT_ID);

      if (adaptLinksOnly(args)) {
        handleAdaptLinks(gitLab, map);
        System.out.println("Bug links adapted.");
      } else {
        handleDelete(args, gitLab);

        DefaultSearchData search = new DefaultSearchData();
        search.add("product", CONFIG.SOURCE_PROJECT);
        search.add("limit", "0");

        Iterable<Issue> i = session.searchBugs(search, null);
        for (Issue bug : i) {
          if (!gitLab.importIssue(bug, map)) {
            continue;
          }
        }
        System.out.println(String.format("%s GitLab issues imported.", map.entrySet().size()));
      }
      session.close();
    }
  }

  private void handleAdaptLinks(BugImporter gitLab, Map<String, BugObject> m) {
    m.entrySet().stream().forEach(e -> replace(gitLab, m, e.getValue()));
  }

  private void replace(BugImporter gitLab, Map<String, BugObject> m, BugObject o) {
    Iterator<CommentObject> i = o.getComments().iterator();
    while (i.hasNext()) {
      try {
        gitLab.updateComment(m, i.next(), o);
      } catch (Exception e) {
        Logger.logError(e);
      }
    }
  }

  private void handleDelete(String[] args, BugImporter gitLab) throws GitLabApiException {
    String delete = CmdLine.parseArguments(args).get("-d");
    if (delete != null) {
      gitLab.deleteAllIssues();
    }
  }

  private boolean adaptLinksOnly(String[] args) throws GitLabApiException {
    String adaptOnly = CmdLine.parseArguments(args).get("-alo");
    if (adaptOnly != null && adaptOnly.equalsIgnoreCase("true")) {
      return true;
    }
    return false;
  }

  public static String replaceText(String str) {
    String s = replaceNL(str);
    return replaceBugId(s);
  }

  private static String replaceNL(String str) {
    return str.replace("\n", "<br>");
  }

  private static String replaceBugId(String str) {
    String bugPattern = "bug.[0-9]*";
    Pattern p = Pattern.compile("bug.([0-9]+)");
    Matcher m = p.matcher(str);
    if (m.find()) {
      String srcBug = m.group(1);
      String query = String.format("%s&group_id=&project_id=%s", srcBug, CONFIG.TARGET_PROJECT_ID);
      query =
          "http://estscm1.intern.est.fujitsu.com/search?utf8=%E2%9C%93&search="
              + query
              + "&scope=issues";
      String link = String.format("<a href=\"%s\">bug %s<a>", query, srcBug);
      str = str.replace(srcBug, link);
    }
    return str;
  }

  private Map<String, BugObject> readLogFile() throws IOException {
    map = Logger.instance().read();
    return map;
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

    System.out.println(String.format("Connecting: %s", CONFIG.BUGZILLA_BASEURL));
    se.setHttpSessionParams(sp);
    se.setBaseUrl(new URL(CONFIG.BUGZILLA_BASEURL));
    se.setBugzillaBugClass(DefaultIssue.class);

    return se;
  }

  public static void main(String[] args) {
    try {
      new Migration().run(args);
    } catch (Exception e) {
      Logger.logError(e);
    } finally {
      if (Logger.instance != null) Logger.instance.release();
    }
  }
}
