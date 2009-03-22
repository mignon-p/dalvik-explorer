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

import java.util.*;

public final class Mp3Info {
    // Where can we find this track on disk?
    public final String filename;
    
    // These come from the ID3 tags.
    public final String album;
    public final String artist;
    public final String disc;
    public final String title;
    public final String track;
    
    // Used to sort keep tracks in their album order in search results.
    private final int order;
    
    // Used to speed up searching.
    private final List<String> lowercaseData;
    
    public final int id;
    
    public Mp3Info(String title, String artist, String album, String disc, String track, String filename) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.filename = filename;
        this.disc = fixDiscOrTrack(disc);
        this.track = fixDiscOrTrack(track);
        
        this.order = Integer.parseInt(this.disc) * 100 + Integer.parseInt(this.track);
        this.lowercaseData = Arrays.asList(title.toLowerCase(), artist.toLowerCase(), album.toLowerCase());
        
        // We use a hash, and use these ids rather than indexes, in the hope that it enables bookmarks.
        // FIXME: is this useful?
        this.id = toString().hashCode();
    }
    
    private String fixDiscOrTrack(String s) {
        // We sometimes get empty data. Default to "1" so downstream code knows that disc and track are always numbers.
        if (s == null || s.length() == 0) {
            return "1";
        }
        // We usually get data like "7/9", but we only want to know that this is the seventh of nine.
        s = s.replaceAll("/\\d+$", "");
        return s;
    }
    
    public boolean matches(List<String> terms) {
        for (String term : terms) {
            if (!matchesTerm(term)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean matchesTerm(String term) {
        for (String datum : lowercaseData) {
            if (datum.contains(term)) {
                return true;
            }
        }
        return false;
    }
    
    public int compareToByOrder(Mp3Info rhs) {
        return order - rhs.order;
    }
    
    public int compareToByAlbum(Mp3Info rhs) {
        return album.compareTo(rhs.album);
    }
    
    @Override public boolean equals(Object o) {
        if (!(o instanceof Mp3Info)) {
            return false;
        }
        final Mp3Info rhs = (Mp3Info) o;
        return filename.equals(rhs.filename) && album.equals(rhs.album) && artist.equals(rhs.artist) && disc.equals(rhs.disc) && title.equals(rhs.title) && track.equals(rhs.track);
    }
    
    @Override public String toString() {
        return "Mp3Info[title='" + title + "',artist='" + artist + "'album='" + album + "',disc=" + disc + ",track=" + track + ",order=" + order + ",id=" + id + " (" + filename + ")]";
    }
}
