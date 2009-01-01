/*
 * 12/21/2008
 *
 * AutoCompleteDescWindow.java - A window containing a description of the
 * currently selected completion.
 * Copyright (C) 2008 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com/rsyntaxtextarea
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA.
 */
package org.fife.ui.autocomplete;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;


/**
 * The optional "description" window that describes the currently selected
 * item in the auto-completion window.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class AutoCompleteDescWindow extends JWindow implements HyperlinkListener {

	/**
	 * The parent AutoCompletion instance.
	 */
	private AutoCompletion ac;

	/**
	 * Renders the HTML description.
	 */
	private JEditorPane descArea;

	/**
	 * The toolbar with "back" and "forward" buttons.
	 */
	private JToolBar descWindowNavBar;

	/**
	 * Action that goes to the previous description displayed.
	 */
	private Action backAction;

	/**
	 * Action that goes to the next description displayed.
	 */
	private Action forwardAction;

	/**
	 * History of descriptions displayed.
	 */
	private List history;

	/**
	 * The current position in {@link #history}.
	 */
	private int historyPos;

	/**
	 * Used on OS X, where non-editable JEditorPanes don't have their cursors
	 * made into hand cursors on hyperlink mouseover.
	 */
	private Cursor prevCursor;

	/**
	 * The resource bundle for this window.
	 */
	private ResourceBundle bundle;

	/**
	 * The resource bundle name.
	 */
	private static final String MSG =
					"org.fife.ui.autocomplete.AutoCompleteDescWindow";

	private static final boolean IS_OS_X = System.getProperty("os.name").
													indexOf("OS X")>-1;


	/**
	 * Constructor.
	 *
	 * @param owner The parent window.
	 * @param ac The parent autocompletion.
	 */
	public AutoCompleteDescWindow(Window owner, AutoCompletion ac) {

		super(owner);
		this.ac = ac;

		JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		descArea = createDescArea();
		JScrollPane sp = new JScrollPane(descArea);
		sp.setViewportBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		sp.setBackground(descArea.getBackground());
		sp.getViewport().setBackground(descArea.getBackground());
		cp.add(sp);

		descWindowNavBar = new JToolBar();
		backAction = new ToolBarBackAction();
		forwardAction = new ToolBarForwardAction();
		descWindowNavBar.setFloatable(false);
		descWindowNavBar.add(new JButton(backAction));
		descWindowNavBar.add(new JButton(forwardAction));

		JPanel temp = new JPanel(new BorderLayout());
		SizeGrip rp = new SizeGrip();
		temp.add(descWindowNavBar, BorderLayout.LINE_START);
		temp.add(rp, BorderLayout.LINE_END);
		cp.add(temp, BorderLayout.SOUTH);

		setContentPane(cp);

		setFocusableWindowState(false);

		history = new ArrayList(1); // Usually small
		historyPos = -1;

	}


	/**
	 * Sets the currently displayed description and updates the history.
	 *
	 * @param html The new description.
	 */
	private void addToHistory(String html) {
		history.add(++historyPos, html);
		clearHistoryAfterCurrentPos();
		setActionStates();
	}


	/**
	 * Clears the history of viewed descriptions.
	 */
	private void clearHistory() {
		history.clear(); // Try to free some memory.
		historyPos = -1;
		if (descWindowNavBar!=null) {
			setActionStates();
		}
	}


	/**
	 * Makes the current history page the last one in the history.
	 */
	private void clearHistoryAfterCurrentPos() {
		for (int i=history.size()-1; i>historyPos; i--) {
			history.remove(i);
		}
		setActionStates();
	}


	/**
	 * Creates the customized JEditorPane used to render HTML documentation.
	 *
	 * @return The JEditorPane.
	 */
	private JEditorPane createDescArea() {

		JEditorPane descArea = new JEditorPane("text/html", null);

		// Jump through a few hoops to get things looking nice in Nimbus
		if (UIManager.getLookAndFeel().getName().equals("Nimbus")) {
			System.out.println("DEBUG: Creating Nimbus-specific changes");
			Color selBG = descArea.getSelectionColor();
			Color selFG = descArea.getSelectedTextColor();
			descArea.setUI(new javax.swing.plaf.basic.BasicEditorPaneUI());
			descArea.setSelectedTextColor(selFG);
			descArea.setSelectionColor(selBG);
		}

		descArea.getCaret().setSelectionVisible(true);
		descArea.setEditable(false);
		descArea.addHyperlinkListener(this);

		// Make it use "tooltip" background color.
		descArea.setBackground(getDefaultBackground());

		// Force JEditorPane to use a certain font even in HTML.
		Font font = UIManager.getFont("Label.font");
		HTMLDocument doc = (HTMLDocument)descArea.getDocument();
		doc.getStyleSheet().addRule("body { font-family: " + font.getFamily() +
				"; font-size: " + font.getSize() + "pt; }");

		return descArea;

	}


	/**
	 * Returns the default background color to use for the description
	 * window.
	 *
	 * @return The default background color.
	 */
	protected Color getDefaultBackground() {
		System.out.println(UIManager.getColor("info"));
		Color c = UIManager.getColor("ToolTip.background");
		if (c==null) { // Some LookAndFeels like Nimbus
			c = UIManager.getColor("info"); // Used by Nimbus (and others)
			if (c==null) {
				c = SystemColor.infoText; // System default
			}
		}
		return c;
	}


	/**
	 * Returns the localized message for the specified key.
	 *
	 * @param key The key.
	 * @return The localized message.
	 */
	private String getString(String key) {
		if (bundle==null) {
			bundle = ResourceBundle.getBundle(MSG);
		}
		return bundle.getString(key);
	}


	/**
	 * Called when a hyperlink is clicked.
	 *
	 * @param e The event.
	 */
	public void hyperlinkUpdate(HyperlinkEvent e) {
System.out.println(descArea.isEnabled() + ", " + e);
		HyperlinkEvent.EventType type = e.getEventType();

		if (type.equals(HyperlinkEvent.EventType.ACTIVATED)) {
			URL url = e.getURL();
			if (url!=null) {
				ExternalURLHandler handler = ac.getExternalURLHandler();
				if (handler!=null) {
					handler.urlClicked(url);
					return;
				}
				// No handler - try loading in external browser (Java 6+ only).
				try {
					Util.browse(new URI(url.toString()));
					//descArea.setText(null);
					//descArea.read(url.openStream(), descArea.getDocument());
					////descArea.setPage(url);
					////descArea.setCaretPosition(0); // In case we scrolled
					////addToHistory(descArea.getText());
				} catch (/*IO*/URISyntaxException ioe) {
					UIManager.getLookAndFeel().provideErrorFeedback(descArea);
					ioe.printStackTrace();
//					try {
//						descArea.read(url.openStream(), null);
//					} catch (IOException ioe2) {
//						ioe2.printStackTrace();
//						UIManager.getLookAndFeel().provideErrorFeedback(descArea);
//					}
				}
			}
			else { // Simple function name text, like in c.xml
				// FIXME: This is really a hack, and we assume we can find the
				// linked-to item in the same CompletionProvider.
				AutoCompletePopupWindow parent =
								(AutoCompletePopupWindow)getParent();
				CompletionProvider p = parent.getSelection().getProvider();
				if (p instanceof AbstractCompletionProvider) {
					String name = e.getDescription();
					Completion c = ((AbstractCompletionProvider)p).
										getCompletionByInputText(name);
					setDescriptionFor(c, true);
				}
			}
		}

		// OS X needs a little push to use the hand cursor for links in a
		// non-editable JEditorPane
		else if (IS_OS_X) {
			boolean entered = HyperlinkEvent.EventType.ENTERED.equals(type);
			if (entered) {
				prevCursor = descArea.getCursor();
				descArea.setCursor(Cursor.getPredefinedCursor(
											Cursor.HAND_CURSOR));
			}
			else {
				descArea.setCursor(prevCursor);
			}
		}

	}


	/**
	 * Enables or disables the back and forward actions as appropriate.
	 */
	private void setActionStates() {
		backAction.setEnabled(historyPos>0);
		forwardAction.setEnabled(historyPos>-1 && historyPos<history.size()-1);
	}


	/**
	 * Sets the description displayed in this window.  This clears the
	 * history.
	 *
	 * @param item The item whose description you want to display.
	 */
	public void setDescriptionFor(Completion item) {
		setDescriptionFor(item, false);
	}


	/**
	 * Sets the description displayed in this window.
	 *
	 * @param item The item whose description you want to display.
	 * @param addToHistory Whether to add this page to the page history
	 *        (as opposed to clearing it and starting anew).
	 */
	protected void setDescriptionFor(Completion item, boolean addToHistory) {
		String desc = item==null ? null : item.getSummary();
		if (desc==null) {
			desc = "<html><em>" + getString("NoDescAvailable") + "</em>";
		}
		descArea.setText(desc);
		descArea.setCaretPosition(0); // In case of scrolling
		if (!addToHistory) {
			// Remove everything first if this is going to be the only
			// thing in history.
			clearHistory();
		}
		addToHistory(desc);
	}


	/**
	 * {@inheritDoc} 
	 */
	public void setVisible(boolean visible) {
		if (!visible) {
			clearHistory();
		}
		super.setVisible(visible);
	}


	/**
	 * Action that moves to the previous description displayed.
	 */
	class ToolBarBackAction extends AbstractAction {

		public ToolBarBackAction() {
			ClassLoader cl = getClass().getClassLoader();
			URL url = cl.getResource("org/fife/ui/autocomplete/arrow_left.png");
			try {
				Icon icon = new ImageIcon(ImageIO.read(url));
				putValue(Action.SMALL_ICON, icon);
			} catch (IOException ioe) { // Never happens
				ioe.printStackTrace();
				putValue(Action.SHORT_DESCRIPTION, "Back");
			}
		}

		public void actionPerformed(ActionEvent e) {
			if (historyPos>0) {
				descArea.setText((String)history.get(--historyPos));
				descArea.setCaretPosition(0);
				setActionStates();
			}
		}

	}


	/**
	 * Action that moves to the previous description displayed.
	 */
	class ToolBarForwardAction extends AbstractAction {

		public ToolBarForwardAction() {
			ClassLoader cl = getClass().getClassLoader();
			URL url = cl.getResource("org/fife/ui/autocomplete/arrow_right.png");
			try {
				Icon icon = new ImageIcon(ImageIO.read(url));
				putValue(Action.SMALL_ICON, icon);
			} catch (IOException ioe) { // Never happens
				ioe.printStackTrace();
				putValue(Action.SHORT_DESCRIPTION, "Forward");
			}
		}

		public void actionPerformed(ActionEvent e) {
			if (history!=null && historyPos<history.size()-1) {
				descArea.setText((String)history.get(++historyPos));
				descArea.setCaretPosition(0);
				setActionStates();
			}
		}

	}


}