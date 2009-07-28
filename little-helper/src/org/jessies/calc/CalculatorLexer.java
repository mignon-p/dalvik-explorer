package org.jessies.calc;

/*
 * This file is part of LittleHelper.
 * Copyright (C) 2009 Elliott Hughes <enh@jessies.org>.
 * 
 * LittleHelper is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Talc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.*;
import java.math.*;
import java.util.*;
import static org.jessies.calc.BigDecimals.*;

public class CalculatorLexer {
    private static final int EOF = -1;
    
    private final boolean DEBUG_LEXER = false;
    
    private final MyPushbackReader reader;
    
    private CalculatorToken token;
    private String identifier;
    private BigDecimal number;
    
    public CalculatorLexer(String expression) {
        this(new StringReader(expression), null);
    }
    
    public CalculatorLexer(File file) throws IOException {
        this(new InputStreamReader(new FileInputStream(file)), file);
    }
    
    private CalculatorLexer(Reader reader, File file) {
        this.reader = new MyPushbackReader(new BufferedReader(reader), file);
        nextToken();
    }
    
    public void nextToken() {
        try {
            token = nextToken0();
            if (DEBUG_LEXER) {
                System.err.println("nextToken() => " + token);
            }
        } catch (IOException ex) {
            throw new CalculatorError("I/O error: " + ex.toString());
        }
    }
    
    private CalculatorToken maybe(char expectedChar, CalculatorToken yesToken, CalculatorToken noToken) throws IOException {
        int ch = reader.read();
        if (ch == expectedChar) {
            return yesToken;
        }
        reader.unread(ch);
        return noToken;
    }
    
    private CalculatorToken nextToken0() throws IOException {
        int ch;
        
        // Skip whitespace and control characters.
        while ((ch = reader.read()) != EOF && ch <= ' ') {
        }
        
        switch (ch) {
        case EOF: return CalculatorToken.END_OF_INPUT;
            
        case '(': return CalculatorToken.OPEN_PARENTHESIS;
        case ',': return CalculatorToken.COMMA;
        case ')': return CalculatorToken.CLOSE_PARENTHESIS;
            
        case '=': return maybe('=', CalculatorToken.EQ, CalculatorToken.ASSIGN);
        case '+': return CalculatorToken.PLUS;
        case '-': return CalculatorToken.MINUS;
        case '/': return CalculatorToken.DIV;
        case '%': return CalculatorToken.MOD;
        case '!': return maybe('=', CalculatorToken.NE, CalculatorToken.PLING);
        case '~': return CalculatorToken.B_NOT;
        case '^': return CalculatorToken.POW;
            
        case '&': return CalculatorToken.B_AND;
        case '|': return CalculatorToken.B_OR;
        case '*': return CalculatorToken.MUL;
            
        case '\u221a': return CalculatorToken.SQRT;
            
        case '<':
            {
                int ch2 = reader.read();
                if (ch2 == '<') {
                    return CalculatorToken.SHL;
                } else if (ch2 == '=') {
                    return CalculatorToken.LE;
                } else {
                    reader.unread(ch2);
                    return CalculatorToken.LT;
                }
            }
        case '>':
            {
                int ch2 = reader.read();
                if (ch2 == '>') {
                    return CalculatorToken.SHR;
                } else if (ch2 == '=') {
                    return CalculatorToken.GE;
                } else {
                    reader.unread(ch2);
                    return CalculatorToken.GT;
                }
            }
        
        default:
            if ((ch >= '0' && ch <= '9') || ch == '.') {
                // Number.
                StringBuilder text = new StringBuilder();
                
                // Work out the base.
                int base = 10;
                if (ch == '0') {
                    int ch2 = reader.read();
                    if (ch2 == 'x') {
                        base = 16;
                    } else if (ch2 == 'o') {
                        base = 8;
                    } else if (ch2 == 'b') {
                        base = 2;
                    } else {
                        reader.unread(ch2);
                    }
                }
                
                boolean isReal = (ch == '.');
                while (ch != EOF && (isValidDigit((char) ch, base) || (base == 10 && ch == '.'))) {
                    text.append((char) ch);
                    if (ch == '.') {
                        isReal = true;
                    }
                    ch = reader.read();
                }
                
                if (ch == 'E') {
                    isReal = true; // FIXME: this is a hack so we go via the BigDecimal constructor which supports this notation.
                    text.append('E');
                    ch = reader.read();
                    if (ch == '-' || ch == '+') {
                        text.append((char) ch);
                        ch = reader.read();
                    }
                    while (ch != EOF && (isValidDigit((char) ch, 10))) {
                        text.append((char) ch);
                        ch = reader.read();
                    }
                }
                reader.unread(ch);
                
                if (isReal) {
                    number = /*BigDecimals.*/fromString(text.toString());
                } else {
                    number = new BigDecimal(new BigInteger(text.toString(), base), /*BigDecimals.*/MATH_CONTEXT);
                }
                
                return CalculatorToken.NUMBER;
            } else if (isIdentifierStartCharacter(ch)) {
                // Identifier.
                StringBuilder text = new StringBuilder();
                while (ch != EOF && isIdentifierCharacter(ch)) {
                    text.append((char) ch);
                    ch = reader.read();
                }
                reader.unread(ch);
                identifier = text.toString();
                return CalculatorToken.IDENTIFIER;
            } else {
                throw new CalculatorError("invalid character '" + ensurePrintable(ch) + "' in input");
            }
        }
    }
    
    public static String ensurePrintable(int ch) {
        if (ch >= ' ' && ch <= '~') {
            return  String.valueOf((char) ch);
        } else {
            return String.format("\\u%04x", ch);
        }
    }
    
    private static boolean isValidDigit(char ch, int base) {
        if (base <= 10) {
            return (ch >= '0' && ch < ('0' + base));
        } else {
            if (ch >= '0' && ch <= '9') {
                return true;
            } else {
                ch = Character.toLowerCase(ch);
                return (ch >= 'a' && ch < ('a' + base - 10));
            }
        }
    }
    
    private static boolean isIdentifierStartCharacter(int ch) {
        return Character.isJavaIdentifierStart(ch);
    }
    
    private static boolean isIdentifierCharacter(int ch) {
        return Character.isJavaIdentifierPart(ch);
    }
    
    public CalculatorToken token() {
        return token;
    }
    
    public String identifier() {
        if (token != CalculatorToken.IDENTIFIER) {
            throw new CalculatorError("Lexer.identifier called when current token was " + token);
        }
        return identifier;
    }
    
    public BigDecimal number() {
        if (token != CalculatorToken.NUMBER) {
            throw new CalculatorError("Lexer.number called when current token was " + token);
        }
        return number;
    }
    
    /**
     * Like the JDK PushbackReader, but with a larger default pushback buffer, and more intelligent behavior when pushing back EOF.
     */
    private static class MyPushbackReader extends FilterReader {
        // The pushback buffer.
        private static final int BUFFER_SIZE = 8;
        private final char[] buf = new char[BUFFER_SIZE];
        private int pos = buf.length;
        
        // What file we're reading, or null if we're reading from a non-file source (such as a string).
        private final File file;
        
        // Humans count lines from 1, and these are for error reporting.
        private int lineNumber = 1;
        private int columnNumber = 1;
        
        public MyPushbackReader(Reader in, File file) {
            super(in);
            this.file = file;
        }
        
        @Override public int read() throws IOException {
            synchronized (lock) {
                int result = (pos < buf.length) ? buf[pos++] : super.read();
                ++columnNumber;
                if (result == '\n') {
                    ++lineNumber;
                    columnNumber = 1;
                }
                return result;
            }
        }
        
        public void unread(int c) {
            if (c == EOF || c < ' ') {
                // Don't push back stuff we'll only skip again anyway.
                // In particular, pushing back \n to causes trouble because we don't know what column that puts us in.
                return;
            }
            synchronized (lock) {
                if (pos == 0) {
                    throw new CalculatorError("pushback buffer overflow");
                }
                --columnNumber;
                buf[--pos] = (char) c;
            }
        }
        
        @Override public boolean ready() throws IOException {
            synchronized (lock) {
                return (pos < buf.length) || super.ready();
            }
        }
        
        @Override public void mark(int readAheadLimit) throws IOException { throw new UnsupportedOperationException(); }
        @Override public boolean markSupported() { return false; }
        @Override public int read(char cbuf[], int off, int len) throws IOException { throw new UnsupportedOperationException(); }
        @Override public void reset() throws IOException { throw new UnsupportedOperationException(); }
        @Override public long skip(long n) throws IOException { throw new UnsupportedOperationException(); }
    }
}
