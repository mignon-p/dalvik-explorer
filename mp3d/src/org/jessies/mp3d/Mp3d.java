/**
 * This file is part of mp3d.
 * Copyright (C) 2009 Elliott Hughes.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jessies.mp3d;

import com.sun.net.httpserver.*;
import e.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

public class Mp3d {
    private static final int MP3D_PORT = 8888;
    
    private List<File> musicDirectories;
    
    private LinkedBlockingQueue<Mp3Info> playQueue = new LinkedBlockingQueue<Mp3Info>();
    private List<Mp3Info> allMp3s = Collections.emptyList();
    private volatile Mp3Info nowPlaying = null;
    private volatile Process player = null;
    
    private class MainHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            final String formName = "search_form";
            final String inputName = "mp3d_q";
            
            try {
                ArrayList<String> terms = new ArrayList<String>();
                final String rawQuery = t.getRequestURI().getRawQuery();
                String q = null;
                if (rawQuery != null) {
                    q = StringUtilities.urlDecode(rawQuery);
                    if (q.startsWith(inputName + "=")) {
                        q = q.substring(inputName.length() + 1);
                    }
                    for (String term : q.split(" ")) {
                        terms.add(term.toLowerCase());
                    }
                }
                
                final StringBuilder page = new StringBuilder();
                
                page.append("<html>\n");
                page.append("<head>\n");
                page.append(" <title>mp3d</title>\n");
                page.append(" <link rel='stylesheet' href='/static/mp3d.css' type='text/css'>\n");
                page.append(" <script type='text/javascript' src='/static/mp3d.js'></script>\n");
                page.append("</head>\n");
                page.append("<body onload='document." + formName + "." + inputName + ".focus()'>\n");
                
                page.append("<h1>Queued Music</h1>\n");
                if (nowPlaying != null) {
                    ArrayList<Mp3Info> shownQueue = new ArrayList<Mp3Info>();
                    shownQueue.add(nowPlaying);
                    shownQueue.addAll(playQueue);
                    appendMp3Table(page, shownQueue, true);
                } else {
                    appendMp3Table(page, playQueue, true);
                }
                
                page.append("<h1>Search Library</h1>\n");
                page.append("<form name='" + formName + "'>");
                page.append("<input name='" + inputName + "' type='text' value='" + (q != null ? q : "") + "'>");
                page.append("</form>\n");
                
                if (!terms.isEmpty()) {
                    // Collect the matching mp3s.
                    final ArrayList<Mp3Info> matchingMp3s = new ArrayList<Mp3Info>();
                    for (Mp3Info mp3 : allMp3s) {
                        if (mp3.matches(terms)) {
                            matchingMp3s.add(mp3);
                        }
                    }
                    
                    // Sort first by disc/track order...
                    Collections.sort(matchingMp3s, new Comparator<Mp3Info>() {
                        @Override public int compare(Mp3Info lhs, Mp3Info rhs) {
                            return lhs.compareToByOrder(rhs);
                        }
                    });
                    // ...and then "group by" album.
                    Collections.sort(matchingMp3s, new Comparator<Mp3Info>() {
                        @Override public int compare(Mp3Info lhs, Mp3Info rhs) {
                            return lhs.compareToByAlbum(rhs);
                        }
                    });
                    
                    page.append("<form name='add_form' action='/add' method='post'>");
                    appendMp3Table(page, matchingMp3s, false);
                    if (!matchingMp3s.isEmpty()) {
                        page.append("<p><input type='submit' value='Add to Queue'>");
                    }
                    page.append("</form>\n");
                }
                
                page.append("<div id='footer'>");
                page.append("<p>Copyright &copy; 2009 <a href='http://www.jessies.org/~enh/'>Elliott Hughes</a>.</p>");
                page.append("</div>");
                
                page.append("</body>\n");
                page.append("</html>\n");
                
                HttpUtilities.sendResponse(t, HttpURLConnection.HTTP_OK, page.toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private class StaticHandler implements HttpHandler {
        private HashMap<String, byte[]> staticContents = new HashMap<String, byte[]>();
        
        public void put(String url, byte[] content) {
            staticContents.put(url, content);
        }
        
        public void put(String url, File file) throws IOException {
            byte[] content = ByteBufferUtilities.readFile(file).array();
            staticContents.put(url, content);
        }
        
        public void handle(HttpExchange t) throws IOException {
            final byte[] content = staticContents.get(t.getRequestURI().getPath());
            if (content != null) {
                HttpUtilities.sendResponse(t, HttpURLConnection.HTTP_OK, content);
            } else {
                HttpUtilities.send404(t);
            }
        }
    }
    
    private Mp3Info findMp3(int id) {
        synchronized (allMp3s) {
            for (Mp3Info mp3 : allMp3s) {
                if (mp3.id == id) {
                    return mp3;
                }
            }
        }
        return null;
    }
    
    private class AddHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            try {
                // Read the form input.
                BufferedReader in = new BufferedReader(new InputStreamReader(t.getRequestBody(), Charset.forName("UTF-8")));
                String line;
                while ((line = in.readLine()) != null) {
                    for (String nameAndValue : line.split("&")) {
                        final int id = Integer.parseInt(nameAndValue.substring(nameAndValue.indexOf('=') + 1));
                        playQueue.put(findMp3(id));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            
            HttpUtilities.sendSeeOther(t, "/");
        }
    }
    
    private class RemoveHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            final Mp3Info mp3 = requestedMp3(t);
            
            if (mp3.equals(nowPlaying)) {
                // Stop the player...
                player.destroy();
                try {
                    // ...and wait for it to stop before redirecting the user's browser, so they get to see the new state of the play queue.
                    player.waitFor();
                } catch (InterruptedException ex) {
                }
            } else {
                playQueue.remove(mp3);
            }
            
            HttpUtilities.sendSeeOther(t, "/");
        }
    }
    
    private Mp3Info requestedMp3(HttpExchange t) throws NumberFormatException {
        final String path = t.getRequestURI().getPath();
        final int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        return findMp3(id);
    }
    
    private String getResourceFilename(String leafName) {
        return System.getProperty("org.jessies.projectRoot") + File.separator + "lib" + File.separator + "data" + File.separator + leafName;
    }
    
    private Mp3d(List<File> musicDirectories) throws Exception {
        this.musicDirectories = musicDirectories;
        
        // Index all the mp3s.
        final List<File> mp3files = findMp3Files(musicDirectories);
        this.allMp3s = scanId3v2Tags(mp3files);
        
        // Set up the static files we need to serve.
        final StaticHandler staticHandler = new StaticHandler();
        
        staticHandler.put("/static/mp3d.css", new File(getResourceFilename("mp3d.css")));
        staticHandler.put("/static/mp3d.js", new File(getResourceFilename("mp3d.js")));
        staticHandler.put("/static/add.png", new File("/usr/share/icons/gnome/16x16/actions/gtk-add.png"));
        staticHandler.put("/static/play.png", new File("/usr/share/icons/gnome/16x16/actions/gtk-media-play-ltr.png"));
        staticHandler.put("/static/remove.png", new File("/usr/share/icons/gnome/16x16/actions/gtk-remove.png"));
        
        // Start the HTTP server.
        final HttpServer server = HttpServer.create(new InetSocketAddress(MP3D_PORT), 0);
        server.createContext("/", new MainHandler());
        server.createContext("/add", new AddHandler());
        server.createContext("/remove/", new RemoveHandler());
        server.createContext("/static/", staticHandler);
        server.setExecutor(null);
        server.start();
        
        // Start the mp3 player thread.
        new Thread(new PlayQueueRunnable()).start();
        
        // Announce the HTTP server via DNS-SD.
        final int portNumber = server.getAddress().getPort();
        ProcessUtilities.spawn(null, "/usr/bin/avahi-publish", "-f", "-s", "mp3d", "_http._tcp", Integer.toString(portNumber));
    }
    
    private class PlayQueueRunnable implements Runnable {
        public void run() {
            while (true) {
                try {
                    System.err.println("Waiting for something to play...");
                    nowPlaying = playQueue.take();
                    
                    // For greater control over playback, we might want to use Java mp3 playing code:
                    // http://www.javazoom.net/javalayer/docs/docs1.0/index.html
                    
                    System.err.println("Playing " + nowPlaying + "...");
                    try {
                        // FIXME: if we're interrupted, we start another mplayer off which doesn't die when we do.
                        player = new ProcessBuilder().command("/usr/bin/mplayer", "-quiet", "-msglevel", "all=1", nowPlaying.filename).start();
                        player.waitFor();
                    } catch (Exception ex) {
                        Log.warn("Failed to spawn mplayer", ex);
                    } finally {
                        nowPlaying = null;
                        player = null;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    private List<Mp3Info> scanId3v2Tags(List<File> mp3files) {
        final long t0 = System.nanoTime();
        final ArrayList<Mp3Info> mp3s = new ArrayList<Mp3Info>();
        for (File mp3file : mp3files) {
            try {
                Mp3Info mp3 = new Id3v2Scanner(mp3file).scanId3v2Tags();
                if (mp3 != null) {
                    //System.err.println(mp3);
                    mp3s.add(mp3);
                }
            } catch (Exception ex) {
                System.err.println("Problem with " + mp3file);
                ex.printStackTrace();
            }
        }
        final long t1 = System.nanoTime();
        System.err.println("Scanned " + mp3s.size() + " .mp3 files' ID3v2 tags in " + TimeUtilities.nsToString(t1 - t0));
        return mp3s;
    }
    
    private List<File> findMp3Files(List<File> musicDirectories) {
        final long t0 = System.nanoTime();
        final List<File> mp3files = new ArrayList<File>();
        for (File directory : musicDirectories) {
            findMp3Files(directory, mp3files);
        }
        final long t1 = System.nanoTime();
        System.err.println("Found " + mp3files.size() + " .mp3 files in " + TimeUtilities.nsToString(t1 - t0));
        return mp3files;
    }
    
    private void findMp3Files(File directory, List<File> result) {
        // FIXME: parallelize?
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                findMp3Files(file, result);
            } else if (file.toString().endsWith(".mp3")) {
                result.add(file);
            } else {
                //System.err.println("warning: '" + file + "' is neither a directory nor a .mp3 file");
            }
        }
    }
    
    private void appendMp3Table(StringBuilder out, Collection<Mp3Info> mp3s, boolean isQueue) {
        // If there's only one choice, select it automatically to save the user a click.
        final String checked = (mp3s.size() == 1) ? " checked" : "";
        
        out.append("<table width='100%'>\n");
        out.append("<thead>\n");
        final String col1 = isQueue ? "&nbsp;" : "<input type='checkbox' onclick='checkAll(this);'" + checked + ">";
        out.append(" <tr> <th width='24px'>" + col1 + "</th> <th>Title</th> <th width='20%'>Artist</th> <th width='20%'>Album</th> </tr>\n");
        out.append("</thead>\n");
        out.append("<tbody>\n");
        int rowCount = 0;
        for (Mp3Info mp3 : mp3s) {
            out.append((rowCount % 2) == 0 ? "<tr>" : "<tr class='alt'>");
            out.append("<td>");
            if (isQueue) {
                out.append("<a href='/remove/" + mp3.id + "'><img src='/static/remove.png'></a>");
            } else {
                out.append("<input type='checkbox' name='id' value='" + mp3.id + "'" + checked + ">");
            }
            out.append("</td>");
            // FIXME: html escape these strings!
            out.append("<td>" + mp3.title + "</td>");
            out.append("<td>" + mp3.artist + "</td>");
            out.append("<td>" + mp3.album + "</td>");
            out.append("</tr>\n");
            ++rowCount;
        }
        if (rowCount == 0) {
            out.append("<tr><td></td> <td>(Nothing.)</td> <td></td> <td></td> </tr>\n");
        }
        out.append("</tbody>\n");
        out.append("</table>\n");
    }
    
    private static void usage() {
        System.err.println("usage: mp3d MP3DIR...");
        System.exit(1);
    }
    
    public static void main(String[] args) throws Exception {
        // It's traditional on Mac OS and Linux to keep mp3s in ~/Music.
        if (args.length == 0) {
            args = new String[] { "~/Music" };
        }
        
        // Check the arguments are all directories.
        final ArrayList<File> musicDirectories = new ArrayList<File>();
        for (String arg : args) {
            File dir = FileUtilities.fileFromString(arg);
            if (!dir.exists() || !dir.isDirectory()) {
                usage();
            }
            musicDirectories.add(dir);
        }
        
        // Start the server.
        new Mp3d(musicDirectories);
    }
}
