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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gitlab4j.api.Constants.StateEvent;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.FileUpload;
import org.gitlab4j.api.models.Issue;
import org.oscm.bugzilla.Logger;
import org.oscm.bugzilla.Migration;

import b4j.core.Attachment;
import b4j.core.Comment;
import b4j.core.Session;

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
    } catch (GitLabApiException e) {
      Logger.logError(e);
    }
  }

  @Override
  public List<Discussion> importComments(Session s, b4j.core.Issue bug, Issue i) {
    final Map<String, Attachment> map = exportAttachments(s, bug);
    int cnt = 1;
    insertBugAttachemntInfos(map, cnt, i);
    List<Discussion> dis = new ArrayList<Discussion>();
    for (Comment c : bug.getComments()) {
      String author = c.getAuthor().getName();
      String body = createComment(c);
      Date creation = c.getCreationTimestamp();
      try {
        Discussion d =
            client.getDiscussionsApi().createIssueDiscussion(projectId, i.getIid(), body, creation);
        dis.add(d);

        for (String aId : c.getAttachments()) {
          Attachment a = map.get(aId);
          if (a != null) {
            createAttachmentNote(map, cnt, aId, i);
          }
        }
      } catch (GitLabApiException e) { // TODO Auto-generated catch block
        Logger.logError(e);
      }
    }    
    return dis;
  }

  private void insertBugAttachemntInfos(Map<String, Attachment> map, int cnt, Issue i) {
    if (!map.isEmpty()) {
     for (String key : map.keySet()) {
        try {
          createAttachmentNote(map, cnt++, key, i);
        } catch (GitLabApiException e) {
          Logger.logError(e);
        }
      }
    }
  }

  void createAttachmentNote(Map<String, Attachment> map, int cnt, String aId, Issue i)
      throws GitLabApiException {
    Attachment a = map.get(aId);
    if (a != null) {
      client.getNotesApi().createIssueNote(projectId, i.getIid(), getAttachmentText(a, cnt));
    }
  }

  /** @return */
  private String getAttachmentText(Attachment a, int cnt) {
    StringBuffer at = new StringBuffer();
    at.append(
        String.format(
            "**Attachment %s**: [%s](%s) Type: %s, Created %s \n",
            String.valueOf(cnt),
            a.getFilename(),
            a.get("URL"),
            a.getType(),
            Migration.DATEFORMAT.format(a.getDate())));
    at.append(a.getDescription());
    at.append("\n");
    return at.toString();
  }

  FileUpload copyFile(InputStream i, String type) throws IOException, GitLabApiException {

    File temp = null;
    try (InputStream is = i) {
      temp = File.createTempFile("gitbugzilla_", "");
      temp.deleteOnExit();
      Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    FileUpload fileUpload = client.getProjectApi().uploadFile(projectId, temp, type);
    return fileUpload;
  }

  @Override
  public Map<String, Attachment> exportAttachments(Session s, b4j.core.Issue bug) {
    HashMap<String, Attachment> map = new HashMap<String, Attachment>();
    final Collection<Attachment> ac = bug.getAttachments();
    for (Attachment a : ac) {
      try (InputStream is = s.getAttachment(a)) {
        final FileUpload uf = copyFile(is, a.getType());
        a.set("URL", uf.getUrl());
        map.put(a.getId(), a);
      } catch (GitLabApiException | IOException e) {
        Logger.logError(e);
      }
    }
    return map;
  }

  /** @see org.oscm.bugzilla.gitlab.TargetIssue#delete(java.lang.Integer) */
  @Override
  public void delete(Integer issueId) throws GitLabApiException {
    client.getIssuesApi().deleteIssue(projectId, issueId);
  }
}
