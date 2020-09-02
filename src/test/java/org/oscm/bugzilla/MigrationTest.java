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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** @author goebel */
public class MigrationTest {

  @Test
  public void replaceId_duplicate_src() {
    String text = "*** This bug is a duplicate of bug 11200 ***";
    // when
    String out = Migration.replaceText(text);
    System.out.println(out);
    // then
    assertEquals(1, countOccurences(out, "<a href="));
    assertTrue(out.contains("bug 11200"));
  }

  @Test
  public void replaceId_duplicate_target() {
    String text = "*** Bug 11200 has been marked as duplicate of this bug ***";
    // when
    String out = Migration.replaceText(text);
    System.out.println(out);
    // then
    assertEquals(1, countOccurences(out, "<a href="));
    assertTrue(out.contains("Bug 11200"));
  }

  @Test
  public void replaceId_duplicate_extended() {
    String text =
        "This is similar to bug 13408 but even equal to 11200.\n. *** Bug 11200 has been marked as duplicate of this bug ***";
    // when
    String out = Migration.replaceText(text);
    System.out.println(out);
    // then
    assertEquals(2, countOccurences(out, "<a href="));
    assertTrue(out.contains("Bug 11200"));
    assertTrue(out.contains("bug 13408"));
  }  

  @Test
  public void replaceId_fixDuplicateMsg() {
    String text = "*** This bug is a duplicate of 13200 ***";
        // when
    String out = Migration.replaceText(text);
    System.out.println(out);
    // then
    assertEquals(1, countOccurences(out, "<a href="));
    assertTrue(out.contains("bug 13200"));
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
