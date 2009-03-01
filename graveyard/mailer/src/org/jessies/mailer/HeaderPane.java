package org.jessies.mailer;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.mail.*;
import javax.swing.*;

public class HeaderPane extends Box {
    private List<Attachment> attachments;
    
    private JLabel toLabel = new JLabel("");
    private JLabel ccLabel = new JLabel("");
    private JLabel bccLabel = new JLabel("");
    private List<JLabel> attachmentDetailsLabels = new ArrayList<JLabel>();
    
    public HeaderPane() {
        super(BoxLayout.PAGE_AXIS);
        
        setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 0));
        changeColorsIn(this, toLabel, ccLabel, bccLabel);
        
        add(toLabel);
        add(ccLabel);
        add(bccLabel);
        
        setMessage(null);
    }
    
    private void changeColorsIn(JComponent... components) {
        for (JComponent component : components) {
            // Use a darker variant of the selection.
            // FIXME: this might not work well in all themes.
            component.setBackground(UIManager.getColor("List.selectionBackground").darker());
            component.setForeground(UIManager.getColor("List.selectionForeground"));
            component.setOpaque(true);
        }
    }
    
    public void setMessage(Message message) {
        if (message == null) {
            // Improves our appearance on start-up or when multiple messages are selected, or when we're viewing an empty folder.
            // Without this, we only see the thin strip of border, and that looks like a bug.
            toLabel.setText(" ");
            
            // These ones we do want to hide.
            ccLabel.setText("");
            bccLabel.setText("");
        } else {
            updateLabel(toLabel, "To", Message.RecipientType.TO, message);
            updateLabel(ccLabel, "Cc", Message.RecipientType.CC, message);
            updateLabel(bccLabel, "Bcc", Message.RecipientType.BCC, message);
        }
    }
    
    private void updateLabel(JLabel label, String prefix, Message.RecipientType recipientType, Message message) {
        String newLabel = "";
        try {
            Address[] addresses = message.getRecipients(recipientType);
            if (addresses != null && addresses.length > 0) {
                newLabel = prefix + ": " + StringUtilities.join(Arrays.asList(addresses), ", ");
            }
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
        label.setText(newLabel);
        // FIXME: can we set a tool tip that only appears if the displayed text has been truncated?
        // FIXME: we could fit more on each line.
        // FIXME: we will be fooled by addresses such as "Idiot, Village <dodo@example.com>".
        // FIXME: the overall idea of using JLabel is probably wrong; we should probably automatically expand to take more than one line.
        label.setToolTipText("<html><body>" + newLabel.replace("&", "&amp;").replace("\"", "&quot;").replace(">", "&gt;").replace("<", "&lt;").replace(", ", ",<br>&nbsp;&nbsp;&nbsp;&nbsp;"));
    }
    
    public void setAttachments(List<Attachment> attachments) {
        // Remove any old attachment details.
        for (JLabel label : attachmentDetailsLabels) {
            remove(label);
        }
        attachmentDetailsLabels.clear();
        
        // Show detailed information about each attachment.
        int unnamedAttachmentNumber = 0;
        for (Attachment attachment : attachments) {
            // FIXME: it might make more sense to move this into Attachment. We'd probably lose the unique numbering, but we'd gain the ability to pull the subject line out of message/rfc822 attachments.
            String displayName = attachment.getFilename();
            if (displayName == null) {
                displayName = "unnamed attachment " + Integer.toString(++unnamedAttachmentNumber);
            }
            
            JLabel label = new JLabel(displayName + " (" + Mailer.byteCountToString(attachment.getByteCount()) + ")");
            label.setIcon(attachment.getIcon());
            label.addMouseListener(new AttachmentDoubleClickListener(attachment));
            changeColorsIn(label);
            add(label);
            attachmentDetailsLabels.add(label);
        }
        
        // Make sure the UI is updated.
        getParent().validate();
    }
    
    private static class AttachmentDoubleClickListener extends MouseAdapter {
        private Attachment attachment;
        
        public AttachmentDoubleClickListener(Attachment attachment) {
            this.attachment = attachment;
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                new Thread(new Runnable() {
                    public void run() {
                        // FIXME: we might want to write our own replacement for ProgressMonitor, which is pretty lame, but it'll do for now.
                        // Work around the fact that you can't set the title for the dialog; if we had another use for it in the same application, this would be potentially broken.
                        UIManager.put("ProgressMonitor.progressText", "Download Progress");
                        ProgressMonitor progressMonitor = new ProgressMonitor(null, attachment.getFilename(), "", 0, 100);
                        String temporaryFileName = attachment.saveAttachment(progressMonitor);
                        if (temporaryFileName != null) {
                            String launcher = (GuiUtilities.isMacOs() ? "open" : "gnome-open");
                            ProcessUtilities.spawn(null, new String[] { launcher, temporaryFileName } );
                        }
                    }
                }).start();
            }
        }
    }
}
