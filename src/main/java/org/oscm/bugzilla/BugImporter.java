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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.Issue;
import org.oscm.bugzilla.gitlab.IssueFactory;
import org.oscm.bugzilla.gitlab.TargetIssue;
import org.oscm.bugzilla.model.BugObject;
import org.oscm.bugzilla.model.CommentObject;

import b4j.core.Component;
import b4j.core.Version;
import b4j.core.session.BugzillaHttpSession;

/** @author goebel */
public class BugImporter {

  private GitLabApi client;
  private String projectId;

  private List<Issue> allIssues = null;
  private BugzillaHttpSession session = null;

  /** @param session */
  public BugImporter(BugzillaHttpSession session) {
    this.session = session;
  }

  @SuppressWarnings("boxing")
  void connect(String url, String[] credentials, String projectId) throws ConnectException {

    try {
      client = GitLabApi.oauth2Login(url, credentials[0], credentials[1]);

      client.setRequestTimeout(15000, 20000);
    } catch (GitLabApiException e) {
      System.err.println("Error accessing " + url);
      Logger.logError(e);
      throw new ConnectException(e.getLocalizedMessage());
    }
    this.projectId = projectId;
  }

  String getDefaultIssueHeader(b4j.core.Issue bug) {

    StringBuffer b = new StringBuffer();
    String user = bug.getReporter().getId();
    Date time = bug.getCreationTimestamp();
    String description = bug.getDescription();
    String cc = asString(bug, "cc");
    if (cc.length() > 0) b.append(String.format("<br>***CC***: %s<br>", cc));

    String os = asString(bug, "os");
    if (os.length() > 0) b.append(String.format("<br>***Operating System***: %s<br>", os));

    String pf = asString(bug, "rep_platform");
    if (pf.length() > 0) b.append(String.format("<br>***Platform***: %s<br>", pf));

    String fb = asString(bug, "cf_foundby");
    if (fb.length() > 0) b.append(String.format("<br>***Found by***: %s<br>", fb));

    String url = asString(bug, "url");
    if (url.length() > 0) b.append(String.format("<br>***Found by***: %s<br><br>", url));

    b.append(
        String.format(
            "Originally reported from: **%s** at %s.\n\n%s",
            user,
            Migration.DATEFORMAT.format(time),
            DescriptionFormatter.replaceText(description)));

    return b.toString();
  }

  List<Issue> getAllIssues() throws GitLabApiException {
    if (allIssues == null) {
      allIssues = client.getIssuesApi().getIssues(projectId);
    }
    return allIssues;
  }

  private boolean isClosed(b4j.core.Issue bug) {
    return !bug.getStatus().isOpen();
  }

  private String getIssueTypeLabel(b4j.core.Issue bug) {
    if ("enhancement".equals(bug.getSeverity().getName().toLowerCase())) {
      return Config.getInstance().TARGET_LABEL_ENHANCEMENT;
    } else {
      return Config.getInstance().TARGET_LABEL_BUG;
    }
  }

  private String getIssueStateLabel(b4j.core.Issue bug) {
    return "state: " + bug.getStatus().getName();
  }

  private String getIssuePrioLabel(b4j.core.Issue bug) {
    return "prio: " + bug.getPriority().getName();
  }

  private String getProjectLabel() {
    return Config.getInstance().PROJECT_LABEL;
  }

  private String getLabels(b4j.core.Issue bug) {

    String l =
        String.format(
            "%s,%s,%s,%s,%s,%s,%s,%s",
            getProjectLabel(),
            getIssueTypeLabel(bug),
            getIssueStateLabel(bug),
            getIssueServerityLabel(bug),
            getIssuePrioLabel(bug),
            getVersionLabels(bug),
            getComponentLabels(bug),
            getMilestoneLabel(bug));
    if (bug.isResolved() || bug.isClosed()) {
      l += (",resolution: " + bug.getResolution().getName());
    }
    return l;
  }

  private String getIssueServerityLabel(b4j.core.Issue bug) {
    return "severity: " + bug.getSeverity().getName();
  }

