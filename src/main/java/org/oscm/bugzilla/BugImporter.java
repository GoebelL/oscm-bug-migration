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
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.ProxyClientConfig;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.Issue;
import org.oscm.bugzilla.gitlab.IssueFactory;
import org.oscm.bugzilla.gitlab.TargetIssue;
import org.oscm.bugzilla.model.BugObject;
import org.oscm.bugzilla.model.CommentObject;

/** @author goebel */
public class BugImporter {

  private GitLabApi client;
  private String projectId;

  private List<Issue> allIssues = null;

  @SuppressWarnings("boxing")
  void connect(String url, String[] credentials, String projectId) throws ConnectException {

    try {

      Map<String, Object> proxyConfig =
          ProxyClientConfig.createProxyClientConfig("http://proxy.intern.est.fujitsu.com:8080");

      client = GitLabApi.oauth2Login(url, credentials[0], credentials[1], null, proxyConfig, true);

      client.setRequestTimeout(15000, 20000);
    } catch (GitLabApiException e) {
      System.err.println("Error accessing " + url);
      Logger.logError(e);
      throw new ConnectException(e.getLocalizedMessage());
    }
    this.projectId = projectId;
  }

  public String getDefaultIssueHeader(String user, Date time, String description) {
    return String.format(
        "Originally reported from: **%s** at %s.\n\n\\%s",
        user, Migration.DATEFORMAT.format(time), description);
  }

  private List<Issue> getAllIssues() throws GitLabApiException {
    if (allIssues == null) {
      allIssues = client.getIssuesApi().getIssues(projectId);
    }
    return allIssues;
  }

  public boolean isContained(List<Issue> issues, b4j.core.Issue bug) {
    for (Issue i : issues) {
      if (bug.getSummary().equals(i.getTitle())) {
        return true;
      }
    }
    return false;
  }

  private boolean isClosed(b4j.core.Issue bug) {
    return !bug.getStatus().isOpen();
  }

  private String getIssueStateLabel(b4j.core.Issue bug) {
    if ("enhancement".equals(bug.getSeverity().getName().toLowerCase())) {
      return Config.getInstance().TARGET_LABEL_ENHANCEMENT;
    } else {
      return Config.getInstance().TARGET_LABEL_BUG;
    }
  }

  private String getIssuePrioLabel(b4j.core.Issue bug) {
    return bug.getPriority().getName();
  }

  private String getProjectLabel() {
    return Config.getInstance().PROJECT_LABEL;
  }

  private String getLabels(b4j.core.Issue bug) { 
    return String.format(
        "%s,%s,%s", getProjectLabel(), getIssueStateLabel(bug), getIssuePrioLabel(bug));
  }

  public boolean importIssue(b4j.core.Issue bug, Map<String, BugObject> map)
      throws GitLabApiException, IOException {

    // Skip already imported bugs
    if (isContained(getAllIssues(), bug)) {
      return false;
    }

    TargetIssue ti = IssueFactory.getInstance().newTargetIssue(client, projectId);

    // Import to GitLab
    Issue i = importIssue(ti, bug, getLabels(bug));

    // Import discussions with attachment
    List<Discussion> discussions = importComments(ti, bug, i);

    // Close for non-open bugs
    closeIfNotOpen(ti, bug, i.getLabels().stream().collect(Collectors.joining(",")), i);

    // Log
    writeToLogs(bug, discussions, map, i);

    return true;
  }

  private void writeToLogs(
      b4j.core.Issue bug, List<Discussion> discussions, Map<String, BugObject> map, Issue i)
      throws IOException {
     
    System.out.println(String.format("Bug %s imported as Issue #%s to GitLab project %s.", bug.getId(), i.getIid(), projectId));
    BugObject m =  getBugModel(bug, discussions, i);
    Logger.instance().writeBug(m);
    map.put(bug.getId(), m);
  }

  BugObject getBugModel( b4j.core.Issue bug, List<Discussion> discussions, Issue i) {
      List<CommentObject> comments = new ArrayList<CommentObject>();
      for (Discussion d : discussions) {
          d.getNotes().stream().forEach(n -> comments.add(new CommentObject(n.getBody())));
        ;
      }
      return new BugObject(bug, comments, String.valueOf(i.getIid()));
  }
  
  private List<Discussion> importComments(TargetIssue ti, b4j.core.Issue bug, Issue i) {
    return ti.importComments(bug, i);
  }

  private void closeIfNotOpen(TargetIssue ti, b4j.core.Issue bug, final String labels, Issue i)
      throws GitLabApiException {
    if (isClosed(bug)) {
      ti.closeIssue(bug, labels, i);
    }
  }

  private Issue importIssue(TargetIssue ti, b4j.core.Issue bug, final String labels)
      throws GitLabApiException {
    return ti.create(
        bug,
        labels,
        getDefaultIssueHeader(
            bug.getReporter().getId(), bug.getCreationTimestamp(), bug.getDescription()));
  }
}
