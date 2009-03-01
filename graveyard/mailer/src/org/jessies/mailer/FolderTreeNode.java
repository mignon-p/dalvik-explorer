package org.jessies.mailer;

import java.util.*;
import javax.mail.*;
import javax.swing.tree.*;

public class FolderTreeNode extends DefaultMutableTreeNode {
    private Folder folder;
    
    public FolderTreeNode(Folder folder) {
        super(folder);
        this.folder = folder;
        initChildren();
    }
    
    public boolean isLeaf() {
        try {
            if ((folder.getType() & Folder.HOLDS_FOLDERS) == 0) {
                return true;
            }
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
        return false;
    }
    
    public Folder getFolder() {
        return folder;
    }
    
    private void initChildren() {
        if (isLeaf()) {
            return;
        }
        
        try {
            // Add all this folder's sub-folders in alphabetical order as child nodes.
            Folder[] subFolders = folder.list();
            Arrays.sort(subFolders, new FolderNameComparator());
            for (Folder subFolder : subFolders) {
                add(new FolderTreeNode(subFolder));
            }
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
    }
    
    public String toString() {
        return folder.getName();
    }
}
