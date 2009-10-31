package org.jessies.mathdroid;

/**
 * At the moment, a history item is a "question" (the user's input) and a string "answer".
 * In future, answers might not be just strings.
 */
public class HistoryItem {
    String question;
    String answer;
    
    public HistoryItem(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }
}
