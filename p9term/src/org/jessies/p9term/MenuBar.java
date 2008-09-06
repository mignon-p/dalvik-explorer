package org.jessies.p9term;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Provides a menu bar for Mac OS, and acts as a source of Action instances for
 * the pop-up menu on all platforms.
 */
public class MenuBar extends EMenuBar {
    public MenuBar() {
        add(makeFileMenu());
        add(makeEditMenu());
        add(makeScrollbackMenu());
        add(makeHelpMenu());
    }
    
    private JMenu makeFileMenu() {
        JMenu menu = new JMenu("File");
        menu.add(new NewShellAction());
        
        menu.addSeparator();
        menu.add(new CloseAction());
        
        menu.addSeparator();
        menu.add(new ResetAction());
        
        return menu;
    }
    
    private JMenu makeEditMenu() {
        JMenu menu = new JMenu("Edit");
        // FIXME: add selection-related menu items.
        //menu.addSeparator();
        // FIXME: add find-related menu items.
        
        P9Term.getPreferences().initPreferencesMenuItem(menu);
        
        return menu;
    }
    
    private JMenu makeScrollbackMenu() {
        JMenu menu = new JMenu("Scrollback");
        
        menu.add(new ScrollToTopAction());
        menu.add(new ScrollToBottomAction());
        
        menu.addSeparator();
        menu.add(new PageUpAction());
        menu.add(new PageDownAction());
        
        menu.addSeparator();
        menu.add(new LineUpAction());
        menu.add(new LineDownAction());
        
        menu.addSeparator();
        menu.add(new ClearScrollbackAction());
        
        return menu;
    }
    
    private JMenu makeHelpMenu() {
        HelpMenu helpMenu = new HelpMenu();
        return helpMenu.makeJMenu();
    }
    
    /**
     * Tests whether the given event corresponds to a keyboard
     * equivalent.
     */
    public static boolean isKeyboardEquivalent(KeyEvent event) {
        final int modifier = GuiUtilities.getDefaultKeyStrokeModifier();
        return ((event.getModifiers() & modifier) == modifier);
    }
    
    private static KeyStroke makeKeyStroke(String key) {
        return GuiUtilities.makeKeyStroke(key, false);
    }
    
    private static KeyStroke makeShiftedKeyStroke(String key) {
        return GuiUtilities.makeKeyStroke(key, true);
    }
    
