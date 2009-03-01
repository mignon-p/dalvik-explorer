package org.jessies.mailer;

import java.util.*;
import javax.mail.*;
import javax.mail.event.*;
import javax.mail.internet.*;
import javax.swing.table.*;

public class FolderTableModel extends AbstractTableModel {
    private Folder folder;
    private List<Message> messages;
    
    private static final String[] columnNames = { "", "From", "Subject", "Received" };
    private static final Class<?>[] columnTypes = { String.class, String.class, String.class, String.class };
    
    public FolderTableModel() {
        try {
            setFolder(null);
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
    }
    
    public Folder getFolder() {
        return folder;
    }
    
    public void setFolder(Folder newFolder) throws MessagingException {
        if (folder != null) {
            try {
                folder.removeMessageCountListener(messageCountListener);
                folder.close(true);
            } catch (Exception ex) {
                // Failure here seems to happen relatively often.
                // enp has a corrupt folder, for example, and enh finds that Exchange seems to sometimes close folders on him, maybe because he's running Outlook at the same time.
                // It's not currently obvious that there's much to be gained by showing this error to the user.
                // It does seem that we should blunder on, rather than back out, because the UI's already been updated by this point.
                // If we back out here, we really need to back out the UI change too, and going that route seems like a mistake.
                ex.printStackTrace();
            }
        }
        
        this.folder = newFolder;
        this.messages = Collections.<Message>emptyList();
        
        if (folder != null) {
            scanWholeFolder();
            folder.addMessageChangedListener(messageChangedListener);
            folder.addMessageCountListener(messageCountListener);
        }
    }
    
    private MessageChangedListener messageChangedListener = new MessageChangedListener() {
        public void messageChanged(MessageChangedEvent e) {
            Message changedMessage = e.getMessage();
            for (int i = 0; i < messages.size(); ++i) {
                if (messages.get(i) == changedMessage) {
                    fireTableRowsUpdated(i, i);
                }
            }
        }
    };
    
    private MessageCountListener messageCountListener = new MessageCountListener() {
        public void messagesAdded(MessageCountEvent e) {
            try {
                // I don't think it matters where we put the new messages, but for the sanity of users who don't sort, and because it's the cheapest option, we'll bung them at the end.
                Message[] newMessages = e.getMessages();
                
                // Work out where the new items will appear.
                final int firstNewRow = messages.size();
                final int lastNewRow = firstNewRow + newMessages.length - 1;
                
                // Actually insert the new items, and notify the listeners.
                messages.addAll(Arrays.asList(newMessages));
                fireTableRowsInserted(firstNewRow, lastNewRow);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        public void messagesRemoved(MessageCountEvent e) {
            // We'll have removed messages from the table before we remove them from the server.
            // If another client is accessing the mailbox concurrently, though, messages might disappear under us.
            removeMessages(e.getMessages());
        }
    };
    
    public void removeMessages(Message[] removedMessages) {
        try {
            // Translate the array of removed Message instances into a list of all the row indexes to be removed from the model.
            // Assumes the list of removed messages is small.
            ArrayList<Integer> deadIndexes = new ArrayList<Integer>();
            for (Message message : removedMessages) {
                int deadIndex = messages.indexOf(message);
                if (deadIndex != -1) {
                    deadIndexes.add(deadIndex);
                }
            }
            
            // Actually remove the rows, notifying the listeners as we go (rather than bother trying to coalesce).
            for (int i = deadIndexes.size() - 1; i >= 0; --i) {
                int deadIndex = deadIndexes.get(i);
                messages.remove(deadIndex);
                fireTableRowsDeleted(deadIndex, deadIndex);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void scanWholeFolder() throws MessagingException {
        if (folder.isOpen() == false) {
            folder.open(Folder.READ_WRITE);
        }
        
        // Bulk-fetch the message envelopes.
        Message[] newMessages = folder.getMessages();
        FetchProfile fetchProfile = new FetchProfile();
        // FIXME: add CONTENT_INFO if we start to display the size
        //fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
        fetchProfile.add(FetchProfile.Item.ENVELOPE);
        fetchProfile.add(FetchProfile.Item.FLAGS);
        folder.fetch(newMessages, fetchProfile);
        
        this.messages = new ArrayList<Message>();
        messages.addAll(Arrays.asList(newMessages));
        
        fireTableDataChanged();
    }
    
    public Message getMessage(int row) {
        return messages.get(row);
    }
    
    //---------------------
    // Implementation of the TableModel methods
    //---------------------
    
    public String getColumnName(int column) {
        return columnNames[column];
    }
    
    public Class<?> getColumnClass(int column) {
        return columnTypes[column];
    }
    
    public int getColumnCount() {
        return columnNames.length;
    }
    
    public int getRowCount() {
        return messages.size();
    }
    
    public Object getValueAt(int row, int column) {
        try {
            Message message = getMessage(row);
            switch (column) {
            case 0:
                // Answered?
                return (message.isSet(Flags.Flag.ANSWERED) ? "R" : "");
                
            case 1:
                // From.
                Address[] addresses = message.getFrom();
                if (addresses == null || addresses.length == 0) {
                    return "(No sender)";
                }
                // Given "Personal Name <user@example.com>", choose "Personal Name" if it's available, and "user@example.com" if not.
                InternetAddress address = (InternetAddress) addresses[0];
                String name = address.getPersonal();
                return (name != null) ? name : address.getAddress();
                
            case 2:
                // Subject.
                String subject = message.getSubject();
                return (subject != null) ? subject : "(No subject)";
                
            case 3:
                // Date.
                Date date = message.getReceivedDate();
                return (date != null) ? Mailer.dateToIsoString(date) : "Unknown";
                
            default:
                return "<no-column-" + column + ">";
            }
        } catch (MessagingException ex) {
            ex.printStackTrace();
            return "<error>";
        }
    }
}
