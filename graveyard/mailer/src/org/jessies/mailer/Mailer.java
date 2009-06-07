package org.jessies.mailer;

import com.apple.eawt.*;
import com.sun.mail.imap.*;
import e.forms.*;
import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;

public class Mailer extends MainFrame {
    private static final SimpleDateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss Z");
    private static final Font SMALL_FONT = makeSmallFont();
    
    // UI.
    private DefaultMutableTreeNode mailboxesTreeRoot = new DefaultMutableTreeNode("Invisible Root");
    private ETree mailboxesTree;
    private JTable mailboxTable;
    private HeaderPane headerPane;
    private JScrollPane previewScrollPane;
    private PTextArea textPreviewPane;
    private HtmlPane htmlPreviewPane;
    private EStatusBar statusBar;
    
    // Actions.
    private DeleteAction deleteAction = new DeleteAction();
    private DeletePermanentlyAction deletePermanentlyAction = new DeletePermanentlyAction();
    private ForwardAction forwardAction = new ForwardAction();
    private ReplyAction replyAction = new ReplyAction();
    private ShowHeadersAction showHeadersAction = new ShowHeadersAction();
    private ShowRawMessageAction showRawMessageAction = new ShowRawMessageAction();
    private ShowStructureAction showStructureAction = new ShowStructureAction();
    
    // Preferences.
    private JSlider scaleSlider;
    private JCheckBox showCrosshairCheckBox;
    private JCheckBox showGridCheckBox;
    private JCheckBox keepOnTopCheckBox;
    
    private Session mailSession;
    private Folder sentFolder;
    private Folder trashFolder;
    // FIXME: a better implementation might be to pull the text and html preview components out into their own custom component, and have getMessage/setMessage on that component.
    private Message currentlyPreviewedMessage;
    
    private List<AccountInfo> accounts = AccountInfo.readFromDisk();
    
