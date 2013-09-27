/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.commons.validation;

/**
 * @since 3.10.0-M2
 */
public abstract class AbstractMarkupValidator implements IMarkupValidator {

  //Window Event Attributes
  protected static final String[] WINDOW_EVENT_ATTRIBUTES = new String[]{
      "onafterprint", "onbeforeprint", "onbeforeunload", "onerror", "onhaschange", "onload", "onmessage", "onoffline",
      "ononline", "onpagehide", "onpageshow", "onpopstate", "onredo", "onresize", "onstorage", "onundo", "onunload"
  };

  // Form Events
  protected static final String[] FORM_EVENT_ATTRIBUTES = new String[]{
      "onblur", "onchange", "oncontextmenu", "onfocus", "onformchange", "onforminput", "oninput", "oninvalid", "onreset",
      "onselect", "onsubmit"
  };

  // Keyboard Events
  protected static final String[] KEYBOARD_EVENT_ATTRIBUTES = new String[]{"onkeydown", "onkeypress", "onkeyup"};

  // Mouse Events
  protected static final String[] MOUSE_EVENT_ATTRIBUTES = new String[]{
      "onclick", "ondblclick", "ondrag", "ondragend", "ondragenter", "ondragleave", "ondragover", "ondragstart", "ondrop",
      "onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup", "onmousewheel", "onscroll"
  };

  // Media Events
  protected static final String[] MEDIA_EVENT_ATTRIBUTES = new String[]{
      "onabort", "oncanplay", "oncanplaythrough", "ondurationchange", "onemptied", "onended", "onerror", "onloadeddata",
      "onloadedmetadata", "onloadstart", "onpause", "onplay", "onplaying", "onprogress", "onratechange", "onreadystatechange",
      "onseeked", "onseeking", "onstalled", "onsuspend", "ontimeupdate", "onvolumechange", "onwaiting"
  };
}
