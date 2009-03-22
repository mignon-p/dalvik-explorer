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
import java.io.*;
import java.net.*;
import java.util.zip.GZIPOutputStream;

public class HttpUtilities {
    public static void sendSeeOther(HttpExchange t, String newLocation) throws IOException {
        t.getResponseHeaders().set("Location", newLocation);
        sendResponse(t, HttpURLConnection.HTTP_SEE_OTHER, new byte[0]);
    }
    
    public static void send404(HttpExchange t) throws IOException {
        sendFailure(t, HttpURLConnection.HTTP_NOT_FOUND, "Not Found");
    }
    
    public static void sendFailure(HttpExchange t, int code, String message) throws IOException {
        sendResponse(t, code, "<html><body><h1>" + code + " " + message + "</h1></body></html>");
    }
    
    public static void sendResponse(HttpExchange t, int code, String string) throws IOException {
        t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        sendResponse(t, code, string.getBytes("UTF-8"));
    }
    
    public static void sendResponse(HttpExchange t, int code, byte[] bytes) throws IOException {
        // FIXME: check user agent accepts gzip content encoding?
        t.getResponseHeaders().set("Content-Encoding", "gzip");
        t.sendResponseHeaders(code, 0);
        final GZIPOutputStream os = new GZIPOutputStream(t.getResponseBody());
        os.write(bytes);
        os.finish();
        t.close();
    }
}
