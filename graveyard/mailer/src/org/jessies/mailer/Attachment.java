package org.jessies.mailer;

import java.awt.*;
import java.io.*;
import javax.imageio.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;

public class Attachment {
    public static final ImageIcon ATTACHMENT_ICON = new ImageIcon("/usr/share/icons/gnome/16x16/status/mail-attachment.png");
    
    // Canonical.
    private Part part;
    
    // Cheap.
    private String filename;
    private int byteCount;
    
    // Expensive.
    private Image image;
    private String text;
    
    public Attachment(Part part) {
        this.part = part;
        try {
            this.filename = part.getFileName();
            this.byteCount = part.getSize();
            
            /*
            FIXME: we don't want to do this until the user needs the attachment data. It's potentially very expensive.
            if (part.isMimeType("image/*")) {
                this.image = ImageIO.read(part.getInputStream());
            } else if (part.isMimeType("text/*")) {
                // FIXME: actually read the text.
                this.text = "<FIXME>";
            } else {
                // FIXME: actually read the binary.
            }
            */
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public String saveAttachment(ProgressMonitor progressMonitor) {
        // It would be nice to use MimeBodyPart.saveFile, but we have to do all the tedious work ourselves because we want to be able to show progress.
        OutputStream out = null;
        InputStream in = null;
        try {
            // FIXME: might want to strip dodgy characters from filename!
            String prefix = "attachment-";
            File file = File.createTempFile(prefix, filename);
            file.deleteOnExit();
            
            int estimatedByteCount = part.getSize();
            // Assuming it's base64 encoded, we'll actually get fewer bytes than are in the body part, because a base64-encoded file takes more space than the original.
            // FIXME: check the actual encoding.
            estimatedByteCount /= 1.37;
            progressMonitor.setMaximum(estimatedByteCount);
            
            int totalByteCount = 0;
            progressMonitor.setNote(Mailer.byteCountToString(totalByteCount) + " of " + Mailer.byteCountToString(estimatedByteCount));
            
            out = new BufferedOutputStream(new FileOutputStream(file));
            in = part.getInputStream();
            byte[] buffer = new byte[8192];
            int byteCount;
            while ((byteCount = in.read(buffer)) > 0) {
                out.write(buffer, 0, byteCount);
                
                totalByteCount += byteCount;
                progressMonitor.setProgress(totalByteCount);
                progressMonitor.setNote(Mailer.byteCountToString(totalByteCount) + " of " + Mailer.byteCountToString(estimatedByteCount));
                if (progressMonitor.isCanceled()) {
                    return null;
                }
            }
            
            return file.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            progressMonitor.close();
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public ImageIcon getIcon() {
        ImageIcon icon = new ImageIcon("/usr/share/icons/gnome/16x16/mimetypes/unknown.png");
        try {
            if (part.isMimeType("image/*")) {
                icon = new ImageIcon("/usr/share/icons/gnome/16x16/mimetypes/gnome-mime-image.png");
            } else if (part.isMimeType("text/*")) {
                icon = new ImageIcon("/usr/share/icons/gnome/16x16/mimetypes/gnome-mime-text.png");
            } else if (part.isMimeType("application/msword")) {
                icon = new ImageIcon("/usr/share/icons/gnome/16x16/mimetypes/gnome-mime-application-msword.png");
            } else if (part.isMimeType("application/vnd.ms-excel")) {
                icon = new ImageIcon("/usr/share/icons/gnome/16x16/mimetypes/gnome-mime-application-vnd.ms-excel.png");
            } else if (part.isMimeType("application/vnd.ms-powerpoint")) {
                icon = new ImageIcon("/usr/share/icons/gnome/16x16/mimetypes/gnome-mime-application-vnd.ms-powerpoint.png");
            } else if (part.isMimeType("application/pdf") || filenameEndsWith(".pdf")) {
                icon = new ImageIcon("/usr/share/icons/gnome/16x16/mimetypes/gnome-mime-application-pdf.png");
            } else if (part.isMimeType("application/zip") || filenameEndsWith(".zip")) {
                icon = new ImageIcon("/usr/share/icons/gnome/16x16/mimetypes/gnome-mime-application-zip.png");
            }
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
        return icon;
    }
    
    private boolean filenameEndsWith(String extension) {
        return (filename != null && filename.toLowerCase().endsWith(extension.toLowerCase()));
    }
    
    public String getFilename() {
        return filename;
    }
    
    public int getByteCount() {
        return byteCount;
    }
    
    public String toString() {
        return "Attachment[part=" + part + "\"" + filename + "\",byteCount=" + byteCount + "]";
    }
}
