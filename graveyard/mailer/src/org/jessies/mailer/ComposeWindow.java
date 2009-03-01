package org.jessies.mailer;

import com.sun.mail.smtp.*;
import e.forms.*;
import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import javax.swing.event.*;

public class ComposeWindow extends JFrame {
    // SMTP configuration.
    private static final int SMTPS_PORT = 465;
    private static final int SUBMISSION_PORT = 587;
    private static final int SMTP_PORT = 25;
    private static final int[] SMTP_PORTS_IN_ORDER = new int[] { SUBMISSION_PORT, SMTPS_PORT, SMTP_PORT };
    
    // UI.
    private Action sendAction = new SendAction();
    private Action attachAction = new AttachAction();
    private JTextField toField = new JTextField(40);
    private JTextField ccField = new JTextField(40);
    private JTextField subjectField = new JTextField(40);
    private PTextArea textArea = new PTextArea(20, 40);
    
    private Mailer mailer;
    private Session session;
    private Message originalMessage;
    private Message message;
    private AccountInfo account;
    
    public static ComposeWindow newForForwarding(Mailer mailer, Session session, Message originalMessage) throws Exception {
        // FIXME: when we support attachments, we should perhaps attach the original. Or is "forward as attachment" a separate command?
        String body = "\n";
        body += "\n";
        body += "-----Original Message-----\n";
        body += "From: " + originalMessage.getFrom()[0] + "\n";
        body += "Sent: " + Mailer.dateToIsoString(originalMessage.getSentDate()) + "\n";
        body += "To: " + StringUtilities.join(Arrays.asList(originalMessage.getRecipients(Message.RecipientType.TO)), ", ") + "\n";
        body += "Subject: " + originalMessage.getSubject() + "\n";
        body += "\n";
        body += new MimeBodyExtractor(originalMessage, false).getText();
        
        return new ComposeWindow(mailer, session, originalMessage, new MimeMessage(session), "", "",  "Fwd: " + originalMessage.getSubject(), body);
    }
    
    public static ComposeWindow newForReplyingTo(Mailer mailer, Session session, Message originalMessage) throws Exception {
        Message newMessage = originalMessage.reply(true);
        try {
            // MimeMessage.reply automatically sets the ANSWERED flag for us.
            // We don't want it to, but it does so much genuinely useful stuff for us (setting up the various header fields) that our best option is just to undo this one thing.
            originalMessage.setFlag(Flags.Flag.ANSWERED, false);
        } catch (MessagingException ex) {
            // Oh, well. A bummer, but not worth giving up over.
            ex.printStackTrace();
        }
        
        String to = formatAddressesExcludingSelf(newMessage.getRecipients(Message.RecipientType.TO), mailer.currentAccount().emailAddress());
        String cc = formatAddressesExcludingSelf(newMessage.getRecipients(Message.RecipientType.CC), mailer.currentAccount().emailAddress());
        
        // Start with some space to top-post a reply in.
        String body = "\n\n";
        // FIXME: insert the signature here.
        // FIXME: do we want to include "On " + Mailer.dateToIsoString(originalMessage.getSentDate()) + " "? Or is that more verbose than it is useful? Maybe just the date, and not the time?
        body += originalMessage.getFrom()[0] + " wrote:\n";
        body += new MimeBodyExtractor(originalMessage, false).getText().replaceAll("(?m)^", "> ");
        
        return new ComposeWindow(mailer, session, originalMessage, newMessage, to, cc, newMessage.getSubject(), body);
    }
    
    private static String formatAddressesExcludingSelf(Address[] recipients, String myAddress) {
        // javax.mail is an old API, and uses null arrays.
        if (recipients == null) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (Address recipient : recipients) {
            if (recipients.length > 1 && ((InternetAddress) recipient).getAddress().equalsIgnoreCase(myAddress)) {
                continue;
            }
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(recipient.toString());
        }
        return result.toString();
    }
    
    private void flagAsAnswered() {
        try {
            if (originalMessage != null) {
                originalMessage.setFlag(Flags.Flag.ANSWERED, true);
            }
        } catch (Exception ex) {
            // In addition to the usual chance of MessagingException, we get different kinds of exception for different kinds of "read-only".
            ex.printStackTrace();
        }
    }
    
    public static ComposeWindow newFromScratch(Mailer mailer, Session session) throws Exception {
        return new ComposeWindow(mailer, session, null, new MimeMessage(session), "", "", "", "");
    }
    
