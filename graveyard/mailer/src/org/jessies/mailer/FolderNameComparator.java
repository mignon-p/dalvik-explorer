package org.jessies.mailer;

import java.util.*;
import javax.mail.*;

/**
 * Compares Folder instances based on the values they return from getName.
 */
public class FolderNameComparator implements Comparator<Folder> {
    public int compare(Folder o1, Folder o2) {
        return o1.getName().compareToIgnoreCase(o2.getName());
    }
    
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }
}
