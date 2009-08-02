package org.jessies.mathdroid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewFlipper;
import java.util.HashMap;
import org.jessies.calc.Calculator;
import org.jessies.calc.CalculatorError;
import org.jessies.calc.UnitsConverter;

public class Mathdroid extends Activity implements TextView.OnEditorActionListener, View.OnClickListener {
    private static final String TAG = "Mathdroid";
    
    // Constants for the options menu items.
    private static final int OPTIONS_MENU_HELP = 0;
    
    private final Calculator calculator = new Calculator();
    
    private final HashMap<Integer, String> buttonMap = new HashMap<Integer, String>();
    
    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final EditText queryView = (EditText) findViewById(R.id.q);
        queryView.setOnEditorActionListener(this);
        
        initButtonMap();
        
        ((Button) findViewById(R.id.more)).setOnClickListener(this);
        ((Button) findViewById(R.id.less)).setOnClickListener(this);
        
        ((ImageButton) findViewById(R.id.del)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.exe)).setOnClickListener(this);
        
        for (int id : buttonMap.keySet()) {
            ((Button) findViewById(id)).setOnClickListener(this);
        }
        
        loadState();
    }
    
    private void initButtonMap() {
        buttonMap.put(R.id.digit0, "0");
        buttonMap.put(R.id.digit1, "1");
        buttonMap.put(R.id.digit2, "2");
        buttonMap.put(R.id.digit3, "3");
        buttonMap.put(R.id.digit4, "4");
        buttonMap.put(R.id.digit5, "5");
        buttonMap.put(R.id.digit6, "6");
        buttonMap.put(R.id.digit7, "7");
        buttonMap.put(R.id.digit8, "8");
        buttonMap.put(R.id.digit9, "9");
        buttonMap.put(R.id.eng,    "E");
        buttonMap.put(R.id.dot,    ".");
        buttonMap.put(R.id.ans,    "Ans");
        buttonMap.put(R.id.plus,   "+");
        buttonMap.put(R.id.minus,  "-");
        buttonMap.put(R.id.times,  "\u00d7");
        buttonMap.put(R.id.divide, "\u00f7");
        buttonMap.put(R.id.pi,     "\u03c0");
        buttonMap.put(R.id.sqrt,   "\u221a");
        buttonMap.put(R.id.open,   "(");
        buttonMap.put(R.id.close,  ")");
        buttonMap.put(R.id.pow,    "^");
        buttonMap.put(R.id.log10,  "log10()");
        buttonMap.put(R.id.logE,   "logE()");
        buttonMap.put(R.id.sin,    "sin()");
        buttonMap.put(R.id.cos,    "cos()");
        buttonMap.put(R.id.tan,    "tan()");
        buttonMap.put(R.id.asin,   "asin()");
        buttonMap.put(R.id.acos,   "acos()");
        buttonMap.put(R.id.atan,   "atan()");
        buttonMap.put(R.id.e,      "e");
        buttonMap.put(R.id.pling,  "!");
        buttonMap.put(R.id.comma,  ",");
        buttonMap.put(R.id.x,      "x");
        buttonMap.put(R.id.ceil,   "ceil()");
        buttonMap.put(R.id.floor,  "floor()");
    }
    
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        menu.add(0, OPTIONS_MENU_HELP, 0, "Help").setIcon(android.R.drawable.ic_menu_help);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case OPTIONS_MENU_HELP:
            showHelp();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override public void onPause() {
        super.onPause();
        saveState();
    }
    
    public void onClick(View view) {
        final EditText queryView = (EditText) findViewById(R.id.q);
        final int id = view.getId();
        switch (id) {
        case R.id.del:
            del(queryView);
            break;
        case R.id.exe:
            exe(queryView);
            break;
        case R.id.more:
        case R.id.less:
            ((ViewFlipper) findViewById(R.id.flipper)).showNext();
            break;
        default:
            if (view instanceof Button) {
                buttonPressed(queryView, id);
            }
        }
    }
    
    // Invoked if the user hits the physical "return" key while the EditText has focus.
    public boolean onEditorAction(TextView queryView, int actionId, KeyEvent event) {
        if (event != null && event.getAction() == KeyEvent.ACTION_UP) {
            // We already handled the ACTION_DOWN event, and don't want to repeat the work.
            return true;
        }
        exe((EditText) queryView);
        return true;
    }
    
    private void buttonPressed(EditText queryView, int id) {
        // Insert the new text (by replacing the entire content, which is our only option).
        // If there's a selection, we overwrite it.
        final String newText = buttonMap.get(id);
        final String existingText = queryView.getText().toString();
        final int startOffset = queryView.getSelectionStart();
        final int endOffset = queryView.getSelectionEnd();
        queryView.setText(existingText.substring(0, startOffset) + newText + existingText.substring(endOffset));
        // Put the caret back in the right place.
        int newCaretOffset;
        if (newText.endsWith(")")) {
            // For functions, we automatically insert both parentheses, so we need to move the caret back between them.
            newCaretOffset = startOffset + newText.length() - 1;
        } else {
            newCaretOffset = startOffset + newText.length();
        }
        queryView.setSelection(newCaretOffset, newCaretOffset);
    }
    
    private void del(EditText queryView) {
        int startOffset = queryView.getSelectionStart();
        int endOffset = queryView.getSelectionEnd();
        final String existingText = queryView.getText().toString();
        if (existingText.length() == 0) {
            return;
        }
        if (startOffset == endOffset) {
            // Remove the character before the caret.
            --startOffset;
            queryView.setText(existingText.substring(0, startOffset) + existingText.substring(endOffset));
        } else {
            // Remove the selection.
            queryView.setText(existingText.substring(0, startOffset) + existingText.substring(endOffset));
        }
        queryView.setSelection(startOffset, startOffset);
    }
    
    private void exe(EditText queryView) {
        final String queryText = queryView.getText().toString().trim();
        if (queryText.length() == 0) {
            // FIXME: report an error?
            return;
        }
        
        final String answerText = computeAnswer(queryText);
        final TextView answerView = (TextView) findViewById(R.id.a);
        answerView.setText(answerText);
        ((EditText) queryView).selectAll();
        return;
    }
    
    private String computeAnswer(String query) {
        try {
            String answer = null;
            /*
             * // Convert bases.
             * NumberDecoder numberDecoder = new NumberDecoder(query);
             * if (numberDecoder.isValid()) {
             * for (String item : numberDecoder.toStrings()) {
             * model.addElement(item);
             * }
             * }
             */
            if (answer == null) {
                // Convert units.
                answer = UnitsConverter.convert(query);
            }
            if (answer == null) {
                // Evaluate mathematical expressions.
                answer = calculator.evaluate(query);
            }
            if (answer == null) {
                answer = "Dunno, mate.";
            }
            return answer;
        } catch (CalculatorError ex) {
            return "Error: " + ex.getMessage();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "What do you mean?";
        }
    }
    
    private void loadState() {
        Log.i(TAG, "Restoring state");
        final SharedPreferences state = getPreferences(MODE_PRIVATE);
        if (state.getInt("version", 0) != 1) {
            // We've never been run before, or the last run was an incompatible version.
            return;
        }
        
        final EditText queryView = (EditText) findViewById(R.id.q);
        final TextView answerView = (TextView) findViewById(R.id.a);
        queryView.setText(state.getString("query", ""));
        answerView.setText(state.getString("answer", ""));
    }
    
    private void saveState() {
        Log.i(TAG, "Saving state");
        final EditText queryView = (EditText) findViewById(R.id.q);
        final TextView answerView = (TextView) findViewById(R.id.a);
        
        final SharedPreferences.Editor state = getPreferences(MODE_PRIVATE).edit();
        state.putInt("version", 1);
        state.putString("query", queryView.getText().toString());
        state.putString("answer", answerView.getText().toString());
        state.commit();
    }
    
    private void showHelp() {
        startActivity(new Intent(this, MathdroidHelp.class));
    }
}
