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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author goebel */
public class DescriptionFormatter {

  public static String replaceText(String str) {
    String s = replaceNL(str);
    s = fixCodeSections(s);
    return replaceBugId(s);
  }

  private static String replaceNL(String str) {

    return str.replace("\n", "<br>");
  }

  static String fixCodeSections(String text) {
    Pattern p = Pattern.compile("\\`{3}(.*?)\\`{3}");
    Matcher m = p.matcher(text);
    Map<String, String> map = new HashMap<String, String>();

    while (m.find()) {
      String match = text.substring(m.start(), m.end());
      if (match.contains("<br>")) {
        String rep = match.replaceAll("<br>", "\n\n");
        map.put(match, rep);
      }
    }

    for (String key : map.keySet()) {
      text = text.replace(key, map.get(key));
    }
    return text;
  }

  static String replaceBugId(String str) {
    str = fixDuplicateMsg(str);
    str = replaceLink(str);
    StringBuffer sb = new StringBuffer();
    Pattern p = Pattern.compile("([b|B]ug)\\s([0-9]+)");
    Matcher m = p.matcher(str);
    while (m.find()) {
      String srcBug = m.group(2);
      String query =
          String.format("%s&group_id=&project_id=%s", srcBug, Migration.CONFIG.TARGET_PROJECT_ID);
      query =
          "http://estscm1.intern.est.fujitsu.com/search?utf8=%E2%9C%93&search="
              + query
              + "&scope=issues";
      String link = String.format("<a href=\"%s\">%s %s<a>", query, m.group(1), srcBug);

      str = m.replaceFirst(link); // str.replace(srcBug, link);
      int end = str.indexOf(link) + link.length();
      sb.append(str.substring(0, end));
      str = str.substring(end);
      m = p.matcher(str);
    }
    if (sb.length() > 0) {
      sb.append(str);
      return sb.toString();
    }
    return str;
  }

  static String replaceLink(String str) {
    String baseUrl = Config.getInstance().BUGZILLA_BASEURL;
    StringBuffer sb = new StringBuffer();

    Pattern p = Pattern.compile(Pattern.quote(baseUrl) + "[^=]*=([0-9]+)");
    Matcher m = p.matcher(str);

    while (m.find()) {
      String srcBug = m.group(1);
      str = m.replaceFirst("bug " + srcBug); //
      m = p.matcher(str);
    }
    return str;
  }

  static String fixDuplicateMsg(String str) {
    Pattern p = Pattern.compile("duplicate\\sof\\s([0-9]+)");
    Matcher m = p.matcher(str);
    if (m.find()) {
      String srcBug = m.group(1);
      str = m.replaceFirst("duplicate of bug " + srcBug);
    }
    return str;
  }
}