    private ComposeWindow(Mailer mailer, Session session, Message originalMessage, Message message, String to, String cc, String subject, String body) {
        this.mailer = mailer;
        this.account = mailer.currentAccount();
        this.session = session;
        this.originalMessage = originalMessage;
        this.message = message;
        
        initSubjectListener();
        
        setLayout(new BorderLayout(0, 6));
        
        // Set all the initial field values.
        toField.setText(to);
        ccField.setText(cc);
        subjectField.setText(subject);
        toField.setCaretPosition(0);
        ccField.setCaretPosition(0);
        subjectField.setCaretPosition(0);
        
        // Set up the text area.
        Mailer.configureTextArea(textArea);
        textArea.setText(body);
        
        // Put together the overall UI.
        add(makeTopPanel(), BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        pack();
        
        initFocus();
        
        setLocationRelativeTo(mailer);
        
        JFrameUtilities.setFrameIcon(this);
    }
    
    private void initSubjectListener() {
        // Keep the title bar up-to-date with respect to the subject field.
        subjectField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateTitleBar();
            }
            
            public void insertUpdate(DocumentEvent e) {
                updateTitleBar();
            }
            
            public void removeUpdate(DocumentEvent e) {
                updateTitleBar();
            }
        });
        // Set up the invariant.
        updateTitleBar();
    }
    
    private void updateTitleBar() {
        String title = subjectField.getText();
        if (title.trim().length() == 0) {
            title = "(No subject)";
        }
        setTitle(title);
    }
    
    private void initFocus() {
        // Make sure that hitting return in any of the header fields just hands on to the next text field/area in the sequence.
        toField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ccField.requestFocusInWindow();
            }
        });
        ccField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                subjectField.requestFocusInWindow();
            }
        });
        subjectField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.requestFocusInWindow();
            }
        });
        
        // Ensure that shift-tab in the text area takes us back up into the subject field.
        textArea.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.isConsumed() == false && e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_TAB) {
                    subjectField.requestFocusInWindow();
                }
            }
        });
        
        // Give focus to the most appropriate component.
        if (toField.getText().length() == 0) {
            toField.requestFocusInWindow();
        } else {
            textArea.requestFocusInWindow();
        }
    }
    
    private JComponent makeHeaderForm() {
        FormPanel headerForm = new FormPanel();
        headerForm.addRow("To:", toField);
        headerForm.addRow("Cc:", ccField);
        headerForm.addRow("Subject:", subjectField);
        return headerForm;
    }
    
    private JComponent makeToolBar() {
        JToolBar toolBar = new JToolBar();
        
        JButton sendButton = Mailer.makeButton(sendAction);
        JButton attachButton = Mailer.makeButton(attachAction);
        
        toolBar.setFloatable(false);
        toolBar.add(sendButton);
        toolBar.add(attachButton);
        return toolBar;
    }
    
    private JComponent makeTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(makeToolBar(), BorderLayout.NORTH);
        topPanel.add(makeHeaderForm(), BorderLayout.CENTER);
        return topPanel;
    }
    
    private class SendAction extends AbstractAction {
        private SendAction() {
            super("Send");
            putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("D", true));
            putValue(SMALL_ICON, new ImageIcon("/usr/share/icons/gnome/16x16/actions/mail-send.png"));
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            send();
        }
    }
    
    private void setUiEnabledState(boolean newState) {
        sendAction.setEnabled(newState);
        attachAction.setEnabled(newState);
        toField.setEnabled(newState);
        ccField.setEnabled(newState);
        subjectField.setEnabled(newState);
        textArea.setEnabled(newState);
    }
    
    private void send() {
        try {
            setUiEnabledState(false);
            
            message.setFrom(new InternetAddress(account.emailAddress(), account.realName()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toField.getText(), false));
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccField.getText(), false));
            message.setSubject(subjectField.getText());
            message.setText(textArea.getText());
            message.setHeader("X-Mailer", "org.jessies.mailer " + AboutBox.getSharedInstance().getIdentificationString());
            message.setSentDate(new Date());
            message.saveChanges();
            
            int outgoingPort = -1;
            for (int candidatePort : SMTP_PORTS_IN_ORDER) {
                if (SslUtilities.canConnectOn(account.outgoingHost(), candidatePort)) {
                    outgoingPort = candidatePort;
                    break;
                }
            }
            if (outgoingPort == -1) {
                SimpleDialog.showAlert(this, "SMTP", "Can't connect to SMTP server on \"" + account.outgoingHost() + "\" on any well-known port.");
                return;
            }
            
            SMTPTransport smtpTransport = (SMTPTransport) session.getTransport((outgoingPort == SMTPS_PORT) ? "smtps" : "smtp");
            try {
                if (outgoingPort != SMTPS_PORT) {
                    smtpTransport.setStartTLS(true);
                }
                smtpTransport.connect(account.outgoingHost(), outgoingPort, account.outgoingUser(), null);
                smtpTransport.sendMessage(message, message.getAllRecipients());
                
                flagAsAnswered();
                appendToSentFolder();
            } finally {
                System.err.println("Response: " + smtpTransport.getLastServerResponse());
                smtpTransport.close();
            }
            
            // If we get here, everything went okay.
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            setUiEnabledState(true);
        }
    }
    
    private void appendToSentFolder() {
        Folder sentFolder = mailer.sentFolder();
        if (sentFolder == null) {
            // FIXME: warn?
            return;
        }
        try {
            // Make sure the newly-sent message isn't considered unread.
            message.setFlag(Flags.Flag.SEEN, true);
            
            sentFolder.appendMessages(new Message[] { message });
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
    }
    
    private class AttachAction extends AbstractAction {
        private AttachAction() {
            super("Attach...");
            GnomeStockIcon.configureAction(this);
            putValue(SMALL_ICON, Attachment.ATTACHMENT_ICON);
        }
        
        public void actionPerformed(ActionEvent e) {
            // FIXME!
        }
        
        public boolean isEnabled() {
            return false;
        }
    }
}
