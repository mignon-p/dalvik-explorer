package org.jessies.p9term;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;

public class TerminalFrame extends JFrame {
    private JTerminalPane terminal;
    
    private final Color originalBackground = getBackground();
    
    public TerminalFrame(JTerminalPane terminal) {
        super("p9term");
        this.terminal = terminal;
        initFrame();
        initFocus();
        terminal.start(this);
    }
    
    private void initFrame() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Misnomer: we add our own WindowListener.
        
        JFrameUtilities.setFrameIcon(this);
        
        P9Term.getInstance().getFrames().addFrame(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                P9Term.getInstance().getFrames().frameStateChanged();
            }
            
            @Override
            public void windowClosing(WindowEvent event) {
                handleWindowCloseRequestFromUser();
            }
            
            @Override
            public void windowIconified(WindowEvent event) {
                P9Term.getInstance().getFrames().frameStateChanged();
            }
            
            @Override
            public void windowDeiconified(WindowEvent event) {
                P9Term.getInstance().getFrames().frameStateChanged();
            }
        });
        initMenuBar();
        initTerminals();
        optionsDidChange();
        
        pack();
        setVisible(true);
        
        GuiUtilities.finishGnomeStartup();
    }
    
    private void initTerminals() {
        updateBackground();
        
        Dimension initialSize = terminal.getSize();
        
        terminal.invalidate();
        setContentPane(terminal);
        validate();
        
        terminal.requestFocus();
        
        Dimension finalSize = getContentPane().getSize();
        fixTerminalSizesAfterAddingOrRemovingTabbedPane(initialSize, finalSize);
    }
    
    private void initMenuBar() {
        setJMenuBar(new MenuBar());
        
        // Work around Sun bug 4949810 (setJMenuBar doesn't call revalidate/repaint).
        getJMenuBar().revalidate();
        
        // Work around Sun bug 6526971 (quick alt-tabbing on Windows can give focus to menu bar).
        if (GuiUtilities.isWindows()) {
            addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowLostFocus(WindowEvent e) {
                    MenuSelectionManager.defaultManager().clearSelectedPath();
                }
            });
        }
    }
    
    private void initFocus() {
        terminal.requestFocus();
    }
    
    /**
     * Increases the size of the frame based on the amount of space taken
     * away from the terminal to insert the tabbed pane. The end result
     * should be that the terminal size remains constant but the window
     * grows.
     */
    private void fixTerminalSizesAfterAddingOrRemovingTabbedPane(Dimension initialSize, Dimension finalSize) {
        // GNOME's current default window manager automatically ignores setSize if the window is maximized.
        // Windows doesn't, and that causes us to resize a maximized window to be larger than the display, which is obviously unwanted.
        // This early exit fixes Windows' behavior and doesn't hurt Linux.
        if ((getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH) {
            return;
        }
        
        Dimension size = getSize();
        size.height += (initialSize.height - finalSize.height);
        size.width += (initialSize.width - finalSize.width);
        
        // We dealt above with the case where the window is maximized, but we also have to deal with the case where the window is simply very tall.
        // GNOME and Mac OS will correctly constrain the window for us, but on Windows we have to try to do it ourselves.
        if (GuiUtilities.isWindows()) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
            final int availableVerticalScreenSpace = screenSize.height - screenInsets.top - screenInsets.bottom;
            if (getLocation().y + size.height > availableVerticalScreenSpace) {
                size.height = availableVerticalScreenSpace - getLocation().y;
            }
        }
        
        setSize(size);
    }

    /**
     * Removes the terminal and closes the window.
     */
    public void closeTerminalPane(JTerminalPane victim) {
        setVisible(false);
    }
    
    public void handleWindowCloseRequestFromUser() {
        if (terminal.doCheckedCloseAction() == false) {
            // If the user hit "Cancel" for one terminal, cancel the close for all other terminals in the same window.
            return;
        }
    }
    
    /**
     * Tidies up after the frame has been hidden.
     * We can't use a ComponentListener because that's invoked on the EDT, as is handleQuit, which relies on us tidying up while it goes.
     */
    @Override
    public void setVisible(boolean newState) {
        super.setVisible(newState);
        if (newState == false) {
            terminal.destroyProcess();
            dispose();
            P9Term.getInstance().getFrames().removeFrame(this);
        }
    }
    
    /**
     * Gives the frame the same background color as the terminal to improve appearance during resizes.
     * We don't do this when showing multiple tabs because Mac OS' tabbed pane is partially transparent.
     * Even on GNOME and Windows it would look odd because the tabbed pane is the outermost component, and the new space ought to belong to it, and share its color.
     */
    private void updateBackground() {
        setBackground(P9Term.getPreferences().getColor(P9TermPreferences.BACKGROUND_COLOR));
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        updateTransparency();
    }
    
    private void updateTransparency() {
        GuiUtilities.setFrameAlpha(this, P9Term.getPreferences().getDouble(P9TermPreferences.ALPHA));
    }
    
    public void optionsDidChange() {
        updateBackground();
        updateTransparency();
        terminal.optionsDidChange();
        repaint();
    }
    
    public void processLine(final String line) {
        terminal.processLine(line);
    }
    
    public void processSpecialCharacter(final char ch) {
        terminal.processSpecialCharacter(ch);
    }
}
