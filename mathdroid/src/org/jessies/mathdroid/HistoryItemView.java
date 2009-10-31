package org.jessies.mathdroid;

import android.content.*;
import android.view.*;
import android.widget.*;

/**
 * A view for the items in the history transcript.
 * Basically just two text views: one for the question, another for the answer.
 */
public class HistoryItemView extends LinearLayout {
    private final TextView mQuestionView;
    private final TextView mAnswerView;
    
    public HistoryItemView(Context context, HistoryItem item) {
        super(context);
        
        this.setOrientation(VERTICAL);
        
        mQuestionView = new TextView(context);
        mQuestionView.setTextAppearance(context, R.style.history_question_appearance);
        mQuestionView.setText(item.question);
        addView(mQuestionView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        
        mAnswerView = new TextView(context);
        mAnswerView.setText(" = " + item.answer);
        addView(mAnswerView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
    }
    
    // Allows reuse of a recycled HistoryItemView.
    public void setItem(HistoryItem item) {
        mQuestionView.setText(item.question);
        mAnswerView.setText(" = " + item.answer);
    }
}
