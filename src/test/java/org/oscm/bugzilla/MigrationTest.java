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

import static org.junit.Assert.assertTrue;

import org.junit.Test;



/** @author goebel */
public class MigrationTest {

  @Test
  public void replaceId() {
    String text = "*** This bug is a duplicate of bug 11200 ***";
    // when
    String out = Migration.replaceText(text);
    System.out.println(out);
    // then
    assertTrue(out.contains("<a href="));
    assertTrue(out.contains("bug 11200"));
  }
}
