package org.jessies.p9term;

import e.util.*;

/**
 * Recognizes escape sequences consisting of ASCII ESC followed by a single character.
 * Note that most of these are mainly of historical interest, even though some of them look similar to more common sequences.
 */
public class SingleCharEscapeAction implements TerminalAction {
    private TerminalControl control;
    private char escChar;
    
    public SingleCharEscapeAction(TerminalControl control, char escChar) {
        this.control = control;
        this.escChar = escChar;
    }

    public void perform(TerminalFrame terminal) {
        switch (escChar) {
            /*
            case 'D':  // Move the cursor down one line, scrolling if it reaches the bottom of scroll region.  Effectively NL.
                terminal.processSpecialCharacter('\n');
                break;
            case 'E':  // Move cursor to start of next line, scrolling if required.  Effectively CR,NL
                terminal.processSpecialCharacter('\r');
                terminal.processSpecialCharacter('\n');
                break;
            case 'c':  // Power on (full reset).
                terminal.fullReset();
                break;
            */
            default:
                Log.warn("Unrecognized single-character escape \"" + escChar + "\".");
        }
    }
    
    private String getType() {
        switch (escChar) {
        case '6': return "rxvt: scr_backindex (not supported)";
        case '7': return "Save cursor";
        case '8': return "Restore cursor";
        case '9': return "rxvt: scr_forwardindex (not supported)";
        case '=': return "Set private mode application keypad";
        case '>': return "Unset private mode application keypad";
        case 'D': return "Down one line";
        case 'E': return "Move to start of next line";
        case 'H': return "Set tab at cursor";
        case 'M': return "Cursor up one line";
        case 'Z': return "Send device attributes (obsolete)";
        case 'c': return "Full reset";
        case 'n': return "Invoke character set 2";
        case 'o': return "Invoke character set 3";
        case '|':
        case '}':
        case '~': return "Invoke G3, G2, G1 character sets as GR";
        default: return "Unrecognized:" + escChar;
        }
    }
    
    public String toString() {
        return "SingleCharEscapeAction[" + getType() + "]";
    }
    
    private void unsupported(String description) {
        Log.warn("Unsupported single-character escape \"" + escChar + "\" (" + description + ").");
    }
}
