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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.Issue;
import org.gitlab4j.api.models.Note;

import b4j.core.Comment;

/** @author goebel */
public class GitLabIssueTest implements TargetIssue {
  private static int issueCnt = 1;
  private String projectId;

  public GitLabIssueTest(String projectId) {
    this.projectId = projectId;
  }

  @SuppressWarnings("boxing")
  @Override
  public Issue create(b4j.core.Issue bug, String labels, String header) throws GitLabApiException {
    Issue dummy = new Issue();
    dummy.setIid(issueCnt++);
    dummy.setLabels(Arrays.asList(labels.split(",")));
    dummy.setDescription(header);
    dummy.setTitle(getSummary(bug));
    return dummy;
  }

  @Override
  public void closeIssue(b4j.core.Issue bug, String labels, Issue i) throws GitLabApiException {}

  @Override
  public List<Discussion> importComments(b4j.core.Issue bug, Issue i) {
    List<Discussion> discussions = new ArrayList<Discussion>();
    for (Comment c : bug.getComments()) {
      String author = c.getAuthor().getName();
      String body = c.getTheText();
      Date creation = c.getCreationTimestamp();
      Discussion d = new Discussion();
      Note[] n = new Note[1];
      n[0] = new Note();
      n[0].setBody(createComment(c));
      d.setNotes(Arrays.asList(n));
      discussions.add(d);
    }
    return discussions;
  }
}
