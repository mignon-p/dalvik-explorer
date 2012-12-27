package org.jessies.mathdroid;

import android.app.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import junit.framework.*;

public class MathdroidTests extends Activity implements TestListener {
  private static final int BLACK = 0xff000000;
  private static final int GREEN = 0xff00ff00;
  private static final int RED   = 0xffff0000;
  private static final int WHITE = 0xffffffff;

  private TextView mSummary;

  private ProgressBar mProgressBar;

  private TextView mTextView;
  private int mTextLength;

  private long mCurrentTestStartMs;

  private int mTestSuiteSize;
  private int mTestCount;
  private ArrayList<Test> mFailedTests = new ArrayList<Test>();
  private ArrayList<Throwable> mFailures = new ArrayList<Throwable>();

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.level_3_diagnostics);

    mSummary = (TextView) findViewById(R.id.test_summary);
    mProgressBar = (ProgressBar) findViewById(R.id.test_progress);
    mTextView = (TextView) findViewById(R.id.test_results);

    mTextView.setText("");
    mTextLength = 0;

    // TODO: cope better with rotation.
    new Thread(new Runnable() {
      public void run() {
        performLevel3Diagnostic();
      }
    }).start();
  }

  private void performLevel3Diagnostic() {
    // TODO: pass the classes in as an Intent extra.
    TestSuite suite = new TestSuite(org.jessies.calc.CalculatorTest.class, org.jessies.calc.UnitsConverterTest.class);
    mTestSuiteSize = suite.countTestCases();

    TestResult result = new TestResult();
    result.addListener(this);

    // Let junit run the tests.
    long t0 = System.currentTimeMillis();
    suite.run(result);
    long t1 = System.currentTimeMillis();
    long durationMs = (t1 - t0);

    mProgressBar.post(new Runnable() {
      public void run() {
        mProgressBar.setVisibility(View.GONE);
      }
    });

    if (mFailedTests.isEmpty()) {
      setSummary(GREEN, "All " + mTestCount + " tests passed in " + prettyDurationMs(durationMs) + ".");
    }
  }

  public void addError(Test test, Throwable failure) {
    mFailedTests.add(test);
    mFailures.add(failure);
  }

  public void addFailure(Test test, AssertionFailedError failure) {
    addError(test, failure);
  }

  public void startTest(Test test) {
    mCurrentTestStartMs = System.currentTimeMillis();
  }

  public void endTest(Test test) {
    ++mTestCount;

    mProgressBar.post(new Runnable() {
      public void run() {
        // For some reason, calling setMax from onCreate didn't always work. (On JB.)
        mProgressBar.setMax(mTestSuiteSize);
        mProgressBar.setProgress(mTestCount);
      }
    });

    long endMs = System.currentTimeMillis();
    long durationMs = (endMs - mCurrentTestStartMs);

    boolean failed = !mFailedTests.isEmpty() && (mFailedTests.get(mFailedTests.size() - 1) == test);

    int color = (failed ? RED : GREEN);
    int start = mTextLength + 1;
    int end = start + 4;

    String testName = test.toString().replaceAll("test(.*)\\((.*)\\)", "$2.$1");

    String text = (failed ? "[FAIL]" : "[PASS]") + " " + testName;
    if (durationMs > 1000) {
      text += " (" + prettyDurationMs(durationMs) + ")";
    }
    text += "\n";

    if (failed) {
      // TODO: this is probably more useful in logcat than in the UI.
      Throwable failure = mFailures.get(mFailedTests.size() - 1);
      text += stackTrace(failure);
    }

    append(text);
    setColor(color, start, end);

    updateSummary();
  }

  private void updateSummary() {
    int failureCount = mFailures.size();
    int passCount = mTestCount - failureCount;
    String summary = String.format("Tested: %d  Passed: %d  Failed: %d", mTestCount, passCount, failureCount);
    setSummary(mFailedTests.isEmpty() ? GREEN : RED, summary);
  }

  private static String stackTrace(Throwable t) {
    StringWriter stringWriter = new StringWriter();
    t.printStackTrace(new PrintWriter(stringWriter));
    String[] lines = stringWriter.getBuffer().toString().split("\n");

    // Find the junit framework's first call into user code.
    int end = lines.length - 1;
    for (; end >= 0; --end) {
      if (lines[end].indexOf("java.lang.reflect.Method.invokeNative(Native Method)") != -1) {
        break;
      }
    }

    // Take all the user frames, but filter out the junit Assert machinery.
    String result = "";
    for (int i = 0; i < end; ++i) {
      if (lines[i].indexOf("junit.framework.Assert.") == -1) {
        result += lines[i] + "\n";
      }
    }
    return result;
  }

  private static String prettyDurationMs(long ms) {
    return String.format("%.1f s", ((double) ms) / 1000.0);
  }

  private void setSummary(final int backgroundColor, final String text) {
    mSummary.post(new Runnable() {
      public void run() {
        mSummary.setText(text);
        mSummary.setBackgroundColor(backgroundColor);
        mSummary.setTextColor((backgroundColor == RED) ? WHITE : BLACK);
      }
    });
  }

  private void append(final String text) {
    mTextLength += text.length();
    mTextView.post(new Runnable() {
      public void run() {
        mTextView.append(text);
        ((ScrollView) mTextView.getParent()).smoothScrollTo(0, mTextView.getBottom());
      }
    });
  }

  private void setColor(final int color, final int startIndex, final int endIndex) {
    mTextView.post(new Runnable() {
      public void run() {
        Spannable spannable = (Spannable) mTextView.getText();
        spannable.setSpan(new ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    });
  }
}
