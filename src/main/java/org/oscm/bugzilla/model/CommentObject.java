/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 23.07.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.bugzilla.model;

/** @author goebel */
public class CommentObject {

  public CommentObject() {
    super();
  }

  public CommentObject(String comment) {
    description = comment;
  }

  private String description;

  /** @return the description */
  public String getDescription() {
    return description;
  }

  /** @param description the description to set */
  public void setDescription(String description) {
    this.description = description;
  }
}
