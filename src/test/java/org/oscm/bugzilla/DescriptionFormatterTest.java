/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 03.09.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.bugzilla;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** @author goebel */
public class DescriptionFormatterTest {

  @Test
  public void replaceId_duplicate_src() {
    String text = "*** This bug is a duplicate of bug 11200 ***";
    // when
    String out = DescriptionFormatter.replaceText(text);
    System.out.println(out);
    // then
    assertEquals(1, countOccurences(out, "<a href="));
    assertTrue(out.contains("bug 11200"));
  }

  @Test
  public void replaceId_duplicate_target() {
    String text = "*** Bug 11200 has been marked as duplicate of this bug ***";
    // when
    String out = DescriptionFormatter.replaceText(text);
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
    String out = DescriptionFormatter.replaceText(text);
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
    String out = DescriptionFormatter.replaceText(text);
    System.out.println(out);
    // then
    assertEquals(1, countOccurences(out, "<a href="));
    assertTrue(out.contains("bug 13200"));
  }

  @Test
  public void fixBugCodeSections() {
    System.out.println("\n***");
    // given
    String a = "Outside and <br> ``` inside, <br>inside2, <br>inside3 <br> ``` <br>outside<br>";
    String b = "Outside ``` inside <br> ``` ``` inside2<br>```Outside2 ``` inside3 <br> ``` ";
    String c =
        "Outside<br> ```inside <br> inside2<br>inside3<br>end``` ``` inside4 <br> inside5 ```<br> ``not inside`` line<br>text`<br>garbage`<br> ``` this code line<br> line<br>``` ";

    // when
    a = DescriptionFormatter.fixCodeSections(a);
    b = DescriptionFormatter.fixCodeSections(b);
    c = DescriptionFormatter.fixCodeSections(c);

    // then
    System.out.println("A " + a);
    System.out.println("B: " + b);
    System.out.println("C: " + c);

    System.out.println("\n***");

    assertEquals(3, countOccurences(a, "<br>"));
    assertEquals(0, countOccurences(b, "<br>"));
    assertEquals(5, countOccurences(c, "<br>"));
  }

  
  @Test
  public void fixCodeSectionsAdvanced() {
      
      String val = "[Build number]\n" + 
          "              2.0.13\n" + 
          "\n" + 
          "              [How to reproduce]\n" + 
          "              1. Check log agent works well (with active status).\n" + 
          "              2. Stop log agent\n" + 
          "              3. Check status of log agent, `systemctl status monasca-log-agent`\n" + 
          "\n" + 
          "              [Observed result]\n" + 
          "              Status becomes `failed` as below.\n" + 
          "\n" + 
          "              ```\n" + 
          "              # systemctl status monasca-log-agent\n" + 
          "\n" + 
          "              ● monasca-log-agent.service\n" + 
          "                 Loaded: loaded (/etc/systemd/system/monasca-log-agent.service; enabled; vendor preset: disabled)\n" + 
          "                 Active: failed (Result: timeout) since 月 2018-11-05 18:26:02 JST; 2min 0s ago\n" + 
          "               Main PID: 13080\n" + 
          "                 CGroup: /system.slice/monasca-log-agent.service\n" + 
          "\n" + 
          "              11月 05 18:21:05 cmmhost0606.novalocal logstash[13080]: Sending logstash logs to /var/log/monasca-log-agent/...og.\n" + 
          "              11月 05 18:21:06 cmmhost0606.novalocal logstash[13080]: {:timestamp=>\"2018-11-05T18:21:06.927000+0900\", :mes...d\"}\n" + 
          "              11月 05 18:26:02 cmmhost0606.novalocal systemd[1]: Stopping monasca-log-agent.service...\n" + 
          "              11月 05 18:26:02 cmmhost0606.novalocal systemd[1]: monasca-log-agent.service stop-sigterm timed out. Killing.\n" + 
          "              11月 05 18:26:02 cmmhost0606.novalocal systemd[1]: monasca-log-agent.service still around after SIGKILL. Ignoring.\n" + 
          "              11月 05 18:26:02 cmmhost0606.novalocal systemd[1]: monasca-log-agent.service stop-final-sigterm timed out. ...ing.\n" + 
          "              11月 05 18:26:02 cmmhost0606.novalocal systemd[1]: monasca-log-agent.service still around after final SIGKI...ode.\n" + 
          "              11月 05 18:26:02 cmmhost0606.novalocal systemd[1]: Stopped monasca-log-agent.service.\n" + 
          "              11月 05 18:26:02 cmmhost0606.novalocal systemd[1]: Unit monasca-log-agent.service entered failed state.\n" + 
          "              11月 05 18:26:02 cmmhost0606.novalocal systemd[1]: monasca-log-agent.service failed.\n" + 
          "              Hint: Some lines were ellipsized, use -l to show in full.\n" + 
          "              ```\n" + 
          "\n" + 
          "              [Expected result]\n" + 
          "              Log agent is stopped properly (From CMM user POV, I don't know if the agent is stopped properly)\n" + 
          "\n" + 
          "              [Request from MoH SE]\n" + 
          "              1. They would like to know if the agent is stopped properly\n" + 
          "              2. They would like to show `inactive` status when stop the agent";
      
      String result = DescriptionFormatter.replaceText(val);
      System.out.println(result);
  
  }
  
  @Test
  public void replace_link_single() {
    String text = "This is some text\nwhich contains a bugzilla link http://wwwi.est.fujitsu.com/bug01/show_bug.cgi?id=14069 and nothing more.\nEnd.";
    // when
    String out = DescriptionFormatter.replaceLink(text);
    System.out.println(out);
    // then
    assertTrue(out.contains("bug 14069"));
    assertEquals(1, countOccurences(out, "bug 14069"));
  }
  
  @Test
  public void replace_link_multiple() {
    String text = "\nSome text with a bugzilla link http://wwwi.est.fujitsu.com/bug01/show_bug.cgi?id=14069 and\n"+
            "\nanother bugzilla link http://wwwi.est.fujitsu.com/bug01/show_bug.cgi?id=13021 and " +
            "\nyet another bug link http://wwwi.est.fujitsu.com/bug01/show_bug.cgi?id=13023.\n\nAgain http://wwwi.est.fujitsu.com/bug01/show_bug.cgi?id=13023 - but that's all!";
                        
    // when
    String out = DescriptionFormatter.replaceLink(text);
    System.out.println(out);
    // then
    assertTrue(out.contains("bug 14069"));
    assertTrue(out.contains("bug 13023"));
    assertTrue(out.contains("bug 13021"));
     
    assertEquals(1, countOccurences(out, "bug 14069"));
    assertEquals(1, countOccurences(out, "bug 13021"));
    assertEquals(2, countOccurences(out, "bug 13023"));
    
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
