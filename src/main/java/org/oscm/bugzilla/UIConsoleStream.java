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

import java.awt.EventQueue;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** @author goebel */
import javax.swing.JTextArea;

class UIConsoleStream extends OutputStream {
  
  private ConsoleAppender appender;

  UIConsoleStream(JTextArea ta) {
    this(ta, 1000);
  }

  UIConsoleStream(JTextArea ta, int nrLine) {
    if (1 > nrLine) {
      throw new IllegalArgumentException("" + "too low line limit");
    }
    appender = new ConsoleAppender(ta, nrLine);
  }

  public synchronized void close() {
    appender = null;
  }

  public synchronized void write(byte[] ba, int s, int len) {
    if (appender != null) {
      appender.append(b2Str(ba, s, len));
    }
  }

  public synchronized void write(byte[] ab) {
    write(ab, 0, ab.length);
  }

  public synchronized void write(int i) {
    write(new byte[] {(byte) i}, 0, 1);
  }

  public synchronized void clear() {
    if (appender != null) {
      appender.clear();
    }
  }

  private static String b2Str(byte[] ba, int off, int len) {
    try {
      return new String(ba, off, len, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized void flush() {}

  static class ConsoleAppender implements Runnable {
    private int curLn;
    private boolean clr;
    private boolean defer;

    private static final String ls = System.getProperty("line.separator", "\n");
    private final JTextArea txtArea;
    private final int maxLines;
    private final LinkedList<Integer> intLns;
    private final List<String> txt;

    ConsoleAppender(JTextArea ta, int maxlin) {
      txtArea = ta;
      maxLines = maxlin;
      intLns = new LinkedList<Integer>();
      txt = new ArrayList<String>();

      curLn = 0;
      clr = false;
      defer = true;
    }

    @SuppressWarnings("boxing")
    public synchronized void run() {
      if (clr) {
        txtArea.setText("");
      }
      for (String val : txt) {
        curLn += val.length();
        if (val.endsWith("\n") || val.endsWith(ls)) {
          if (intLns.size() >= maxLines) {
            Integer i = intLns.removeFirst();
            txtArea.replaceRange("", 0, i);
          }
          intLns.addLast(curLn);
          curLn = 0;
        }
        txtArea.append(val);
      }
      txt.clear();
      defer = true;
      clr = false;
    }

    synchronized void append(String val) {
      txt.add(val);
      if (defer) {
        defer = false;
        EventQueue.invokeLater(this);
      }
    }

    synchronized void clear() {
      curLn = 0;
      clr = true;
      intLns.clear();
      txt.clear();
      if (defer) {
        defer = false;
        EventQueue.invokeLater(this);
      }
    }
  }
}
