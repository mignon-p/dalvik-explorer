package org.jessies.mailer;

import e.util.*;
import java.io.*;
import java.util.*;
import javax.mail.*;

public class MimeBodyExtractor {
    private List<Part> plainParts = new ArrayList<Part>();
    private Part htmlPart;
    
    private List<Attachment> attachments = new ArrayList<Attachment>();
    
    private String text;
    private boolean isHtml;
    
    public MimeBodyExtractor(Message message, boolean allowHtml) {
        this.text = "";
        this.isHtml = false;
        
        if (message == null) {
            return;
        }
        
        //dumpMessageStructure(message);
        
        try {
            extractParts(message);
            if (allowHtml && htmlPart != null) {
                this.text = getHtmlPartContent();
                this.isHtml = true;
            } else if (plainParts.size() > 0) {
                StringBuilder plainText = new StringBuilder();
                for (Part plainPart : plainParts) {
                    plainText.append(plainPart.getContent());
                }
                this.text = plainText.toString();
            } else if (htmlPart != null) {
                this.text = HtmlToPlainTextConverter.convert(getHtmlPartContent());
            } else {
                this.text = "<no usable content>";
            }
            
            // Internet mail tends to use "\r\n", but PTextArea will actually display the "\r"s if we don't strip them.
            text = text.replaceAll("\r\n", "\n");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private String getHtmlPartContent() throws Exception {
        String result = (String) htmlPart.getContent();
        
        // Swing's HTML parser doesn't cope with the standard XML introduction, so strip it.
        String xmlIntroduction = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
        if (result.startsWith(xmlIntroduction)) {
            result = result.substring(xmlIntroduction.length());
        }
        
        return result;
    }
    
    public String getText() {
        return text;
    }
    
    public boolean isHtml() {
        return isHtml;
    }
    
    public List<Attachment> getAttachments() {
        return attachments;
    }
    
    /**
     * Return the primary text content of the message.
     * Originally based on the JavaMail FAQ (http://java.sun.com/products/javamail/FAQ.html).
     * Much changed by real-life experience.
     * 
     * Common forms:
     * 
     * text/plain - text only
     * text/html - HTML only
     * multipart/alternative ( text/plain text/html ) - text and HTML representing the same content
     * multipart/mixed ( text/plain image/jpeg image/jpeg image/jpeg ) - text with image attachments
     * multipart/mixed ( text/plain message/rfc822 ) - text with an attached message; often used for forward-as-attachment
     * multipart/mixed ( multipart/alternative ( text/plain text/html ) application/msword ) - typical Outlook representation of text plus attachment
     * multipart/related ( text/plain image/jpeg ) - Mail.app representation of a text draft with an image attachment
     * 
     * GMail seems to use INLINE for all its text (in some circumstances).
     * Mail.app uses structures such as multipart/mixed ( text/plain image/jpeg text/plain ) where the two text/plain parts should be taken *together* as the body.
     * 
     * FIXME: when collecting, is it worth keeping any of the tree structure? probably just confusing.
     * FIXME: given multipart/mixed ( text/plain image/jpeg text/plain ) should we refer to the filename of the image in the body text we generate? could be very useful given multiple inline images.
     */
    private void extractParts(Part part) throws MessagingException, IOException {
        String disposition = part.getDisposition();
        if (disposition != null && (disposition.equalsIgnoreCase(Part.ATTACHMENT))) {
            attachments.add(new Attachment(part));
        } else if (part.isMimeType("text/plain")) {
            plainParts.add(part);
        } else if (part.isMimeType("text/html")) {
            if (htmlPart != null) {
                // FIXME: i've yet to see this in the wild. until then, i'm not sure what the interpretation should be.
                System.err.println("*** warning: multiple HTML parts");
            }
            this.htmlPart = part;
        } else if (part.isMimeType("multipart/alternative") || part.isMimeType("multipart/mixed") || part.isMimeType("multipart/related")) {
            // multipart/alternative is used when you have multiple representations of the same part.
            // multipart/mixed and multipart/related seem fairly interchangeable.
            // Currently, there's no obvious need to distinguish these three.
            // FIXME: there are other multipart/* content types, but it seems best to decide how to handle them as we come across them.
            Multipart parts = (Multipart) part.getContent();
            for (int i = 0; i < parts.getCount(); ++i) {
                extractParts(parts.getBodyPart(i));
            }
        } else if (part.isMimeType("image/*")) {
            attachments.add(new Attachment(part));
        } else if (part.isMimeType("application/*")) {
            attachments.add(new Attachment(part));
        } else if (part.isMimeType("message/rfc822")) {
            attachments.add(new Attachment(part));
        } else {
            System.err.println("*** warning: unknown part with content type=\"" + part.getContentType() + "\" and class=\"" + part.getClass() + "\"");
        }
    }
    
    public static String mimeStructureToString(Message message) {
        StringBuilder result = new StringBuilder();
        dumpPart(result, "", message);
        return result.toString();
    }
    
    private static void dumpPart(StringBuilder out, String prefix, Part p) {
        try {
            out.append(prefix + "Content-Type: " + p.getContentType() + "\n");
            out.append(prefix + "Class: " + p.getClass().toString() + "\n");
            if (p.getDisposition() != null) {
                out.append(prefix + "Disposition: " + p.getDisposition() + "\n");
            }
            if (p.getFileName() != null) {
                out.append(prefix + "File Name: " + p.getFileName() + "\n");
            }
            
            out.append(prefix + "Content: ");
            Object o = p.getContent();
            if (o == null) {
                out.append("(null)" + "\n");
            } else {
                out.append(o.getClass().toString() + "\n");
            }
            
            if (o instanceof Multipart) {
                String newPrefix = prefix + "    ";
                Multipart mp = (Multipart) o;
                int count = mp.getCount();
                for (int i = 0; i < count; ++i) {
                    out.append(newPrefix + "-----------------" + "\n");
                    dumpPart(out, newPrefix, mp.getBodyPart(i));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
