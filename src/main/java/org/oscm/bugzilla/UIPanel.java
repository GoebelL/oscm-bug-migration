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

import java.awt.Color;
import java.awt.Font;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

class Console {
  final JFrame frame = new JFrame();
  JTextArea textField;

  public void init() {
    frame.pack();
    frame.setVisible(true);
  }

  public Console() {
    textField = new JTextArea(40, 140);
    textField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
    
    textField.setForeground(Color.LIGHT_GRAY);
    textField.setBackground(Color.BLACK);
    textField.setAutoscrolls(true);

    PrintStream con = new PrintStream(new UIConsoleStream(textField));
    System.setOut(con);
    System.setErr(con);
    frame.add(textField);

    JScrollPane sp = new JScrollPane(textField);
    sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    frame.getContentPane().add(sp);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
  }

  public JFrame getFrame() {
    return frame;
  }

  JTextArea getTextArea() {
    return textField;
  }
}

public class UIPanel extends JFrame {
  /** */
  private static final long serialVersionUID = 5876984861586231810L;

  static Console con;

  public static Console popUp() {
    con = new Console();
    con.init();
    UIPanel launcher = new UIPanel();
    launcher.setVisible(true);
    con.getFrame()
        .setLocation(
            launcher.getX() + launcher.getWidth() + launcher.getInsets().right, launcher.getY());
    con.frame.setTitle("Bugzilla Migration");
    return con;
  }

  private UIPanel() {
    super();
    setSize(50, 40);
    setTitle("Launcher");
    setResizable(false);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
  }

  public void dispose() {
    super.dispose();
    System.exit(0);
  }
}