  private String getComponentLabels(b4j.core.Issue bug) {
    StringBuffer b = new StringBuffer();
    for (Iterator<Component> it = bug.getComponents().iterator(); it.hasNext(); ) {
      b.append("comp: ");
      b.append(it.next().getName());
      if (it.hasNext()) b.append(",");
    }
    return b.toString();
  }

  private String getVersionLabels(b4j.core.Issue bug) {
    StringBuffer b = new StringBuffer();

    Collection<Version> c = null;
    if (bug.getAffectedVersionCount() > 0) {
      b.append(versionList("Affected: ", bug.getAffectedVersions()));
    }
    if (bug.getFixVersionCount() > 0) {
      b.append(versionList("Version: ", bug.getFixVersions()));
    }
    if (bug.getPlannedVersionCount() > 0) {
      b.append(versionList("Planned: ", bug.getPlannedVersions()));
    }
    return b.toString();
  }

  private String versionList(String label, Collection<Version> c) {
    StringBuffer b = new StringBuffer();

    for (Iterator<Version> it = c.iterator(); it.hasNext(); ) {
      b.append(label);
      b.append(it.next().getName());
      if (it.hasNext()) b.append(",");
    }
    return b.toString();
  }

  private Object getMilestoneLabel(b4j.core.Issue bug) {

    return "milestone: " + asString(bug, "target_milestone");
    /*


    for (Iterator<Version> it = bug.getFixVersions().iterator(); it.hasNext(); ) {
      b.append("milestone: " + it.next().getName());
      if (it.hasNext()) b.append(",");
    }
    return b.toString();
    */
  }

  public boolean importIssue(b4j.core.Issue bug, Map<String, BugObject> map)
      throws GitLabApiException, IOException {

    TargetIssue ti = IssueFactory.getInstance().newTargetIssue(client, projectId);

    // Skip already imported bugs
    if (ti.isContained(getAllIssues(), bug)) {
      return false;
    }
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

    System.out.println(
        String.format(
            "Bug %s imported as Issue #%s to GitLab project %s.",
            bug.getId(), i.getIid(), projectId));
    BugObject m = getBugModel(bug, discussions, i);
    Logger.instance().writeBug(m);
    map.put(bug.getId(), m);
  }

  BugObject getBugModel(b4j.core.Issue bug, List<Discussion> discussions, Issue i) {
    List<CommentObject> comments = new ArrayList<CommentObject>();
    for (Discussion d : discussions) {
      d.getNotes().stream().forEach(n -> comments.add(new CommentObject(n.getBody())));
    }
    return new BugObject(bug, comments, String.valueOf(i.getIid()));
  }

  private List<Discussion> importComments(TargetIssue ti, b4j.core.Issue bug, Issue i) {
    return ti.importComments(session, bug, i);
  }

  private void closeIfNotOpen(TargetIssue ti, b4j.core.Issue bug, final String labels, Issue i)
      throws GitLabApiException {
    if (isClosed(bug)) {
      ti.closeIssue(bug, labels, i);
    }
  }

  @SuppressWarnings("unchecked")
  private String asString(b4j.core.Issue bug, String propertyName) {
    Object o = bug.get(propertyName);
    Collection<String> names = new ArrayList<String>();
    if (o != null) {
      if (!(o instanceof Collection)) {
        names = new ArrayList<String>();
        if (o instanceof String) names.add((String) o);
      } else {
        names = (Collection<String>) o;
      }
    }
    return String.join(", ", names);
  }

  private Issue importIssue(TargetIssue ti, b4j.core.Issue bug, final String labels)
      throws GitLabApiException {

    String version = asString(bug, "Version");

    return ti.create(bug, labels, getDefaultIssueHeader(bug));
  }

  public void deleteAllIssues() throws GitLabApiException {
    for (Issue i : getAllIssues()) {
      String url = i.getWebUrl();
      IssueFactory.getInstance().newTargetIssue(client, projectId).delete(i.getIid());
      System.out.println(String.format("GitLab issue %s deleted.", url));
    }
    allIssues = null;
  }
}
