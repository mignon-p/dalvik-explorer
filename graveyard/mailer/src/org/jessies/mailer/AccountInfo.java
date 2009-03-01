package org.jessies.mailer;

import e.forms.*;
import e.util.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Represents the configured information about a user account.
 * 
 * Note that we do not and will not store passwords.
 * JavaMail caches them, but each time the user restarts their mailer, they'll have to type their password.
 */
public class AccountInfo {
    // FIXME: this is temporary only. no whining when this moves!
    private static final String ACCOUNT_INFO_FILENAME = "~/org.jessies.mailer.AccountInfo.xml";
    
    // "John Smith".
    private String realName;
    // "john.smith@example.com".
    private String emailAddress;
    
    // "imap.example.com".
    private String incomingHost;
    // Hard-wired for now. imaps is 993; imap would be 143.
    private int incomingPort = 993;
    // "john", the username that John Smith uses to log in.
    private String incomingUser;
    
    // "smtp.example.com".
    private String outgoingHost;
    // FIXME: should be determined experimentally at configuration-time and then hard-wired.
    // If the user's configured SMTPS, for example, we shouldn't switch to TLS on the SMTP or "submission" ports.
    // For now, this field is ignored and determined experimentally at send-time.
    private int outgoingPort;
    // This is currently fixed to be the same as incomingUser.
    // Surely no-one logs in as a different user on their IMAP and SMTP servers?
    private String outgoingUser;
    
    public AccountInfo() {
        this("", "", "", "", "");
    }
    
    public AccountInfo(String realName, String emailAddress, String user, String incomingHost, String outgoingHost) {
        this.realName = realName;
        this.emailAddress = emailAddress;
        this.incomingHost = incomingHost;
        this.incomingUser = user;
        this.outgoingHost = outgoingHost;
        this.outgoingUser = user;
    }
    
    public String realName() {
        return realName;
    }
    
    public String emailAddress() {
        return emailAddress;
    }
    
    public String incomingHost() {
        return incomingHost;
    }
    
    public int incomingPort() {
        return incomingPort;
    }
    
    public String incomingUser() {
        return incomingUser;
    }
    
    public String outgoingHost() {
        return outgoingHost;
    }
    
    public int outgoingPort() {
        return outgoingPort;
    }
    
    public String outgoingUser() {
        return outgoingUser;
    }
    
    public boolean editAccount(JFrame parent) {
        // FIXME: check /etc/passwd or ask Ruby or something to auto-fill the real name? or copy from the first existing account with a non-empty name?
        final JTextField realNameField = new JTextField(realName, 20);
        // FIXME: concatenate System.getProperty("user.name") + "@" + /* domain name */?
        final JTextField emailAddressField = new JTextField(emailAddress, 20);
        final JTextField incomingHostField = new JTextField(incomingHost, 20);
        final JTextField outgoingHostField = new JTextField(outgoingHost, 20);
        final JTextField usernameField = new JTextField(incomingUser, 20);
        
        FormBuilder form = new FormBuilder(parent, "Account Information");
        form.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // FIXME: possibilities include...
                // given "john.smith@example.com" in emailAddressField, set incomingHostField and outgoingHostField to ".example.com"?
                // given "john.smith@example.com" in emailAddressField, set incomingHostField to "imap.example.com"? maybe check if the host exists? serves IMAPS?
                // given "john.smith@example.com" in emailAddressField, set outgoingHostField to "smtp.example.com"? maybe check if the host exists? serves SMTPS/submission/SMTP?
                // for the above examples, maybe try "mail." too?
                // given "john.smith@example.com" in emailAddressField, set usernameField to "john.smith"?
                // ...but we need to not clobber anything the user's typed, including if we auto-fill, then they make edits, and then they go back and edit something that would make us auto-fill again.
                
                // FIXME: @gmail.com and @yahoo.com are special cases where we can fill in all the other fields with their exact values immediately. maybe fill & disable them?
            }
        });
        FormPanel formPanel = form.getFormPanel();
        // FIXME: should email address come first, to make it clearer that "Real Name" is the human's name, not some arbitrary identifier for the account (as some mailers additionally require)?
        formPanel.addRow("Real Name:", realNameField);
        formPanel.addRow("Email Address:", emailAddressField);
        formPanel.addEmptyRow();
        formPanel.addRow("Incoming (IMAP) Server:", incomingHostField);
        formPanel.addRow("Outgoing (SMTP) Server:", outgoingHostField);
        formPanel.addRow("Server Username:", usernameField);
        formPanel.addEmptyRow();
        if (form.show("OK")) {
            this.realName = realNameField.getText();
            this.emailAddress = emailAddressField.getText();
            // FIXME: if we ever need it, we could parse incomingHost and outgoingHost to strip off ":<port>" from the end.
            this.incomingHost = incomingHostField.getText();
            this.outgoingHost = outgoingHostField.getText();
            this.incomingUser = this.outgoingUser = usernameField.getText();
            // FIXME: should probably check that incomingHost offers IMAPS.
            // FIXME: need to port scan outgoingHost to determine what kind of SMTP to use.
            return true;
        }
        return false;
    }
    
    public String toString() {
        return "AccountInfo for " + emailAddress;
    }
    
    public static List<AccountInfo> readFromDisk() {
        ArrayList<AccountInfo> accounts = new ArrayList<AccountInfo>();
        try {
            org.w3c.dom.Document document = XmlUtilities.readXmlFromDisk(ACCOUNT_INFO_FILENAME);
            org.w3c.dom.Element root = document.getDocumentElement();
            
            int version = Integer.parseInt(root.getAttribute("version"));
            if (version == 1) {
                for (org.w3c.dom.Node accountNode = root.getFirstChild(); accountNode != null; accountNode = accountNode.getNextSibling()) {
                    if (accountNode instanceof org.w3c.dom.Element) {
                        org.w3c.dom.Element accountElement = (org.w3c.dom.Element) accountNode;
                        String realName = accountElement.getAttribute("realName");
                        String emailAddress = accountElement.getAttribute("emailAddress");
                        String user = accountElement.getAttribute("incomingUser");
                        String incomingHost = accountElement.getAttribute("incomingHost");
                        String outgoingHost = accountElement.getAttribute("outgoingHost");
                        accounts.add(new AccountInfo(realName, emailAddress, user, incomingHost, outgoingHost));
                    }
                }
            } else {
                throw new RuntimeException("account info file version number " + version + " not supported");
            }
        } catch (Exception ex) {
            Log.warn("Problem reading account info", ex);
        }
        return accounts;
    }
    
    public static void writeToDisk(List<AccountInfo> accounts) {
        try {
            org.w3c.dom.Document document = XmlUtilities.makeEmptyDocument();
            org.w3c.dom.Element root = document.createElement("accounts");
            document.appendChild(root);
            root.setAttribute("version", "1");
            
            for (AccountInfo account : accounts) {
                org.w3c.dom.Element accountElement = document.createElement("account");
                root.appendChild(accountElement);
                accountElement.setAttribute("realName", account.realName);
                accountElement.setAttribute("emailAddress", account.emailAddress);
                accountElement.setAttribute("incomingHost", account.incomingHost);
                accountElement.setAttribute("incomingUser", account.incomingUser);
                accountElement.setAttribute("outgoingHost", account.outgoingHost);
                accountElement.setAttribute("outgoingUser", account.outgoingUser);
            }
            
            XmlUtilities.writeXmlToDisk(ACCOUNT_INFO_FILENAME, document);
        } catch (Exception ex) {
            Log.warn("Problem writing account info", ex);
        }
    }
}
