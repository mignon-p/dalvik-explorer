package org.jessies.p9term;

import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Ties together the subprocess reader thread, the subprocess writer thread, and the thread that processes the subprocess' output.
 * Some basic processing is done here.
 */
public class TerminalControl {
    // Andrew Giddings wanted "windows-1252" for his Psion.
    private static final String CHARSET_NAME = "UTF-8";
    
    // This should be around your system's pipe size.
    // Too much larger and you'll waste time copying unused char[].
    // Too much smaller and you'll waste time making excessive system calls reading just part of what's available.
    // FIXME: add a JNI call to return PIPE_BUF? (It's not strictly required to be the value we're looking for, but it probably is.)
    private static final int INPUT_BUFFER_SIZE = 8192;
    
    // We use "new String" here because we're going to use reference equality later to recognize Terminator-supplied defaults.
    private static final String DEFAULT_SHELL = new String(System.getenv("SHELL"));
    
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_STEP_MODE = false;
    private static final boolean SHOW_ASCII_RENDITION = false;
    
    private static BufferedReader stepModeReader;
    
    private PTextArea textArea;
    private JTerminalPane pane;
    private TerminalFrame terminalFrame;
    private PtyProcess ptyProcess;
    private boolean processIsRunning;
    private boolean processHasBeenDestroyed = false;
    
    private InputStreamReader in;
    private OutputStream out;
    
    private ExecutorService writerExecutor;
    private Thread readerThread;
    
    private boolean automaticNewline;
    
    private StringBuilder lineBuffer = new StringBuilder();
    
    private EscapeParser escapeParser;
    
    // Buffer of TerminalActions to perform.
    private ArrayList<TerminalAction> terminalActions = new ArrayList<TerminalAction>();
    // Semaphore to prevent us from overrunning the EDT.
    private Semaphore flowControl = new Semaphore(30);
    
    public TerminalControl(JTerminalPane pane, PTextArea textArea) {
        reset();
        this.pane = pane;
        this.textArea = textArea;
    }
    
    public void initProcess(List<String> command, String workingDirectory) throws Throwable {
        // We always want to start a login shell.
        // This used to be an option, but it wasn't very useful and it caused confusion.
        // It's also hard to explain the difference without assuming a detailed knowledge of the particular shell.
        // We used to use "--login", but "-l" was more portable.
        // tcsh(1) is so broken that "-l" can only appear on its own.
        // POSIX's sh(1) doesn't even have the notion of login shell (though it does specify vi-compatible line editing).
        // So now we use the 1970s trick of prefixing argv[0] with "-".
        String[] argv = command.toArray(new String[command.size()]);
        String executable = argv[0];
        // We deliberately use reference equality here so we're sure we know what we're meddling with.
        // We only want to modify a call to the user's default shell that Terminator itself inserted into 'command'.
        // If the user's messing about with -e, they get what they ask for no matter what that is.
        // Since we only support -e for compatibility purposes, it's important to have a compatible implementation!
        if (argv[0] == DEFAULT_SHELL) {
            argv[0] = "-" + argv[0];
        }
        
        this.ptyProcess = new PtyProcess(executable, argv, workingDirectory);
        this.processIsRunning = true;
        Log.warn("Created " + ptyProcess);
        this.in = new InputStreamReader(ptyProcess.getInputStream(), CHARSET_NAME);
        this.out = ptyProcess.getOutputStream();
        writerExecutor = ThreadUtilities.newSingleThreadExecutor(makeThreadName("Writer"));
    }
    
    public static ArrayList<String> getDefaultShell() {
        ArrayList<String> command = new ArrayList<String>();
        command.add(DEFAULT_SHELL);
        return command;
    }
    
    public void destroyProcess() {
        if (processIsRunning) {
            try {
                ptyProcess.destroy();
                processHasBeenDestroyed = true;
            } catch (IOException ex) {
                Log.warn("Failed to destroy process " + ptyProcess, ex);
            }
        }
    }
    
    /**
     * Starts listening to output from the child process. This method is
     * invoked when all the user interface stuff is set up.
     */
    public void start(TerminalFrame terminalFrame) {
        this.terminalFrame = terminalFrame;
        
        if (readerThread != null) {
            // Detaching a tab causes start to be invoked again, but we shouldn't do anything.
            return;
        }
        
        if (ptyProcess == null) {
            // If the PtyProcess couldn't start, there's no point carrying on.
            return;
        }
        
        readerThread = startThread("Reader", new ReaderRunnable());
    }
    
