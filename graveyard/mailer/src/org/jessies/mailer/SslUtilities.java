package org.jessies.mailer;

/*
 * Based on "InstallCert.java" 1.1 06/10/09
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 * 
 * It's not known what the "license" above is, but the code came from here:
 * http://blogs.sun.com/andreas/entry/no_more_unable_to_find
 * 
 * Call initSsl to initialize SSL on the port you know that JavaMail is
 * about to use before you call JavaMail. There may be a cleaner way, but
 * I haven't found it.
 * 
 * This was hard enough!
 */

import e.gui.*;
import e.util.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

public class SslUtilities {
    public static boolean canConnectOn(String host, int port) {
        // FIXME: change this class to NetworkUtilities, or move this into the account configuration code (when we have some)?
        try {
            Socket socket = new Socket(host, port);
            socket.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    private static KeyStore readKeyStore(char[] passphrase) throws Exception {
        // FIXME: we probably don't want to be using the system-wide one, do we?
        File file = new File("jssecacerts");
        if (file.isFile() == false) {
            char SEP = File.separatorChar;
            File dir = new File(System.getProperty("java.home") + SEP + "lib" + SEP + "security");
            file = new File(dir, "jssecacerts");
            if (file.isFile() == false) {
                file = new File(dir, "cacerts");
            }
        }
        Log.warn("Loading KeyStore from file \"" + file + "\"...");
        InputStream in = new FileInputStream(file);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(in, passphrase);
        in.close();
        return ks;
    }
    
    public static boolean initSsl(javax.swing.JFrame guiParent, String host, int port) throws Exception {
        // FIXME: not really needed if we're not persisting.
        // FIXME: what should we use if we are?
        char[] passphrase = "changeit".toCharArray();
        
        KeyStore ks = readKeyStore(passphrase);
        
        SSLContext context = SSLContext.getInstance("TLS");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        X509TrustManager defaultTrustManager = (X509TrustManager)tmf.getTrustManagers()[0];
        SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
        context.init(null, new TrustManager[] {tm}, null);
        
        if (testSslConnection(context, host, port)) {
            return true;
        }
        
        X509Certificate[] chain = tm.chain;
        if (chain == null) {
            SimpleDialog.showAlert(guiParent, "Security", "Could not obtain server certificate chain.");
            return false;
        }
        
        String question = "The server \"" + host + ":" + port + "\" sent ";
        if (chain.length == 1) {
            question += "the following certificate:";
        } else {
            question += StringUtilities.pluralize(chain.length, "certificate", "certificates") + ":";
        }
        question += "\n\n";
        
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        for (X509Certificate cert : chain) {
            sha1.update(cert.getEncoded());
            md5.update(cert.getEncoded());
            question += "   Subject:    " + cert.getSubjectDN() + "\n";
            question += "   Issuer:     " + cert.getIssuerDN() + "\n";
            question += "   Type:       " + cert.getType() + "\n";
            question += "   SHA1:       " + toHexString(sha1.digest()) + "\n";
            question += "   MD5:        " + toHexString(md5.digest()) + "\n";
            question += "   Valid from: " + Mailer.dateToIsoString(cert.getNotBefore()) + "\n";
            question += "   Valid to:   " + Mailer.dateToIsoString(cert.getNotAfter()) + "\n";
            question += "\n";
        }
        
        question += "Do you want to trust this server?";
        
        if (SimpleDialog.askQuestion(guiParent, "Security", question, "Trust")) {
            Log.warn("User chose to trust SSL certificate for \"" + host + ":" + port + "\"");
            for (int i = 0; i < chain.length; ++i) {
                X509Certificate cert = chain[i];
                String alias = host + "-" + (i + 1);
                ks.setCertificateEntry(alias, cert);
            }
        }
        
        /*
// Enable this if you want the trust to persist.
        OutputStream out = new FileOutputStream("jssecacerts");
        ks.store(out, passphrase);
        out.close();
        readKeyStore(passphrase);
        System.out.println("Added certificate to keystore 'jssecacerts' using alias '" + alias + "'");
        */
        
        tmf.init(ks);
        defaultTrustManager = (X509TrustManager)tmf.getTrustManagers()[0];
        tm = new SavingTrustManager(defaultTrustManager);
        context.init(null, new TrustManager[] {tm}, null);
        SSLContext.setDefault(context);
        
        return testSslConnection(null, host, port);
    }
    
    private static boolean testSslConnection(SSLContext context, String host, int port) throws Exception {
        if (context == null) {
            context = SSLContext.getDefault();
        }
        SSLSocketFactory factory = context.getSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
        socket.setSoTimeout(10000);
        try {
            socket.startHandshake();
            socket.close();
            Log.warn("SSL certificate for \"" + host + ":" + port + "\" is trusted");
            return true;
        } catch (SSLException ex) {
            Log.warn("SSL certificate for \"" + host + ":" + port + "\" is not trusted (" + ex.getMessage() + ")");
        }
        return false;
    }
    
    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();
    
    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0) {
                sb.append(':');
            }
            int b = (bytes[i] & 0xff);
            sb.append(HEXDIGITS[b >> 4]);
            sb.append(HEXDIGITS[b & 15]);
        }
        return sb.toString();
    }
    
    private static class SavingTrustManager implements X509TrustManager {
        private final X509TrustManager tm;
        private X509Certificate[] chain;
        
        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }
        
        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException();
        }
        
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new UnsupportedOperationException();
        }
        
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }
}
