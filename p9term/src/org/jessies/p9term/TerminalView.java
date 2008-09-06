package org.jessies.p9term;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class TerminalView extends JComponent implements Scrollable {
    private boolean hasFocus = false;
    private boolean displayCursor = true;
    
    public TerminalView() {
        ComponentUtilities.disableFocusTraversal(this);
        setBorder(BorderFactory.createEmptyBorder(1, 4, 4, 4));
        setOpaque(true);
        optionsDidChange();
        becomeDropTarget();
    }
    
    public void optionsDidChange() {
        P9TermPreferences preferences = P9Term.getPreferences();
        if (preferences.getBoolean(P9TermPreferences.USE_ALT_AS_META)) {
            // If we want to handle key events when alt is down, we need to turn off input methods.
            enableInputMethods(false);
        }
        
        setBackground(preferences.getColor("background"));
        setForeground(preferences.getColor("foreground"));
        
        setFont(preferences.getFont(P9TermPreferences.FONT));
    }
    
    public void userIsTyping() {
        if (P9Term.getPreferences().getBoolean(P9TermPreferences.HIDE_MOUSE_WHEN_TYPING)) {
            setCursor(GuiUtilities.INVISIBLE_CURSOR);
        }
    }
    
    private void becomeDropTarget() {
        new TerminalDropTarget(this);
    }
    
    /**
     * Pastes the text on the clipboard into the terminal.
     */
    public void pasteSystemClipboard() {
        pasteClipboard(getToolkit().getSystemClipboard());
    }
    
    /**
     * Pastes the system selection, generally only available on X11.
     */
    public void pasteSystemSelection() {
        Clipboard systemSelection = getToolkit().getSystemSelection();
        if (systemSelection != null) {
            pasteClipboard(systemSelection);
        }
    }
    
    /**
     * Pastes the system selection on X11, the clipboard on Windows
     * and nothing on Mac OS X.
     */
    public void middleButtonPaste() {
        if (GuiUtilities.isWindows()) {
            pasteSystemClipboard();
        } else {
            pasteSystemSelection();
        }
    }
        
    private void pasteClipboard(Clipboard clipboard) {
        try {
            Transferable contents = clipboard.getContents(this);
            String string = (String) contents.getTransferData(DataFlavor.stringFlavor);
            terminalControl.sendUtf8String(string);
        } catch (Exception ex) {
            Log.warn("Couldn't paste.", ex);
        }
    }
    
    private TerminalControl terminalControl;
    
    public TerminalControl getTerminalControl() {
        return terminalControl;
    }
    
    public void setTerminalControl(TerminalControl terminalControl) {
        this.terminalControl = terminalControl;
    }
    
    /** Returns our visible size. */
    public Dimension getVisibleSize() {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
        return scrollPane.getViewport().getExtentSize();
    }
    
    /**
     * Returns the dimensions of an average character. Note that even though
     * we use a fixed-width font, some glyphs for non-ASCII characters can
     * be wider than this. See Markus Kuhn's UTF-8-demo.txt for examples,
     * particularly among the Greek (where some glyphs are normal-width
     * and others are wider) and Japanese (where most glyphs are wide).
     * 
     * This isn't exactly deprecated, but you should really think hard
     * before using it.
     */
    public Dimension getCharUnitSize() {
        FontMetrics metrics = getFontMetrics(getFont());
        int width = metrics.charWidth('W');
        int height = metrics.getHeight();
        // Avoid divide by zero errors, so the user gets a chance to change their font.
        if (width == 0) {
            Log.warn("Insane font width for " + getFont());
            width = 1;
        }
        if (height == 0) {
            Log.warn("Insane font height for " + getFont());
            height = 1;
        }
        return new Dimension(width, height);
    }
    
    /**
     * Returns our size in character units, where 'width' is the number of
     * columns and 'height' the number of rows. (In case you were concerned
     * about the fact that terminals tend to refer to y,x coordinates.)
     */
    public Dimension getVisibleSizeInCharacters() {
        Dimension result = getVisibleSize();
        Insets insets = getInsets();
        result.width -= (insets.left + insets.right);
        result.height -= (insets.top + insets.bottom);
        Dimension character = getCharUnitSize();
        result.width /= character.width;
        result.height /= character.height;
        return result;
    }
    
    public void scrollToBottomButNotHorizontally() {
        JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
        
        BoundedRangeModel verticalModel = pane.getVerticalScrollBar().getModel();
        verticalModel.setValue(verticalModel.getMaximum() - verticalModel.getExtent());
    }
    
    private boolean isLineVisible(int lineIndex) {
        return (lineIndex >= getFirstVisibleLine() && lineIndex <= getLastVisibleLine());
    }
    
    public void scrollToTop() {
        scrollTo(0, 0, 0);
    }
    
    private void scrollTo(final int lineNumber, final int charStart, final int charEnd) {
        Dimension character = getCharUnitSize();
        final int x0 = charStart * character.width;
        final int y0 = lineNumber * character.height - 10;
        final int width = (charEnd - charStart) * character.width;
        final int height = character.height + 20;
        // Showing the beginning of the line first lets us scroll
        // horizontally as far as necessary but no further. We'd rather
        // show more of the beginning of the line in case we've jumped
        // here from a long way away; the beginning is where the
        // context is.
        scrollRectToVisible(new Rectangle(0, y0, 0, height));
        scrollRectToVisible(new Rectangle(x0, y0, width, height));
    }
    
    /**
     * Scrolls to the bottom of the output if doing so fits the user's
     * configuration, or is over-ridden by the fact that we're trying to
     * stay where we were but that *was* the bottom.
     */
    public void scrollOnTtyOutput(boolean wereAtBottom) {
        if (wereAtBottom || P9Term.getPreferences().getBoolean(P9TermPreferences.SCROLL_ON_TTY_OUTPUT)) {
            scrollToBottomButNotHorizontally();
        }
    }
    
    /**
     * Tests whether we're currently at the bottom of the output. Code
     * that's causing output will need to keep the result of invoking this
     * method so it can invoke scrollOnTtyOutput correctly afterwards.
     */
    public boolean isAtBottom() {
        Rectangle visibleRectangle = getVisibleRect();
        boolean atBottom = visibleRectangle.y + visibleRectangle.height >= getHeight();
        return atBottom;
    }
    
    public boolean shouldShowCursor() {
        return displayCursor;
    }
    
    public JViewport getViewport() {
        return (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
    }
    
    public int getFirstVisibleLine() {
        int lineHeight = getCharUnitSize().height;
        Rectangle visibleBounds = getViewport().getViewRect();
        return visibleBounds.y / lineHeight;
    }
    
    public int getLastVisibleLine() {
        int lineHeight = getCharUnitSize().height;
        Rectangle visibleBounds = getViewport().getViewRect();
        return (visibleBounds.y + visibleBounds.height) / lineHeight;
    }

    public boolean hasFocus() {
        return hasFocus;
    }
    
    //
    // Scrollable interface.
    //
    
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }
    
    public int getScrollableUnitIncrement(Rectangle visibleRectangle, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return visibleRectangle.height / 10;
        } else {
            return 3 * getCharUnitSize().width;
        }
    }
    
    public int getScrollableBlockIncrement(Rectangle visibleRectangle, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return visibleRectangle.height;
        } else {
            return visibleRectangle.width;
        }
    }
    
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }
    
    public boolean getScrollableTracksViewportHeight() {
        return false; // We want a vertical scroll-bar.
    }
}
