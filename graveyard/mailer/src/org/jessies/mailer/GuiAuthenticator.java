package org.jessies.mailer;

import e.forms.*;
import java.awt.*;
import javax.mail.*;
import javax.swing.*;

public class GuiAuthenticator extends Authenticator {
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        JPasswordField passwordField = new JPasswordField(40);
        
        String server = getRequestingSite().getHostName();
        if (getRequestingPort() != -1) {
            server += ":" + getRequestingPort();
        }
        
        // Find the currently-focused frame to use as a parent.
        JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner());
        
        FormBuilder form = new FormBuilder(frame, "Login");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Protocol:", new JLabel(getRequestingProtocol()));
        formPanel.addRow("Server:", new JLabel(server));
        formPanel.addRow("User name:", new JLabel(getDefaultUserName()));
        formPanel.addRow("Password:", passwordField);
        
        if (form.show("OK")) {
            return new PasswordAuthentication(getDefaultUserName(), new String(passwordField.getPassword()));
        } else {
            return null;
        }
    }
}