    private static Component getFocusedComponent() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
    }
    
    private static JTerminalPane getFocusedTerminalPane() {
        return (JTerminalPane) SwingUtilities.getAncestorOfClass(JTerminalPane.class, getFocusedComponent());
    }
    
    public static TerminalFrame getFocusedTerminalFrame() {
        return (TerminalFrame) SwingUtilities.getAncestorOfClass(TerminalFrame.class, getFocusedComponent());
    }
    
    //
    // Any new Action should probably subclass one of these abstract
    // classes. Only if your action requires neither a frame nor a
    // terminal pane (i.e. acts upon the application as a whole) should
    // you subclass AbstractAction directly.
    //
    
    /**
     * Superclass for actions that just need a TerminalFrame.
     */
    private abstract static class AbstractFrameAction extends AbstractAction {
        public AbstractFrameAction(String name) {
            super(name);
        }
        
        public void actionPerformed(ActionEvent e) {
            TerminalFrame frame = getFocusedTerminalFrame();
            if (frame != null) {
                performFrameAction(frame);
            }
        }
        
        protected abstract void performFrameAction(TerminalFrame frame);
        
        @Override
        public boolean isEnabled() {
            return (getFocusedTerminalFrame() != null);
        }
    }
    
    /**
     * Superclass for actions that need a JTerminalPane (that may or may
     * not have a frame to itself).
     */
    private abstract static class AbstractPaneAction extends AbstractAction {
        public AbstractPaneAction(String name) {
            super(name);
        }
        
        public void actionPerformed(ActionEvent e) {
            JTerminalPane terminalPane = getFocusedTerminalPane();
            if (terminalPane != null) {
                performPaneAction(terminalPane);
            }
        }
        
        protected abstract void performPaneAction(JTerminalPane terminalPane);
        
        @Override
        public boolean isEnabled() {
            return (getFocusedTerminalFrame() != null);
        }
    }
    
    public abstract static class BindableAction extends AbstractAction {
        private JTerminalPane boundTerminalPane;
        
        public BindableAction(String name) {
            super(name);
        }
        
        public void bindTo(JTerminalPane terminalPane) {
            this.boundTerminalPane = terminalPane;
        }
        
        public void actionPerformed(ActionEvent e) {
            JTerminalPane terminalPane = boundTerminalPane;
            if (terminalPane == null) {
                terminalPane = getFocusedTerminalPane();
            }
            if (terminalPane != null) {
                performOn(terminalPane);
            }
        }
        
        public abstract void performOn(JTerminalPane terminalPane);
        
        
        @Override
        public boolean isEnabled() {
            return (boundTerminalPane != null || getFocusedTerminalPane() != null);
        }
    }
    
    //
    // Custom Actions.
    //
    
    public static class NewShellAction extends AbstractAction {
        public NewShellAction() {
            super("New Shell");
            putValue(ACCELERATOR_KEY, makeKeyStroke("N"));
        }
        
        public void actionPerformed(ActionEvent e) {
            newShell();
        }
        
        public static void newShell() {
            P9Term.getInstance().openFrame(JTerminalPane.newShell());
        }
    }
    
    public static class CloseAction extends AbstractPaneAction {
        public CloseAction() {
            super("Close");
            putValue(ACCELERATOR_KEY, MenuBar.makeKeyStroke("W"));
            GnomeStockIcon.configureAction(this);
        }
        
        @Override
        protected void performPaneAction(JTerminalPane terminalPane) {
            terminalPane.doCheckedCloseAction();
        }
    }
    
    public static class ResetAction extends AbstractPaneAction {
        public ResetAction() {
            super("Reset");
        }
        
        @Override
        protected void performPaneAction(JTerminalPane terminalPane) {
            terminalPane.reset();
        }
    }
    
    public static class ScrollToTopAction extends AbstractPaneAction {
        public ScrollToTopAction() {
            super("Scroll To Top");
            putValue(ACCELERATOR_KEY, MenuBar.makeKeyStroke("HOME"));
        }
        
        @Override
        protected void performPaneAction(JTerminalPane terminalPane) {
            System.err.println("FIXME: scroll to top NYI");
        }
    }
    
    public static class ScrollToBottomAction extends AbstractPaneAction {
        public ScrollToBottomAction() {
            super("Scroll To Bottom");
            putValue(ACCELERATOR_KEY, MenuBar.makeKeyStroke("END"));
        }
        
        @Override
        protected void performPaneAction(JTerminalPane terminalPane) {
            System.err.println("FIXME: scroll to bottom NYI");
        }
    }
    
    public static class PageUpAction extends AbstractPaneAction {
        public PageUpAction() {
            super("Page Up");
            putValue(ACCELERATOR_KEY, MenuBar.makeKeyStroke("PAGE_UP"));
        }
        
        @Override
        protected void performPaneAction(JTerminalPane terminalPane) {
            terminalPane.pageUp();
        }
    }
    
    public static class PageDownAction extends AbstractPaneAction {
        public PageDownAction() {
            super("Page Down");
            putValue(ACCELERATOR_KEY, MenuBar.makeKeyStroke("PAGE_DOWN"));
        }
        
        @Override
        protected void performPaneAction(JTerminalPane terminalPane) {
            terminalPane.pageDown();
        }
    }
    
    public static class LineUpAction extends AbstractPaneAction {
        public LineUpAction() {
            super("Line Up");
            putValue(ACCELERATOR_KEY, MenuBar.makeKeyStroke("UP"));
        }
        
        @Override
        protected void performPaneAction(JTerminalPane terminalPane) {
            terminalPane.lineUp();
        }
    }
    
    public static class LineDownAction extends AbstractPaneAction {
        public LineDownAction() {
            super("Line Down");
            putValue(ACCELERATOR_KEY, MenuBar.makeKeyStroke("DOWN"));
        }
        
        @Override
        protected void performPaneAction(JTerminalPane terminalPane) {
            terminalPane.lineDown();
        }
    }
    
    public static class ClearScrollbackAction extends AbstractPaneAction {
        public ClearScrollbackAction() {
            super("Clear Scrollback");
            putValue(ACCELERATOR_KEY, MenuBar.makeKeyStroke("K"));
        }
        
        @Override
        protected void performPaneAction(JTerminalPane terminalPane) {
        }
    }
}
