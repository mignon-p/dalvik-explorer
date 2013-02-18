package org.jessies.mathdroid;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.webkit.*;
import java.io.*;
import java.util.*;
import org.jessies.calc.*;

/**
 * Displays our help text in a WebView.
 */
public class MathdroidHelp extends Activity {
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.help);
    getWebView().loadData(readAssetFile("help.html"), "text/html; charset=UTF-8", null);
  }

  @Override public void onBackPressed() {
    WebView webView = getWebView();
    if (webView.canGoBack()) {
      webView.goBack();
    } else {
      super.onBackPressed();
    }
  }

  private WebView getWebView() {
    return ((WebView) findViewById(R.id.help_webview));
  }

  private String readAssetFile(String filename) {
    StringBuilder result = new StringBuilder();
    BufferedReader in = null;
    try {
      in = new BufferedReader(new InputStreamReader(getResources().getAssets().open(filename, Context.MODE_WORLD_READABLE)));
      String line;
      while ((line = in.readLine()) != null) {
        if (line.equals("$$BUILT_IN_FUNCTION_LIST$$")) {
          result.append(makeBuiltInFunctionList());
        } else {
          result.append(line);
        }
      }
    } catch (IOException ex) {
      result.append("<p><font color='red'>" + ex.getMessage() + "</font></p>");
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ignored) {
        }
      }
    }
    return result.toString();
  }

  private String makeBuiltInFunctionList() {
    Calculator c = new Calculator();

    TreeMap<String, CalculatorFunction> nameToFunction = new TreeMap<String, CalculatorFunction>(String.CASE_INSENSITIVE_ORDER);
    for (String functionName : c.getFunctionNames()) {
      if (functionName.charAt(0) < 0x7f) { // Deliberately ignore Sigma and Pi.
        nameToFunction.put(functionName, c.getFunction(functionName));
      }
    }

    HashMap<CalculatorFunction, String> operators = new HashMap<CalculatorFunction, String>();
    for (CalculatorToken operator : c.getOperators()) {
      operators.put(c.getFunction(operator), operator.name);
    }

    StringBuilder result = new StringBuilder();
    char lastChar = '\u0000';
    result.append("<ul>\n");
    Map.Entry<String, CalculatorFunction> e;
    while ((e = nameToFunction.pollFirstEntry()) != null) {
      String name = e.getKey();
      if (Character.toLowerCase(name.charAt(0)) != lastChar) {
        result.append("\n<li>");
        lastChar = Character.toLowerCase(name.charAt(0));
      } else {
        result.append(", ");
      }
      result.append(name);
      String operatorName = operators.get(e.getValue());
      if (operatorName != null) {
        result.append(" (" + operatorName + ")");
      }
    }
    result.append("</ul>\n");
    return result.toString();
  }
}
