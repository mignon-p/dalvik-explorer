package org.jessies.mathdroid;

import org.jessies.calc.Node;

/**
 * At the moment, a history item is a "question" (the user's input) and a Node "answer".
 */
public class HistoryItem {
  String question;
  Node answer;

  public HistoryItem(String question, Node answer) {
    this.question = question;
    this.answer = answer;
  }
}
