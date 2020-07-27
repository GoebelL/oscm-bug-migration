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
import java.util.Date;
import java.util.List;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.Constants.StateEvent;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.Issue;
import org.oscm.bugzilla.Logger;

import b4j.core.Comment;

/** @author goebel */
public class GitLabIssue implements TargetIssue {
  private GitLabApi client;

  private String projectId;
  
  GitLabIssue(GitLabApi client, String projectId) {
      this.client = client;
      this.projectId = projectId;
  }
  
  @Override
  public Issue create(b4j.core.Issue bug, final String labels, String header)
      throws GitLabApiException {
    Issue i =
        client
            .getIssuesApi()
            .createIssue(
                projectId,
                getSummary(bug),
                header,
                null,
                null,
                null,
                labels,
                bug.getCreationTimestamp(),
                bug.getUpdateTimestamp(),
                null,
                null);
    return i;
  }
  
  @Override
  public void closeIssue(b4j.core.Issue bug, final String labels, Issue i)
      throws GitLabApiException {

    try {
      client
          .getIssuesApi()
          .updateIssue(
              projectId,
              i.getIid(),
              i.getTitle(),
              i.getDescription(),
              i.getConfidential(),
              null,
              null,
              labels,
              StateEvent.CLOSE,
              bug.getUpdateTimestamp(),
              new Date());
    } catch (GitLabApiException e) { // TODO Auto-generated catch block
      Logger.logError(e);
    }
  }

  @Override
  public List<Discussion> importComments(b4j.core.Issue bug, Issue i) {
    List<Discussion> discussions = new ArrayList<Discussion>();
    for (Comment c : bug.getComments()) {
      String author = c.getAuthor().getName();
      String body = createComment(c);
      Date creation = c.getCreationTimestamp();
      importAttachments(c, i);
      try {
        Discussion d  = client.getDiscussionsApi().createIssueDiscussion(projectId, i.getIid(), body, creation);
        discussions.add(d);
      } catch (GitLabApiException e) { // TODO Auto-generated catch block
        Logger.logError(e);
      }
    }
    return discussions;
  }

  public void importAttachments(Comment c, Issue i) {
    /*
          TODO Implement a fault-tolerant attachments import
          try {
              // c.getAttachments() ...
              // client.getImportExportApi() ...
          } catch (GitLabApiException e) { 
              Logger.logError(e);
          }
    */
  }
}
