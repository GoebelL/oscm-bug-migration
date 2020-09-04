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
import java.text.ParseException;
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
import b4j.core.SearchResultCountCallback;
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

  private int bugCount = 0;
  private SearchResultCountCallback scb =
      new SearchResultCountCallback() {

        @Override
        public void setResultCount(int resultCount) {
          bugCount = resultCount;
        }
      };

  public static final DateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private String[] credentials = new String[2];
  Map<String, BugObject> map = Collections.synchronizedMap(new HashMap<String, BugObject>());

  private void applyDateFilter(DefaultSearchData d) {
    String fromDate = CONFIG.getFromDate();
    if (fromDate.trim().length() > 0) {
      try {
        new SimpleDateFormat("yyyy-MM-dd").parse(fromDate);
      } catch (ParseException e) {
        throw new RuntimeException(
            "Failed to parse given migration.fromDate as yyyy-MM-dd. Check your config.properties.",
            e);
      }
      d.add("chfield", "[Bug creation]");
      d.add("chfieldfrom", fromDate);
      d.add("chfieldto", "Now");
    }
  }

  @SuppressWarnings("boxing")
  public void run(String[] args) throws ConfigurationException, GitLabApiException, IOException {
    readCredentials(args);

    Map<String, BugObject> map = readLogFile();

    UIPanel.popUp();
    printHeader();

    BugzillaHttpSession session = getSession();
    if (session.open()) {
      BugImporter gitLab = new BugImporter(session);

      gitLab.connect(CONFIG.GITLAB_BASEURL, credentials, CONFIG.TARGET_PROJECT_ID);

      handleDelete(args, gitLab);

      if (doImport(args)) {
        DefaultSearchData search = new DefaultSearchData();
        search.add("product", CONFIG.SOURCE_PROJECT);
        search.add("limit", "0");
        applyDateFilter(search);

        Iterable<Issue> i = session.searchBugs(search, scb);
        System.out.println(String.format("Got %s bugs. Now importing each one...", bugCount));
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

  private boolean doImport(String[] args) {
    String deleteOnly = CmdLine.parseArguments(args).get("-do");
    return deleteOnly == null;
  }

  private void printHeader() {
    System.out.println(
        "*****************************************************************************");
    System.out.println(
        "*                                                                           *");
    System.out.println(
        "*  Copyright FUJITSU LIMITED 2020                                           *");
    System.out.println(
        "*                                                                           *");
    System.out.println(
        "*  Created: 2020-09-04, L. Goebel                                           *");
    System.out.println(
        "*                                                                           *");
    System.out.println(
        "*****************************************************************************\n\n");
  }

  private void handleDelete(String[] args, BugImporter gitLab) throws GitLabApiException {
    String delete = CmdLine.parseArguments(args).get("-d");
    String deleteOnly = CmdLine.parseArguments(args).get("-do");

    if (delete != null || deleteOnly != null) {
      gitLab.deleteAllIssues();
    }
  }

  private Map<String, BugObject> readLogFile() throws IOException {
    return Logger.instance().read();
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
