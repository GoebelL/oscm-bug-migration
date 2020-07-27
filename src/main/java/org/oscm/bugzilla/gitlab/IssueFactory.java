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

import org.gitlab4j.api.GitLabApi;
import org.oscm.bugzilla.Config;

/** @author goebel */
public class IssueFactory {
  private boolean testRun = true;

  private IssueFactory() {}

  private static boolean isTestRun() {
    return !Config.getInstance().isProductiveRun();
  }

  private IssueFactory(boolean test) {
    this.testRun = test;
  }

  public TargetIssue newTargetIssue(GitLabApi cient, String projectId) {
    if (testRun) {
      return new GitLabIssueTest(projectId);
    }
    return new GitLabIssue(cient, projectId);
  }

  public static IssueFactory getInstance() {
    if (instance == null) {
      instance = new IssueFactory(isTestRun());
    }
    return instance;
  }

  static IssueFactory instance;
}