    public Mailer() {
        super("Mailer");
        
        // Choose a default size based on the overall screen size.
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        size.height /= 2;
        size.width /= 2;
        setSize(size);
        
        // Set up the UI.
        JFrameUtilities.setFrameIcon(this);
        setJMenuBar(new MailerMenuBar());
        setContentPane(makeUi());
        updateActionStates(0);
        setVisible(true);
        expandMailboxSubjectColumn();
        
        try {
            statusBar.setText("Connecting to incoming mail server...");
            this.mailSession = Session.getInstance(makeJavaMailProperties(), new GuiAuthenticator());
            //mailSession.setDebug(true);
            Store mailStore = logInToImapServer(currentAccount());
            if (mailStore == null) {
                statusBar.setText("Not logged in.");
                return;
            }
            
            statusBar.setText("Examining folder tree...");
            mailboxesTreeRoot.add(new MailStoreTreeNode(mailStore));
            mailboxesTree.expandAll();
            mailboxesTree.selectNodesMatching("inbox", true);
            
            statusBar.setText("Looking for trash folder...");
            initSentAndTrash(mailStore);
            statusBar.setText("");
            
            initCurrentFolderMonitor();
            
            //mailStore.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private Properties makeJavaMailProperties() {
        Properties properties = new Properties();
        
        // This doesn't appear to make much difference to performance, but is supposed to ward off bugs.
        properties.put("mail.imap.partialfetch", "false");
        
        // If we don't set this, the SMTP transport won't even try to authenticate.
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtps.auth", "true");
        
        return properties;
    }
    
    private Store logInToImapServer(AccountInfo account) throws Exception {
        if (SslUtilities.initSsl(this, account.incomingHost(), account.incomingPort()) == false) {
            // The user declined to trust the server.
            return null;
        }
        Store mailStore = mailSession.getStore("imaps");
        while (true) {
            try {
                mailStore.connect(account.incomingHost(), account.incomingPort(), account.incomingUser(), null);
                return mailStore;
            } catch (AuthenticationFailedException ex) {
                String serverError = ex.getMessage();
                if (serverError == null) {
                    // The user declined to authenticate (by hitting "Cancel", say).
                    return null;
                }
                SimpleDialog.showAlert(this, "Failed to log in", "The IMAP server says: \"" + serverError + "\"");
            }
        }
    }
    
    /**
     * Returns the given Date as a String in ISO 8601 format.
     * We have a problem in that JavaMail uses Date, which is a UTC timestamp.
     * We can't find out the original time zone on the parsed date string.
     */
    public static String dateToIsoString(Date date) {
        return ISO_8601.format(date);
    }
    
    /**
     * Returns the given byte count as a String in human-readable SI format.
     */
    public static String byteCountToString(int byteCount) {
        final int KiB = 1024;
        final int MiB = 1024*KiB;
        final int GiB = 1024*MiB;
        if (byteCount < 1*KiB) {
            return byteCount + " B";
        } else if (byteCount < 1*MiB) {
            return String.format("%.1f KiB", (double) byteCount / KiB);
        } else if (byteCount < 1*GiB) {
            return String.format("%.1f MiB", (double) byteCount / MiB);
        } else {
            return String.format("%.1f GiB", (double) byteCount / GiB);
        }
    }
    
    private JComponent makeUi() {
        initAboutBox();
        initMacOsEventHandlers();
        
        initKeepOnTopCheckBox();
        initScaleSlider();
        initShowCrosshairCheckBox();
        initShowGridCheckBox();
        
        JPanel result = new JPanel(new BorderLayout());
        result.add(makeToolBar(), BorderLayout.NORTH);
        result.add(makeThreePaneView(), BorderLayout.CENTER);
        result.add(makeStatusBar(), BorderLayout.SOUTH);
        return result;
    }
    
    private JComponent makeToolBar() {
        // Use a one-word name for "New Message" when it's on a button.
        JButton newButton = makeButton(new NewMessageAction());
        newButton.setText("New");
        
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(newButton);
        toolBar.add(makeButton(replyAction));
        toolBar.add(makeButton(forwardAction));
        return toolBar;
    }
    
    public JComponent makeStatusBar() {
        this.statusBar = new EStatusBar();
        statusBar.setFont(SMALL_FONT);
        
        // Put some space above the status bar, so it doesn't bang so hard against the main content of the window.
        JPanel result = new JPanel(new BorderLayout());
        result.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
        result.add(statusBar, BorderLayout.CENTER);
        return result;
    }
    
    public static JButton makeButton(Action action) {
        JButton button = new JButton(action);
        button.setFocusable(false);
        return button;
    }
    
    private void initAboutBox() {
        AboutBox aboutBox = AboutBox.getSharedInstance();
        aboutBox.setWebSiteAddress("http://software.jessies.org/mailer/");
        aboutBox.addCopyright("Copyright (C) 2007 Free Software Foundation, Inc.");
        aboutBox.addCopyright("All Rights Reserved.");
    }
    
    private void initMacOsEventHandlers() {
        if (GuiUtilities.isMacOs() == false) {
            return;
        }
        
        Application.getApplication().setEnabledPreferencesMenu(true);
        Application.getApplication().addApplicationListener(new ApplicationAdapter() {
            @Override
            public void handlePreferences(ApplicationEvent e) {
                showPreferencesDialog();
                e.setHandled(true);
            }
            
            @Override
            public void handleQuit(ApplicationEvent e) {
                e.setHandled(true);
            }
        });
    }
    
    private JComponent makeThreePaneView() {
        // We need to manually handle the delete and shift-delete actions in the text and HTML preview panes.
        // This seems to be the easiest way, and it's almost bound to be the only way that will work identically for JTextPane and PTextArea.
        KeyListener deleteKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelectedMessages(e.isShiftDown());
                    e.consume();
                }
            }
        };
        
        textPreviewPane = new PTextArea();
        configureTextArea(textPreviewPane);
        textPreviewPane.setEditable(false);
        textPreviewPane.addKeyListener(deleteKeyListener);
        
        htmlPreviewPane = new HtmlPane();
        htmlPreviewPane.addKeyListener(deleteKeyListener);
        
        DefaultTreeModel treeModel = new DefaultTreeModel(mailboxesTreeRoot);
        mailboxesTree = new ETree(treeModel);
        mailboxesTree.setRootVisible(false);
        mailboxesTree.setFont(SMALL_FONT);
        mailboxesTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = e.getNewLeadSelectionPath();
                if (path != null) {
                    Object o = path.getLastPathComponent();
                    if (o instanceof FolderTreeNode) {
                        Folder folder = ((FolderTreeNode) o).getFolder();
                        showFolder(folder);
                    }
                }
            }
        });
        // We can't trust JTree's row height calculation. Make it use the preferred height of the cell renderer component instead.
        mailboxesTree.setRowHeight(-1);
        
        mailboxTable = new ETable();
        mailboxTable.setModel(new FolderTableModel());
        mailboxTable.setFont(SMALL_FONT);
        mailboxTable.getTableHeader().setFont(SMALL_FONT);
        packMailboxTableColumns();
        mailboxTable.getModel().addTableModelListener(new TitleBarUpdater());
        // Set up sorting.
        mailboxTable.setAutoCreateRowSorter(true);
        mailboxTable.getRowSorter().setSortKeys(Collections.singletonList(new RowSorter.SortKey(3, SortOrder.DESCENDING)));
        // Handle selection changes in the mailbox table.
        mailboxTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                final int selectedItemCount = mailboxTable.getSelectedRowCount();
                updateActionStates(selectedItemCount);
                
                if (e.getValueIsAdjusting()) {
                    // We're not interested until the user's finished making their decision.
                    return;
                }
                
                if (selectedItemCount != 1) {
                    // They've selected nothing, or more than one message, so don't show anything.
                    showMessage(null);
                } else {
                    // They've selected a single message, so show it.
                    // Don't touch anything if the message is actually already showing.
                    // This happens, for example, if new mail arrives: table model indexes change, but the Message in question is the same.
                    // Not reshowing in this case isn't just an optimization: it keeps us from losing the user's place by resetting the scroll bar.
                    Message newlySelectedMessage = currentlySelectedMessages().get(0);
                    if (currentlyPreviewedMessage != newlySelectedMessage) {
                        showMessage(newlySelectedMessage);
                    }
                }
            }
        });
        mailboxTable.setDefaultRenderer(String.class, new TableCellRenderer() {
            private TableCellRenderer original = mailboxTable.getDefaultRenderer(String.class);
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = original.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Message message = ((FolderTableModel) table.getModel()).getMessage(mailboxTable.convertRowIndexToModel(row));
                try {
                    // Show unread messages in bold.
                    if (message.isSet(Flags.Flag.SEEN) == false) {
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    }
                    // Show deleted messages in italic.
                    // FIXME: it would be nice to have a "disabled" kind of color here, but I don't know of a suitable UIManager key.
                    if (message.isSet(Flags.Flag.DELETED)) {
                        c.setFont(c.getFont().deriveFont(Font.ITALIC));
                    }
                } catch (MessagingException ex) {
                    ex.printStackTrace();
                }
                return c;
            }
        });
        // Free up shift-del in the table for the "Delete Permanently" menu item.
        mailboxTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK), "this-action-does-not-exist");
        
        this.previewScrollPane = new JScrollPane(textPreviewPane);
        this.headerPane = new HeaderPane();
        
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.add(headerPane, BorderLayout.NORTH);
        previewPanel.add(previewScrollPane, BorderLayout.CENTER);
        
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(mailboxTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), previewPanel);
        verticalSplit.setDividerLocation(200);
        
        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(mailboxesTree), verticalSplit);
        horizontalSplit.setDividerLocation(200);
        
        return horizontalSplit;
    }
    
    // This is called before the UI is visible, to give appropriate amounts of space to columns other than the "Subject" column.
    private void packMailboxTableColumns() {
        Object[] SAMPLE_VALUES = new Object[] { "R", "mailing-list@somewhere.org.uk", "<see expandMailboxSubjectColumn>", "2007-11-11 20:46:23 -0800" };
        for (int i = 0; i < mailboxTable.getModel().getColumnCount(); ++i) {
            packColumn(mailboxTable, i, SAMPLE_VALUES[i]);
        }
        
        // The "answered flag" column shouldn't be allowed to grow.
        TableColumn answeredColumn = mailboxTable.getColumnModel().getColumn(0);
        answeredColumn.setMaxWidth(answeredColumn.getPreferredWidth());
    }
    
    // This is called after the UI is visible, so we can give all free space to the "Subject" column.
    private void expandMailboxSubjectColumn() {
        int width = mailboxTable.getParent().getWidth();
        for (int i = 0; i < mailboxTable.getModel().getColumnCount(); ++i) {
            if (i != 2) {
                width -= mailboxTable.getColumnModel().getColumn(i).getPreferredWidth();
            }
        }
        mailboxTable.getColumnModel().getColumn(2).setPreferredWidth(width);
    }
    
    private void packColumn(JTable table, int columnIndex, Object sampleValue) {
        // Get width of sample column data.
        TableCellRenderer renderer = table.getCellRenderer(0, columnIndex);
        Component component = renderer.getTableCellRendererComponent(table, sampleValue, false, false, 0, columnIndex);
        
        // Set the column's preferred width to correspond.
        table.getColumnModel().getColumn(columnIndex).setPreferredWidth(component.getPreferredSize().width + 4);
    }
    
    public static void configureTextArea(PTextArea textArea) {
        textArea.setTextStyler(new PEmailTextStyler(textArea));
        textArea.setWrapStyleWord(true);
        textArea.addStyleApplicator(new EmailAddressStyleApplicator(textArea));
        BugDatabaseHighlighter.highlightBugs(textArea);
    }
    
    private static Font makeSmallFont() {
        final float PANGO_SCALE_SMALL = (1.0f / 1.2f);
        final Font baseFont = UIManager.getFont("TextArea.font");
        Font smallFont = baseFont.deriveFont(baseFont.getSize2D() * PANGO_SCALE_SMALL);
        return smallFont;
    }
    
    private void updateActionStates(int selectedItemCount) {
        deleteAction.setEnabled(selectedItemCount > 0);
        deletePermanentlyAction.setEnabled(selectedItemCount > 0);
        forwardAction.setEnabled(selectedItemCount == 1);
        replyAction.setEnabled(selectedItemCount == 1);
        showHeadersAction.setEnabled(selectedItemCount == 1);
        showRawMessageAction.setEnabled(selectedItemCount == 1);
        showStructureAction.setEnabled(selectedItemCount > 0);
    }
    
    private class TitleBarUpdater implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            try {
                Folder folder = ((FolderTableModel) e.getSource()).getFolder();
                String title = folder.getName() + " ";
                title += "(";
                title += StringUtilities.pluralize(folder.getMessageCount(), "item", "items");
                int unreadMessageCount = folder.getUnreadMessageCount();
                if (unreadMessageCount > 0) {
                    title += ", " + unreadMessageCount + " unread";
                }
                title += ") - Mailer";
                setTitle(title);
            } catch (MessagingException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public List<AccountInfo> accounts() {
        // This deliberately exposes our list of accounts, for the benefit of AccountsManagerAction.
        // The Mailer class itself probably isn't the place for this, but there's a bit of work to do still before it will become obvious where this should go.
        return accounts;
    }
    
    public AccountInfo currentAccount() {
        // FIXME: need to support multiple accounts.
        // FIXME: need to cope when there's no initial configuration.
        return accounts.get(0);
    }
    
    private List<Message> currentlySelectedMessages() {
        // Get the selected rows as model row indexes (to cope with sorting).
        int[] rows = mailboxTable.getSelectedRows();
        for (int i = 0; i < rows.length; ++i) {
            rows[i] = mailboxTable.convertRowIndexToModel(rows[i]);
        }
        
        // Get the Message instances corresponding to the selected messages.
        FolderTableModel tableModel = (FolderTableModel) mailboxTable.getModel();
        ArrayList<Message> messages = new ArrayList<Message>();
        for (int row : rows) {
            messages.add(tableModel.getMessage(row));
        }
        return messages;
    }
    
    private void initSentAndTrash(Store mailStore) {
        // IMAP doesn't specify a standard name for the sent/trash folders, nor a standard mechanism to identify or locate the sent/trash folders.
        // We just cycle through various possibilities until we find something that exists.
        // Are there IMAP servers that don't create any sent/trash folder by default?
        // Are there IMAP servers stupid enough to do localization of the folder name in the store, rather than leaving it to clients?
        this.sentFolder = findFolderFromPossibleNames(mailStore, Arrays.asList("Sent", "Sent Items", "Sent Messages"));
        this.trashFolder = findFolderFromPossibleNames(mailStore, Arrays.asList("Trash", "Deleted Items", "Deleted Messages"));
        // FIXME: should we check and warn if we couldn't find the sent/trash folders?
        // FIXME: should we offer to create them?
    }
    
    private Folder findFolderFromPossibleNames(Store mailStore, List<String> possibleNames) {
        for (String possibleName : possibleNames) {
            try {
                Folder folder = mailStore.getFolder(possibleName);
                if (folder.exists()) {
                    return folder;
                }
            } catch (MessagingException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }
    
    public Folder sentFolder() {
        return sentFolder;
    }
    
    private void initCurrentFolderMonitor() {
        // We need to prod the server on a regular basis, because although it will tell us things we haven't asked it, it won't speak until spoken to.
        // The JavaMail FAQ recommends calling getNewMessageCount, presumably because it's intention-revealing.
        // Anyway, this ensures that we check once a minute for new mail.
        // If anything's happened on the server, the relevant event listeners will be called.
        final int NEW_MAIL_CHECK_DELAY_IN_MILLISECONDS = 60*1000;
        java.util.Timer currentFolderMonitor = new java.util.Timer("Current Folder Monitor", true);
        currentFolderMonitor.schedule(new TimerTask() {
            public void run() {
                try {
                    statusBar.setText("Checking for new mail...");
                    Folder currentFolder = ((FolderTableModel) mailboxTable.getModel()).getFolder();
                    if (currentFolder != null) {
                        currentFolder.getNewMessageCount();
                    }
                    statusBar.setText("");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, NEW_MAIL_CHECK_DELAY_IN_MILLISECONDS, NEW_MAIL_CHECK_DELAY_IN_MILLISECONDS);
    }
    
    private void showMessage(final Message message) {
        statusBar.setText("Getting message body...");
        try {
            this.currentlyPreviewedMessage = message;
            final MimeBodyExtractor mimeBodyExtractor = new MimeBodyExtractor(message, true);
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    JComponent displayingComponent = (mimeBodyExtractor.isHtml()) ? htmlPreviewPane : textPreviewPane;
                    previewScrollPane.setViewportView(displayingComponent);
                    if (mimeBodyExtractor.isHtml()) {
                        htmlPreviewPane.setText(mimeBodyExtractor.getText());
                    } else {
                        textPreviewPane.setText(mimeBodyExtractor.getText());
                    }
                    ComponentUtilities.divertPageScrollingFromTo(mailboxTable, displayingComponent);
                    
                    // Show the appropriate header/attachment information.
                    headerPane.setMessage(message);
                    headerPane.setAttachments(mimeBodyExtractor.getAttachments());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            statusBar.setText("");
        }
    }
    
    private void showFolder(Folder folder) {
        try {
            statusBar.setText("Selecting folder \"" + folder.getName() + "\"...");
            
            if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0) {
                return;
            }
            
            // Update the table.
            // We point the same model at a different folder.
            // Keeping a model per folder might be a better choice, because it would let us cache multiple folders in memory at once.
            ((FolderTableModel) mailboxTable.getModel()).setFolder(folder);
            mailboxTable.requestFocusInWindow();
            
            if (mailboxTable.getModel().getRowCount() > 0) {
                // I'm not sure this is the right behavior for all folders.
                // It's deeply frustrating that Outlook doesn't do this when you switch to Sent or Trash.
                // But maybe "inbox" should be a special case?
                // Or maybe there should be a timeout, so we remember where you were for, say, a minute, but after that assume you're here for new stuff?
                
                // Select the first message in the folder.
                // This (0,0) is in model coordinates.
                mailboxTable.getSelectionModel().setSelectionInterval(0, 0);
                
                // Scroll to the top.
                // This (0,0) is in view coordinates.
                mailboxTable.scrollRectToVisible(mailboxTable.getCellRect(0, 0, true));
            }
        } catch (MessagingException ex) {
            ex.printStackTrace();
        } finally {
            statusBar.setText("");
        }
    }
    
    private void initShowCrosshairCheckBox() {
        this.showCrosshairCheckBox = new JCheckBox("Show crosshair");
        showCrosshairCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                //scaledImagePanel.setShowCrosshair(showCrosshairCheckBox.isSelected());
            }
        });
        showCrosshairCheckBox.setSelected(true);
    }
    
    private void initShowGridCheckBox() {
        this.showGridCheckBox = new JCheckBox("Show grid");
        showGridCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                //scaledImagePanel.setShowGrid(showGridCheckBox.isSelected());
            }
        });
        showGridCheckBox.setSelected(false);
    }
    
    private void initKeepOnTopCheckBox() {
        this.keepOnTopCheckBox = new JCheckBox("Keep window on top");
        keepOnTopCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Mailer.this.setAlwaysOnTop(keepOnTopCheckBox.isSelected());
            }
        });
        keepOnTopCheckBox.setSelected(false);
    }
    
    private void initScaleSlider() {
        this.scaleSlider = new JSlider(1, 4);
        scaleSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                //scaleFactor = (1 << scaleSlider.getValue());
                //updatePosition(getPointerLocation());
            }
        });
        Hashtable<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
        for (int i = scaleSlider.getMinimum(); i <= scaleSlider.getMaximum(); ++i) {
            labels.put(i, new JLabel(Integer.toString(1 << i) + "x"));
        }
        scaleSlider.setLabelTable(labels);
        scaleSlider.setPaintLabels(true);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setSnapToTicks(true);
        scaleSlider.setValue(1);
    }
    
    private void showPreferencesDialog() {
        FormBuilder form = new FormBuilder(this, "Mailer Preferences");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Scale:", scaleSlider);
        formPanel.addRow("", showCrosshairCheckBox);
        formPanel.addRow("", showGridCheckBox);
        formPanel.addRow("", keepOnTopCheckBox);
        form.showNonModal();
        // also: [x] refresh only when mouse moves
        //       [ ] show mouse hot-spot
        // alternative grid colors?
    }
    
    private class MailerMenuBar extends JMenuBar {
        private MailerMenuBar() {
            if (GuiUtilities.isMacOs() == false) {
                add(makeFileMenu());
            }
            add(makeEditMenu());
            add(makeMessageMenu());
            add(makeHelpMenu());
        }
        
        private JMenu makeFileMenu() {
            JMenu menu = new JMenu("File");
            menu.add(new NewMessageAction());
            menu.add(new QuitAction());
            return menu;
        }
        
        private JMenu makeEditMenu() {
            JMenu menu = new JMenu("Edit");
            menu.add(new AccountsManagerAction(Mailer.this));
            if (GuiUtilities.isMacOs() == false) {
                menu.add(new PreferencesAction());
            }
            return menu;
        }
        
        private JMenu makeMessageMenu() {
            JMenu menu = new JMenu("Message");
            menu.add(deleteAction);
            menu.add(deletePermanentlyAction);
            menu.addSeparator();
            menu.add(replyAction);
            menu.add(forwardAction);
            menu.addSeparator();
            menu.add(showHeadersAction);
            menu.add(showRawMessageAction);
            menu.add(showStructureAction);
            return menu;
        }
        
        private JMenu makeHelpMenu() {
            HelpMenu helpMenu = new HelpMenu();
            return helpMenu.makeJMenu();
        }
    }
    
    private class DeleteAction extends AbstractAction {
        private DeleteAction() {
            super("Delete");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            deleteSelectedMessages(false);
        }
    }
    
    private class DeletePermanentlyAction extends AbstractAction {
        private DeletePermanentlyAction() {
            super("Delete Permanently");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK));
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            deleteSelectedMessages(true);
        }
    }
    
    private void deleteSelectedMessages(final boolean permanently) {
        if (permanently == false && trashFolder == null) {
            SimpleDialog.showAlert(this, "No trash folder", "Non-permanent deletion isn't possible without a trash folder on the IMAP server.");
        }
        
        // Get the messages in question.
        List<Message> messages = currentlySelectedMessages();
        if (messages.size() == 0) {
            return;
        }
        
        statusBar.setText("Deleting " + StringUtilities.pluralize(messages.size(), "message", "messages") + (permanently ? " permanently" : "") + "...");
        
        // Find the next row that isn't selected.
        // This is simple but effective.
        // I implemented the Apple Mail behavior first, but trying both, I prefer this.
        // Yahoo! web mail works the same.
        int newSelectedRow = mailboxTable.getSelectedRow();
        for (; newSelectedRow < mailboxTable.getRowCount(); ++newSelectedRow) {
            if (mailboxTable.isRowSelected(newSelectedRow) == false) {
                break;
            }
        }
        
        final Message[] messageArray = messages.toArray(new Message[messages.size()]);
        
        // Move the selection and remove the rows from the table.
        // We'll do the actual operations next, but this means that our UI doesn't try to display missing rows.
        mailboxTable.setRowSelectionInterval(newSelectedRow, newSelectedRow);
        ((FolderTableModel) mailboxTable.getModel()).removeMessages(messageArray);
        // Give the focus back to the table, or the tree (as the first focusable component in the window) gets it.
        mailboxTable.requestFocusInWindow();
        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MessageDeleter(permanently, messageArray).execute();
            }
        });
    }
    
    private class MessageDeleter extends SwingWorker<Object, Object> {
        private boolean permanently;
        private Message[] messages;
        
        private MessageDeleter(boolean permanently, Message[] messages) {
            this.permanently = permanently;
            this.messages = messages;
        }
        
        @Override
        public Object doInBackground() {
            try {
                // Find out what folder the messages are in.
                Folder folder = messages[0].getFolder();
                
                // If this isn't a permanent deletion, copy the messages to the trash folder first.
                if (permanently == false) {
                    folder.copyMessages(messages, trashFolder);
                }
                
                // Mark the messages (not the copies) for deletion.
                for (Message message : messages) {
                    message.setFlag(Flags.Flag.DELETED, true);
                }
                
                // Tell the server to actually delete the marked messages.
                // Doing this right away means that multiple clients will always be up-to-date with respect to one another.
                folder.expunge();
            } catch (Exception ex) {
                // FIXME: at this point, we've updated the UI but (possibly) failed to update the server.
                ex.printStackTrace();
            }
            return null;
        }
        
        @Override
        protected void done() {
            statusBar.setText((permanently ? "Permanently deleted" : "Deleted") + " " + StringUtilities.pluralize(messages.length, "message", "messages") + ".");
        }
    }
    
    private class ForwardAction extends AbstractAction {
        private ForwardAction() {
            super("Forward");
            putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("F", true));
            putValue(SMALL_ICON, new ImageIcon("/usr/share/icons/gnome/16x16/actions/mail-forward.png"));
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                List<Message> messages = currentlySelectedMessages();
                if (messages.size() == 1) {
                    ComposeWindow.newForForwarding(Mailer.this, mailSession, messages.get(0)).setVisible(true);
                } else if (messages.size() != 0) {
                    // FIXME: should be able to select a bunch of messages and forward them all as attachments to one new message.
                    // FIXME: note that the UI currently won't let us get this far. See updateActionStates.
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private class NewMessageAction extends AbstractAction {
        private NewMessageAction() {
            super("New Message");
            putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("N", false));
            putValue(SMALL_ICON, new ImageIcon("/usr/share/icons/gnome/16x16/actions/mail-message-new.png"));
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                ComposeWindow.newFromScratch(Mailer.this, mailSession).setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private class PreferencesAction extends AbstractAction {
        private PreferencesAction() {
            super("Preferences...");
            GnomeStockIcon.configureAction(this);
            setEnabled(false);
        }
        
        public void actionPerformed(ActionEvent e) {
            showPreferencesDialog();
        }
    }
    
    private class QuitAction extends AbstractAction {
        private QuitAction() {
            super("Quit");
            putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("Q", false));
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }
    
    private class ReplyAction extends AbstractAction {
        private ReplyAction() {
            super("Reply");
            putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("R", false));
            putValue(SMALL_ICON, new ImageIcon("/usr/share/icons/gnome/16x16/actions/mail-reply-sender.png"));
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                // The UI currently ensures we only actually reply to one message at once, but it doesn't really matter.
                List<Message> messages = currentlySelectedMessages();
                for (Message message : messages) {
                    ComposeWindow.newForReplyingTo(Mailer.this, mailSession, message).setVisible(true);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private class ShowHeadersAction extends AbstractAction {
        private ShowHeadersAction() {
            super("Show Full Headers");
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                // The UI currently ensures we only actually reply to one message at once, but it doesn't really matter.
                List<Message> messages = currentlySelectedMessages();
                for (Message message : messages) {
                    JFrameUtilities.showTextWindow(Mailer.this, message.getSubject() + " - Full Headers", headersToString(message));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        private String headersToString(Message message) throws MessagingException {
            StringBuilder result = new StringBuilder();
            Enumeration<?> headers = message.getAllHeaders();
            while (headers.hasMoreElements()) {
                Header h = (Header) headers.nextElement();
                result.append(h.getName());
                result.append(": ");
                result.append(h.getValue());
                result.append("\n");
            }
            return result.toString();
        }
    }
    
    private class ShowRawMessageAction extends AbstractAction {
        private ShowRawMessageAction() {
            super("Show Raw Message");
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                // The UI currently ensures we only actually reply to one message at once, but it doesn't really matter.
                List<Message> messages = currentlySelectedMessages();
                for (Message message : messages) {
                    JFrameUtilities.showTextWindow(Mailer.this, message.getSubject() + " - Raw Message", rawMessage(message));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        private String rawMessage(Message message) throws Exception {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                message.writeTo(out);
                return out.toString("UTF-8").replaceAll("\r", "");
            } catch (Exception ex) {
                ex.printStackTrace();
                return "<failed>\n";
            }
        }
    }
    
    private class ShowStructureAction extends AbstractAction {
        private ShowStructureAction() {
            super("Show MIME Structure");
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                // We support multiple selection here so it's easy to compare two messages' structure.
                List<Message> messages = currentlySelectedMessages();
                for (Message message : messages) {
                    JFrameUtilities.showTextWindow(Mailer.this, message.getSubject() + " - MIME Structure", MimeBodyExtractor.mimeStructureToString(message));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                GuiUtilities.initLookAndFeel();
                new Mailer();
            }
        });
    }
}
