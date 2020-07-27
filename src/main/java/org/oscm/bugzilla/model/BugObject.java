/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 22.07.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.bugzilla.model;

import java.util.List;

/** @author goebel */
public class BugObject {
  private String bugId;
  private String priority;
  private String serverity;
  private String reporter;
  private String assignee;
  private String creation;
  private String issueId;

  private List<CommentObject> comments;

  /** @return the comments */
  public List<CommentObject> getComments() {
    return comments;
  }

  /** @param comments the comments to set */
  public void setComments(List<CommentObject> comments) {
    this.comments = comments;
  }

  /** @return the bugId */
  public String getBugId() {
    return bugId;
  }

  /** @param bugId the bugId to set */
  public void setBugId(String bugId) {
    this.bugId = bugId;
  }

  /** @return the assignee */
  public String getAssignee() {
    return assignee;
  }

  /** @param assignee the assignee to set */
  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }

  /** @return the creation */
  public String getCreation() {
    return creation;
  }

  /** @param creation the creation to set */
  public void setCreation(String creation) {
    this.creation = creation;
  }

  /** @return the summary */
  public String getSummary() {
    return summary;
  }

  /** @param summary the summary to set */
  public void setSummary(String summary) {
    this.summary = summary;
  }

  private String summary;
  /** @return the issueId */
  public String getIssueId() {
    return issueId;
  }

  /** @param issueId the issueId to set */
  public void setIssueId(String issueId) {
    this.issueId = issueId;
  }

  /** @return the priority */
  public String getPriority() {
    return priority;
  }

  /** @param priority the priority to set */
  public void setPriority(String priority) {
    this.priority = priority;
  }

  /** @return the serverity */
  public String getServerity() {
    return serverity;
  }

  /** @param serverity the serverity to set */
  public void setServerity(String serverity) {
    this.serverity = serverity;
  }

  /** @return the reporter */
  public String getReporter() {
    return reporter;
  }

  /** @param reporter the reporter to set */
  public void setReporter(String reporter) {
    this.reporter = reporter;
  }

  public BugObject(b4j.core.Issue bug, List<CommentObject> comments, String issueId) {
    this.issueId = issueId;
    this.bugId = bug.getId();
    this.priority = bug.getPriority().getName();
    this.serverity = bug.getSeverity().getName();
    this.reporter = bug.getReporter().getName();
    this.assignee = bug.getAssignee().getName();
    this.summary = bug.getSummary();
    this.comments = comments;
  }

  public BugObject() {
      super();
  }
}