    private Thread startThread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable, makeThreadName(name));
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
    
    private String makeThreadName(String role) {
        return "Process " + ptyProcess.getProcessId() + " (" + ptyProcess.getPtyName() + ") " + role;
    }
    
    private class ReaderRunnable implements Runnable {
        public void run() {
            try {
                while (true) {
                    char[] chars = new char[INPUT_BUFFER_SIZE];
                    int readCount = in.read(chars, 0, chars.length);
                    if (readCount == -1) {
                        Log.warn("read returned -1 from " + ptyProcess);
                        return; // This isn't going to fix itself!
                    }
                    
                    try {
                        processBuffer(chars, readCount);
                    } catch (Throwable th) {
                        Log.warn("Problem processing output from " + ptyProcess, th);
                    }
                }
            } catch (Throwable th) {
                Log.warn("Problem reading output from " + ptyProcess, th);
            } finally {
                // Our reader might throw an exception before the child has terminated.
                // So "handleProcessTermination" is perhaps not the ideal name.
                handleProcessTermination();
            }
        }
    }
    
    public void setAutomaticNewline(boolean automatic) {
        this.automaticNewline = automatic;
    }
    
    public boolean isAutomaticNewline() {
        return automaticNewline;
    }
    
    /**
     * Invoked both on construction to set the defaults and by the "Reset"
     * action in the UI.
     */
    public void reset() {
        setAutomaticNewline(false);
        System.err.println("FIXME: reset style in TerminalControl.reset");
        //model.setStyle(StyledText.getDefaultStyle());
    }
    
    private static final void doStep() {
        if (DEBUG_STEP_MODE) {
            try {
                if (stepModeReader == null) {
                    stepModeReader = new BufferedReader(new InputStreamReader(System.in));
                }
                stepModeReader.readLine();
            } catch (IOException ex) {
                Log.warn("Problem waiting for stepping input", ex);
            }
        }
    }
    
    private void handleProcessTermination() {
        processIsRunning = false;

        // The readerThread will have shut itself down by now.
        // We need to handle the writer ExecutorService ourselves.
        if (writerExecutor != null) {
            writerExecutor.shutdownNow();
        }
        
        // If the JNI side failed to start, ptyProcess can be null.
        // In that case, we'll already have reported the error.
        if (ptyProcess == null) {
            return;
        }

        Log.warn("calling waitFor on " + ptyProcess);
        try {
            ptyProcess.waitFor();
        } catch (Exception ex) {
            Log.warn("Problem waiting for " + ptyProcess, ex);
            String exceptionDetails = StringUtilities.stackTraceFromThrowable(ex).replaceAll("\n", "\n\r");
            announceConnectionLost(exceptionDetails + "[Problem waiting for process.]");
            return;
        }
        Log.warn("waitFor returned on " + ptyProcess);
        if (ptyProcess.didExitNormally()) {
            int status = ptyProcess.getExitStatus();
            if (pane.shouldHoldOnExit(status)) {
                announceConnectionLost("\n\r[Process exited with status " + status + ".]");
                return;
            }
        } else if (ptyProcess.wasSignaled()) {
            announceConnectionLost("\n\r[Process killed by " + ptyProcess.getSignalDescription() + ".]");
            return;
        } else {
            announceConnectionLost("\n\r[Lost contact with process.]");
            return;
        }

        // If it wasn't a pane close that caused us to get here, close the pane.
        if (processHasBeenDestroyed == false) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    pane.doCloseAction();
                }
            });
        }
    }
    
    public void announceConnectionLost(String message) {
        try {
            final char[] buffer = message.toCharArray();
            processBuffer(buffer, buffer.length);
        } catch (Exception ex) {
            Log.warn("Couldn't say \"" + message + "\"", ex);
        }
    }
    
    private synchronized void processBuffer(char[] buffer, int size) throws IOException {
        boolean sawNewline = false;
        for (int i = 0; i < size; ++i) {
            char ch = buffer[i];
            if (ch == '\n') {
                sawNewline = true;
            }
            processChar(ch);
        }
        flushLineBuffer();
        flushTerminalActions();
        
        if (pane.isShowing() == false) {
            pane.getOutputSpinner().setPainted(true);
            pane.getOutputSpinner().animateOneFrame();
        }
    }
    
    private synchronized void flushTerminalActions() {
        if (terminalActions.size() == 0) {
            return;
        }
        
        final TerminalAction[] actions = terminalActions.toArray(new TerminalAction[terminalActions.size()]);
        terminalActions.clear();
        
        boolean didAcquire = false;
        try {
            flowControl.acquire();
            didAcquire = true;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        for (TerminalAction action : actions) {
                            action.perform(terminalFrame);
                        }
                    } catch (Throwable th) {
                        Log.warn("Couldn't process terminal actions for " + ptyProcess, th);
                    } finally {
                        flowControl.release();
                    }
                }
            });
        } catch (Throwable th) {
            Log.warn("Couldn't flush terminal actions for " + ptyProcess, th);
            if (didAcquire) {
                flowControl.release();
            }
        }
    }
    
    /**
     * According to vttest, these cursor movement characters are still
     * treated as such, even when they occur within an escape sequence.
     */
    private final boolean countsTowardsEscapeSequence(char ch) {
        return (ch != Ascii.BS && ch != Ascii.CR && ch != Ascii.VT);
    }
    
    private synchronized void processChar(final char ch) {
        // Enable this if you're having trouble working out what we're being asked to interpret.
        if (SHOW_ASCII_RENDITION) {
            if (ch >= ' ' || ch == '\n') {
                System.out.print(ch);
            } else {
                System.out.print(".");
            }
        }
        
        if (ch == Ascii.ESC) {
            flushLineBuffer();
            // If the old escape sequence is interrupted; we start a new one.
            if (escapeParser != null) {
                Log.warn("Escape parser discarded with string \"" + escapeParser + "\"");
            }
            escapeParser = new EscapeParser();
            return;
        }
        if (escapeParser != null && countsTowardsEscapeSequence(ch)) {
            escapeParser.addChar(ch);
            if (escapeParser.isComplete()) {
                processEscape();
                escapeParser = null;
            }
        } else if (ch == Ascii.CR) {
            // Do we care? Assume we already saw an LF and dealt with that.
        } else if (ch == Ascii.BS) {
            if (lineBuffer.length() > 1) {
                lineBuffer.deleteCharAt(lineBuffer.length() - 1);
            } else {
                flushLineBuffer();
                doStep();
                processSpecialCharacter(ch);
            }
        } else if (ch == Ascii.BEL) {
            pane.flash();
        } else if (ch == Ascii.NUL) {
            // Most telnetd(1) implementations seem to have a bug whereby
            // they send the NUL byte at the end of the C strings they want to
            // output when you first connect. Since all Unixes are pretty much
            // copy and pasted from one another these days, this silly mistake
            // only needed to be made once.
        } else {
            lineBuffer.append(ch);
        }
    }
    
    private static class PlainTextAction implements TerminalAction {
        private String line;
        
        private PlainTextAction(String line) {
            this.line = line;
        }
        
        public void perform(TerminalFrame terminal) {
            if (DEBUG) {
                Log.warn("Processing line \"" + line + "\"");
            }
            terminal.processLine(line);
        }
        
        public String toString() {
            return "TerminalAction[Process line: " + line + "]";
        }
    }
    
    private synchronized void flushLineBuffer() {
        if (lineBuffer.length() == 0) {
            // Nothing to flush!
            return;
        }
        
        final String line = lineBuffer.toString();
        lineBuffer = new StringBuilder();
        
        doStep();
        
        // Conform to the stated claim that the model's always mutated in the AWT dispatch thread.
        terminalActions.add(new PlainTextAction(line));
    }
    
    public synchronized void processSpecialCharacter(final char ch) {
        terminalActions.add(new TerminalAction() {
            public void perform(TerminalFrame terminal) {
                if (DEBUG) {
                    Log.warn("Processing special char \"" + getCharDesc(ch) + "\"");
                }
                terminal.processSpecialCharacter(ch);
            }
            
            public String toString() {
                return "TerminalAction[Special char " + getCharDesc(ch) + "]";
            }
            
            private String getCharDesc(char ch) {
                switch (ch) {
                    case Ascii.LF: return "LF";
                    case Ascii.CR: return "CR";
                    case Ascii.HT: return "HT";
                    case Ascii.VT: return "VT";
                    case Ascii.BS: return "BS";
                    default: return "??";
                }
            }
        });
    }
    
    public synchronized void processEscape() {
        if (DEBUG) {
            Log.warn("Processing escape sequence \"" + StringUtilities.escapeForJava(escapeParser.toString()) + "\"");
        }
        
        // Invoke all escape sequence handling in the AWT dispatch thread - otherwise we'd have
        // to create billions upon billions of tiny little invokeLater(Runnable) things all over the place.
        doStep();
        TerminalAction action = escapeParser.getAction(this);
        if (action != null) {
            terminalActions.add(action);
        }
    }
    
    public void sendUtf8String(final String s) {
        writerExecutor.execute(new Runnable() {
            public void run() {
                try {
                    if (processIsRunning) {
                        out.write(s.getBytes(CHARSET_NAME));
                        out.flush();
                    }
                } catch (IOException ex) {
                    reportFailedSend("string", s, ex);
                }
            }
        });
    }
    
    private void reportFailedSend(String kind, String value, Exception ex) {
        Log.warn("Couldn't send " + kind + " \"" + StringUtilities.escapeForJava(value) + "\" to " + ptyProcess, ex);
    }
    
    public PtyProcess getPtyProcess() {
        return ptyProcess;
    }
}
