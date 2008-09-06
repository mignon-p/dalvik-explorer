package org.jessies.p9term;

import e.util.*;

/**
Parses 'CSI' escape sequences.  Such sequences always have '[' as their first character,
and then are sometimes followed by a '?' character, then optionally a list of numbers
separated by ';' characters, followed by the final character which tells us what to do with
all that stuff.

@author Phil Norman
*/

public class CSIEscapeAction implements TerminalAction {
    private TerminalControl control;
    private String sequence;
    
    public CSIEscapeAction(TerminalControl control, String sequence) {
        this.control = control;
        this.sequence = sequence;
    }

    public void perform(TerminalFrame terminal) {
        if (processSequence(terminal) == false) {
            Log.warn("Unimplemented escape sequence: \"" + StringUtilities.escapeForJava(sequence) + "\"");
        }
    }
    
    private String getSequenceType(char lastChar) {
        switch (lastChar) {
        case 'A': return "Cursor up";
        case 'B': return "Cursor down";
        case 'C': return "Cursor right";
        case 'c': return "Device attributes request";
        case 'D': return "Cursor left";
        case 'd': return "Move cursor to row";
        case 'G':
        case '`': return "Move cursor column to";
        case 'f':
        case 'H': return "Move cursor to";
        case 'K': return "Kill line contents";
        case 'J': return "Kill lines";
        case 'L': return "Insert lines";
        case 'M': return "Delete lines";
        case 'P': return "Delete characters";
        case 'g': return "Clear tabs";
        case 'h': return "Set DEC private mode";
        case 'l': return "Clear DEC private mode";
        case 'm': return "Set font, color, etc";
        case 'n': return "Device status report";
        case 'r': return "Restore DEC private modes or set scrolling region";
        case 's': return "Save DEC private modes";
        default: return "Unknown:" + lastChar;
        }
    }
    
    public String toString() {
        char lastChar = sequence.charAt(sequence.length() - 1);
        return "CSIEscapeAction[" + getSequenceType(lastChar) + "]";
    }
    
    private boolean processSequence(TerminalFrame terminal) {
        char lastChar = sequence.charAt(sequence.length() - 1);
        String midSequence = sequence.substring(1, sequence.length() - 1);
        switch (lastChar) {/*
        case 'A':
            return moveCursor(model, midSequence, 0, -1);
        case 'B':
            return moveCursor(model, midSequence, 0, 1);
        case 'C':
            return moveCursor(model, midSequence, 1, 0);
        case 'c':
            return deviceAttributesRequest(midSequence);
        case 'D':
            return moveCursor(model, midSequence, -1, 0);
        case 'd':
            return moveCursorRowTo(model, midSequence);
        case 'G':
        case '`':
            return moveCursorColumnTo(model, midSequence);
        case 'f':
        case 'H':
            return moveCursorTo(model, midSequence);
        case 'K':
            return killLineContents(model, midSequence);
        case 'P':
            return deleteCharacters(model, midSequence);
        case 'h':
            return setDecPrivateMode(model, midSequence, true);
        case 'l':
            return setDecPrivateMode(model, midSequence, false);
        case 'm':
            return processFontEscape(model, midSequence);
*/
        default:
            Log.warn("unknown CSI sequence " + StringUtilities.escapeForJava(sequence));
            return false;
        }
    }
    
    /*
    public boolean processFontEscape(TerminalModel model, String sequence) {
        int oldStyle = model.getStyle();
        int foreground = StyledText.getForeground(oldStyle);
        int background = StyledText.getBackground(oldStyle);
        boolean isBold = StyledText.isBold(oldStyle);
        boolean isReverseVideo = StyledText.isReverseVideo(oldStyle);
        boolean isUnderlined = StyledText.isUnderlined(oldStyle);
        boolean hasForeground = StyledText.hasForeground(oldStyle);
        boolean hasBackground = StyledText.hasBackground(oldStyle);
        String[] chunks = sequence.split(";");
        for (String chunk : chunks) {
            int value = (chunk.length() == 0) ? 0 : Integer.parseInt(chunk);
            switch (value) {
            case 0:
                // Clear all attributes.
                hasForeground = false;
                hasBackground = false;
                isBold = false;
                isReverseVideo = false;
                isUnderlined = false;
                break;
            case 1:
                isBold = true;
                break;
            case 2:
                // ECMA-048 says "faint, decreased intensity or second colour".
                // gnome-terminal implements this as grey.
                // xterm does nothing.
                break;
            case 4:
                isUnderlined = true;
                break;
            case 5:
                // Blink on. Unsupported.
                break;
            case 7:
                isReverseVideo = true;
                break;
            case 21:
                // The xwsh man page suggests this should disable bold.
                // ECMA-048 says it turns on double-underlining.
                // xterm does nothing.
                // gnome-terminal treats this the same as 22.
                break;
            case 22:
                // ECMA-048 says "normal colour or normal intensity (neither bold nor faint)".
                // xterm clears the bold flag.
                // gnome-terminal clears the bold and half-intensity flags.
                isBold = false;
                break;
            case 24:
                isUnderlined = false;
                break;
            case 25:
                // Blink off. Unsupported.
                break;
            case 27:
                isReverseVideo = false;
                break;
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
                // Set foreground color.
                foreground = value - 30;
                hasForeground = true;
                break;
            case 39:
                // Use default foreground color.
                hasForeground = false;
                break;
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
                // Set background color.
                background = value - 40;
                hasBackground = true;
                break;
            case 49:
                // Use default background color.
                hasBackground = false;
                break;
            default:
                Log.warn("Unknown attribute " + value + " in [" + StringUtilities.escapeForJava(sequence));
                break;
            }
        }
        model.setStyle(StyledText.getStyle(foreground, hasForeground, background, hasBackground, isBold, isUnderlined, isReverseVideo));
        return true;
    }
    */
}
