/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 24.07.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.bugzilla;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

/** @author goebel */
public class Credentials {

  static String ask(String user) {
    final JPasswordField jpf = new JPasswordField();
    JOptionPane jop =
        new JOptionPane(jpf, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
    JDialog dialog = jop.createDialog(String.format("Password for %s:", user));

    dialog.addComponentListener(
        new ComponentListener() {

          @Override
          public void componentShown(ComponentEvent e) {
            SwingUtilities.invokeLater(
                new Runnable() {
                  @Override
                  public void run() {
                    jpf.requestFocusInWindow();
                  }
                });
          }

          @Override
          public void componentResized(ComponentEvent e) {}

          @Override
          public void componentMoved(ComponentEvent e) {}

          @Override
          public void componentHidden(ComponentEvent e) {}
        });
    dialog.setVisible(true);
    if (jop.getValue() != null) {
      int result = ((Integer) (jop.getValue())).intValue();
      dialog.dispose();
      char[] pw = null;
      if (result == JOptionPane.OK_OPTION) {
        return new String(jpf.getPassword());
      }
    }
    return null;
  }
}
