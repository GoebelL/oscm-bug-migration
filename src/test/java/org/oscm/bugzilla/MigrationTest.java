/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 31.08.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.bugzilla;

import static org.junit.Assert.assertNotNull;

/** @author goebel */
public class MigrationTest {

  public void testConfig_BugzillaUrl() {
    assertNotNull(Config.getInstance().BUGZILLA_BASEURL);
  }

  public void testConfig_GitLabUrl() {
    assertNotNull(Config.getInstance().GITLAB_BASEURL);
  }

  private int countOccurences(String str, String sub) {
    int last = 0;
    int cnt = 0;
    while (last != -1) {
      last = str.indexOf(sub, last);
      if (last != -1) {
        last += sub.length();
        cnt++;
      }
    }
    return cnt;
  }
}
