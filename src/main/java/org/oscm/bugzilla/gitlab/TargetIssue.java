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

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.Issue;
import org.oscm.bugzilla.Migration;

import b4j.core.Comment;

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
  public List<Discussion> importComments(b4j.core.Issue bug, Issue i);

  default String createComment(Comment c) {
    StringBuffer sb = new StringBuffer();
    sb.append("Commented by ");
    sb.append("**");
    sb.append(c.getAuthor().getName());
    sb.append("**");
    sb.append(" at ");
    sb.append(Migration.DATEFORMAT.format(c.getUpdateTimestamp()));
    sb.append(":\n");
    sb.append(c.getTheText());
    return sb.toString();
  }

  default String getSummary(b4j.core.Issue b) {
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    sb.append(b.getId());
    sb.append("] ");
    sb.append(b.getSummary());
    return sb.toString();
  }
}
