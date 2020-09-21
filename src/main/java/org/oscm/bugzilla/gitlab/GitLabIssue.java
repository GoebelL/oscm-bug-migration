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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.gitlab4j.api.Constants.StateEvent;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.FileUpload;
import org.gitlab4j.api.models.Issue;
import org.gitlab4j.api.models.Note;
import org.oscm.bugzilla.Config;
import org.oscm.bugzilla.Logger;
import org.oscm.bugzilla.Migration;

import b4j.core.Attachment;
import b4j.core.Comment;
import b4j.core.Session;

/** @author goebel */
public class GitLabIssue implements TargetIssue {
  private GitLabApi client;

  private String projectId;
  private int attachmentCnt = 1;

  GitLabIssue(GitLabApi client, String projectId) {
    this.client = client;
    this.projectId = projectId;
  }

  @Override
  public Issue create(b4j.core.Issue bug, final String labels, String header)
      throws GitLabApiException {
    List<Integer> assigneeIds = mapAssigneeId(bug);

    Issue i =
        client
            .getIssuesApi()
            .createIssue(
                projectId,
                getSummary(bug),
                header,
                null,
                assigneeIds,
                null,
                labels,
                bug.getCreationTimestamp(),
                bug.getUpdateTimestamp(),
                null,
                null);
    return i;
  }

  private Map<String, List<Integer>> email2GitLabUserId = new HashMap<String, List<Integer>>(20);

  private List<Integer> mapAssigneeId(String email) {
    List<Integer> id = email2GitLabUserId.get(email);
    if (id == null) {
      try {
        Integer uid = client.getUserApi().getUserByEmail(email).getId();
        id = Arrays.asList(new Integer[] {uid});
        ensureMember(uid);
        email2GitLabUserId.put(email, id);
      } catch (Exception e) {
        String defaultAssignee = Config.getInstance().getDefaultAssigneeEmail().toLowerCase();
        if (email.equals(defaultAssignee)) {
          id = Arrays.asList(new Integer[] {Integer.valueOf(-1)});
          email2GitLabUserId.put(email, id);
        } else {
          id = mapAssigneeId(defaultAssignee);
          email2GitLabUserId.put(email, id);
        }
      }
    }
    if (id.size() != 1) {
      return null;
    }
    return id;
  }

  private void ensureMember(Integer uid) {

    try {
      client.getProjectApi().getMember(projectId, uid);
      return;
    } catch (Exception e) {
      // assume not existing -> HTTP 404
    }
    try {
      client.getProjectApi().addMember(projectId, uid, AccessLevel.GUEST);
    } catch (GitLabApiException e) {
      Logger.logError(e);
    }
  }

  private List<Integer> mapAssigneeId(b4j.core.Issue bug) {
    String email = bug.getAssignee().getName();
    return mapAssigneeId(email);
  }

  @Override
  public void closeIssue(b4j.core.Issue bug, final String labels, Issue i)
      throws GitLabApiException {
    List<Integer> assigneeIds = mapAssigneeId(bug);

    try {

      client
          .getIssuesApi()
          .updateIssue(
              projectId,
              i.getIid(),
              i.getTitle(),
              i.getDescription(),
              i.getConfidential(),
              assigneeIds,
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
  public void assignIssue(b4j.core.Issue bug, Issue i) {
    List<Integer> assigneeIds = mapAssigneeId(bug);

    try {
      client.getIssuesApi().assignIssue(projectId, i.getIid(), assigneeIds.get(0));
    } catch (GitLabApiException e) {
      e.printStackTrace();
    }
  }

  @Override
  public List<Discussion> importComments(Session s, b4j.core.Issue bug, Issue i) {
    final Map<String, Attachment> map = exportAttachments(s, bug);
    attachmentCnt = 1;
    List<Discussion> dis = new ArrayList<Discussion>();
    insertBugAttachemntInfos(map, attachmentCnt, i);
    final String text = bug.getDescription();
    for (Comment c : bug.getComments()) {
      // first comment we get from b4j seems to be equal to the bug description -> skip
      if (text.equals(c.getTheText())) continue;

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
            createAttachmentNote(map, attachmentCnt++, aId, i);
            map.remove(aId);
          }
        }
      } catch (GitLabApiException e) {
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

  public void updateDiscussion(String old, String replace, String id)
      throws NumberFormatException, GitLabApiException {
    Stream<Discussion> i =
        client.getDiscussionsApi().getIssueDiscussionsStream(projectId, Integer.valueOf(id));
    for (Iterator<Discussion> iter = i.iterator(); iter.hasNext(); ) {
      Discussion d = iter.next();
      for (Iterator<Note> i2 = d.getNotes().iterator(); i2.hasNext(); ) {
        Note n = i2.next();
        String body = n.getBody();
        if (body.contains(old)) {
          System.out.println(
              String.format("Replace issue #%s, disusion %s note %s.", id, d.getId(), n.getId()));
          client
              .getDiscussionsApi()
              .modifyIssueThreadNote(projectId, Integer.valueOf(id), d.getId(), n.getId(), replace);
        }
      }
    }
  }

  Note createAttachmentNote(Map<String, Attachment> map, int cnt, String aId, Issue i)
      throws GitLabApiException {
    Attachment a = map.get(aId);
    if (a != null) {
      return client
          .getNotesApi()
          .createIssueNote(
              projectId,
              i.getIid(),
              getAttachmentText(a, cnt),
              (Date) a.get(Attachment.UPDATE_TIMESTAMP));
    }
    return null;
  }

  private String getAttachmentTimestamp(Attachment a) {
    final Object o = a.get(Attachment.UPDATE_TIMESTAMP);
    String updated;
    if (o == null) {
      updated = "undefined";
      System.out.println("Attachment.UPDATE_TIMESTAMP undefined");
    } else {
      if (o instanceof Date) {
        updated = Migration.DATEFORMAT.format((Date) o);
      } else if (o instanceof String) {
        updated = Migration.DATEFORMAT.format((String) o);
      } else {
        updated = "undefined";
      }
    }
    return updated;
  }
  /** @return */
  private String getAttachmentText(Attachment a, int cnt) {
    StringBuffer at = new StringBuffer();
    final String updated = getAttachmentTimestamp(a);
    at.append(
        String.format(
            "**Attachment**: [%s](%s) (%s) - Modified: %s",
            a.getFilename(), a.get("URL"), a.getType(), updated));
    at.append("<br>Description: " + a.getDescription());
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
    InputStream is = null;
    for (Attachment a : ac) {
      try {
        is = s.getAttachment(a);
        final FileUpload uf = copyFile(is, a.getType());
        a.set("URL", uf.getUrl());
        map.put(a.getId(), a);
      } catch (GitLabApiException | IOException e) {
        Logger.logError(e);
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
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
