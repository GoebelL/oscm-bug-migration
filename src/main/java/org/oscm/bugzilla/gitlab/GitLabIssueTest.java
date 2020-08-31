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
import java.util.List;
import java.util.Map;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.FileUpload;
import org.gitlab4j.api.models.Issue;
import org.gitlab4j.api.models.Note;
import org.oscm.bugzilla.Logger;
import org.oscm.bugzilla.Migration;

import b4j.core.Attachment;
import b4j.core.Comment;
import b4j.core.Session;

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
  public List<Discussion> importComments(Session s, b4j.core.Issue bug, Issue i) {
    final Map<String, Attachment> map = exportAttachments(s, bug);
    int cnt = 1;
    List<Discussion> discussions = new ArrayList<Discussion>();
    for (Comment c : bug.getComments()) {
      String author = c.getAuthor().getName();
      String body = c.getTheText();
      Date creation = c.getCreationTimestamp();
      Discussion d = new Discussion();
      Note[] n = new Note[1];
      n[0] = new Note();
      String cm = createComment(c);
      for (String aId : c.getAttachments()) {
        cm = appendCommentAttachmentInfos(map, cm, cnt, aId);
      }
      n[0].setBody(cm);
      d.setNotes(Arrays.asList(n));
      discussions.add(d);
    }
    insertBugAttachemntInfos(discussions, map, cnt);

    return discussions;
  }

  private void insertBugAttachemntInfos(List<Discussion> dl, Map<String, Attachment> map, int cnt) {
    if (!map.isEmpty()) {
      Discussion d = new Discussion();
      Note[] n = new Note[1];
      n[0] = new Note();
      String cm = "List of Attachements:\n";
      cm = appendBugAttachmentInfos(map, cnt, cm);
      n[0].setBody(cm);
      d.setNotes(Arrays.asList(n));
      dl.add(0, d);
    }
  }

  private String appendBugAttachmentInfos(
      Map<String, Attachment> map, int cnt, String cm) { // TODO Auto-generated method stub
    for (String key : map.keySet()) {
      cm = appendCommentAttachmentInfos(map, cm, cnt, key);
    }
    return cm;
  }

  String appendCommentAttachmentInfos(Map<String, Attachment> map, String cm, int cnt, String aId) {
    Attachment a = map.get(aId);
    if (a != null) {
      String att =
          String.format(
              "Attachement URL: %s, Type: %s, created %s",
              a.get("URL"), a.getType(), Migration.DATEFORMAT.format(a.getDate()));
      cm = cm + "\n" + att;
    }
    map.remove(aId);
    return cm;
  }

  @Override
  public Map<String, Attachment> exportAttachments(Session s, b4j.core.Issue bug) {
    HashMap<String, Attachment> map = new HashMap<String, Attachment>();
    final Collection<Attachment> ac = bug.getAttachments();
    for (Attachment a : ac) {
      try (InputStream is = s.getAttachment(a)) {
        final FileUpload uf = copyFile(is, a.getType());
        a.set("URL", uf.getUrl());
        a.set("SIZE", uf.getAlt());
        map.put(a.getId(), a);
      } catch (GitLabApiException | IOException e) {
        Logger.logError(e);
      }
    }
    return map;
  }

  FileUpload copyFile(InputStream i, String type) throws IOException, GitLabApiException {

    File temp = null;
    long size = 0;
    try (InputStream is = i) {
      temp = File.createTempFile("gitbugzilla_", "");
      temp.deleteOnExit();
      size = Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    FileUpload fileUpload = new FileUpload();
    fileUpload.setUrl("anygiturl");
    fileUpload.setAlt("Filesize=" + size + " byte.");
    return fileUpload;
  }

  public void updateDiscussion(String old, String replace, String id)
      throws NumberFormatException, GitLabApiException {}

  /** @see org.oscm.bugzilla.gitlab.TargetIssue#delete(java.lang.Integer) */
  @Override
  public void delete(Integer issueId) throws GitLabApiException {}
}
