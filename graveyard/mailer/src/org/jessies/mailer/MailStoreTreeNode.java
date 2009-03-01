package org.jessies.mailer;

import java.util.*;
import javax.mail.*;
import javax.swing.tree.*;

public class MailStoreTreeNode extends DefaultMutableTreeNode {
    private Store mailStore;
    private String name;
    
    public MailStoreTreeNode(Store mailStore) {
        super(mailStore);
        this.mailStore = mailStore;
        initName();
        initChildren();
    }
    
    private void initName() {
        // FIXME: we might want to optionally let the user give a label instead (but we certainly shouldn't force them like most mailers do).
        this.name = mailStore.getURLName().getHost();
    }
    
    private void initChildren() {
        try {
            ArrayList<Folder> subFolders = new ArrayList<Folder>(Arrays.asList(mailStore.getDefaultFolder().list()));
            
            // Sort the folders into alphabetical order.
            Collections.sort(subFolders, new FolderNameComparator());
            
            // Find "INBOX" ...
            int inboxIndex = 0;
            for (int i = 0; i < subFolders.size(); ++i) {
                if (subFolders.get(i).getName().equalsIgnoreCase("INBOX")) {
                    inboxIndex = i;
                    break;
                }
            }
            // ... and move it to the start.
            if (inboxIndex != 0) {
                subFolders.add(0, subFolders.remove(inboxIndex));
            }
            
            // Add the folders as children of this store.
            for (Folder subFolder : subFolders) {
                add(new FolderTreeNode(subFolder));
            }
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
    }
    
    public String toString() {
        return name;
    }
}
