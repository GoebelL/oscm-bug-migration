/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 23.07.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.bugzilla.gitlab;

import java.util.List;
import java.util.Map;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.Issue;
import org.oscm.bugzilla.DescriptionFormatter;
import org.oscm.bugzilla.Migration;

import b4j.core.Attachment;
import b4j.core.Comment;
import b4j.core.Session;

/** @author goebel */
public interface TargetIssue {

  /**
   * Creates an issue in given GitLab project from given Bugzilla bug data.
   *
   * @param bug - the Bugzilla bug to create the isse from
   * @param labels - a comma-separated list of GitHub labels
   * @param header - an issue description including header with imported information.
   * @return the created issue
   * @throws GitLabApiException - if an error occurred
   */
  public Issue create(b4j.core.Issue bug, final String labels, String header)
      throws GitLabApiException;

  /**
   * Closes an issue with given label and properties denoted by given GitLab Issue.
   *
   * @param bug - the Bugzilla bug to create the isse from
   * @param labels - a comma-separated list of GitHub labels
   * @param header - an issue description including header with imported information.
   * @return the created issue
   * @throws GitLabApiException - if an error occurred
   */
  public void closeIssue(b4j.core.Issue bug, final String labels, Issue i)
      throws GitLabApiException;

  /** Imports all comments of the given issue. */
  public List<Discussion> importComments(Session s, b4j.core.Issue bug, Issue i);

  /** Imports all bug attachments of the given issue. */
  public Map<String, Attachment> exportAttachments(Session s, b4j.core.Issue bug);

  /** Create an issue comment text from the given bug comment. */
  default String createComment(Comment c) {
    StringBuffer sb = new StringBuffer();
    sb.append("Commented by ");
    sb.append("**");
    sb.append(c.getAuthor().getName());
    sb.append("**");
    sb.append(" at ");
    sb.append(Migration.DATEFORMAT.format(c.getUpdateTimestamp()));
    sb.append(":\n\n");
    sb.append(DescriptionFormatter.replaceText(c.getTheText()));
    return sb.toString();
  }

  /** Create an issue title from the given bug summary. */
  default String getSummary(b4j.core.Issue b) {
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    sb.append(b.getId());
    sb.append("] ");
    sb.append(b.getSummary());
    return sb.toString();
  }

  public void updateDiscussion(String old, String replace, String id)
      throws NumberFormatException, GitLabApiException;
 
  public void assignIssue(b4j.core.Issue bug, Issue i);
  
  /** Delete the issue with the given Id. */
  public void delete(Integer issueId) throws GitLabApiException;

  public default boolean isContained(List<Issue> issues, b4j.core.Issue bug) {
    for (Issue i : issues) {
      if (i.getTitle().contains(bug.getSummary())) {
        return true;
      }
    }
    return false;
  }
}
