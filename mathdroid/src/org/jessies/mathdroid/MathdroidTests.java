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

public class MathdroidTests extends Activity {
  private static final int BLACK = 0xff000000;
  private static final int GREEN = 0xff00ff00;
  private static final int RED   = 0xffff0000;
  private static final int WHITE = 0xffffffff;

  private TestState mTestState;

  public static class TestState implements TestListener {
    public Activity mUi;

    private TestSuite mSuite;
    private long mCurrentTestStartMs;
    private int mTestSuiteSize;
    private ArrayList<Test> mFinishedTests = new ArrayList<Test>();
    private ArrayList<Long> mTestDurationsMs = new ArrayList<Long>();
    private ArrayList<Test> mFailedTests = new ArrayList<Test>();
    private ArrayList<Throwable> mFailures = new ArrayList<Throwable>();

    public TestState(Activity ui, Class<?>... classes) {
      mUi = ui;
      mSuite = new TestSuite(classes);
      mTestSuiteSize = mSuite.countTestCases();
    }

    public void runTests() {
      textView().setText("");

      TestResult result = new TestResult();
      result.addListener(this);

      // Let junit run the tests.
      long t0 = System.currentTimeMillis();
      mSuite.run(result);
      long t1 = System.currentTimeMillis();

      updateAtEnd(t1 - t0);
    }

    public synchronized void addError(Test test, Throwable failure) {
      mFailedTests.add(test);
      mFailures.add(failure);
    }

    public void addFailure(Test test, AssertionFailedError failure) {
      addError(test, failure);
    }

    public synchronized void startTest(Test test) {
      mCurrentTestStartMs = System.currentTimeMillis();
    }

    private synchronized boolean isFailure(Test t) {
      return mFailedTests.contains(t);
    }

    public synchronized void endTest(Test test) {
      mFinishedTests.add(test);

      long endMs = System.currentTimeMillis();
      long durationMs = (endMs - mCurrentTestStartMs);
      mTestDurationsMs.add(durationMs);

      appendTestInfo(test, durationMs);
      updateProgressBar();
      updateSummary();
    }

    private void appendTestInfo(Test test, long durationMs) {
      String testName = test.toString().replaceAll("test(.*)\\((.*)\\)", "$2.$1");
      boolean failed = isFailure(test);

      String text = (failed ? "[<font color='#ff0000'>FAIL</font>]" : "[<font color='#00ff00'>PASS</font>]") + " " + testName;
      if (durationMs > 1000) {
        text += " (<font color='#ffff00'>" + prettyDurationMs(durationMs) + "</font>)";
      }
      text += "<br>\n";

      if (failed) {
        // TODO: this is probably more useful in logcat than in the UI.
        Throwable failure = mFailures.get(mFailedTests.size() - 1);
        text += stackTrace(failure);
      }

      appendHtml(text);
    }

    private TextView summary() {
      return (TextView) mUi.findViewById(R.id.test_summary);
    }

    private ProgressBar progressBar() {
      return (ProgressBar) mUi.findViewById(R.id.test_progress);
    }

    private TextView textView() {
      return (TextView) mUi.findViewById(R.id.test_results);
    }

    public synchronized void updateProgressBar() {
      progressBar().post(new Runnable() {
        public void run() {
          // For some reason, calling setMax from onCreate didn't always work. (On JB.)
          progressBar().setMax(mTestSuiteSize);
          progressBar().setProgress(mFinishedTests.size());
        }
      });
    }

    public synchronized void updateSummary() {
      int failureCount = mFailures.size();
      int passCount = mFinishedTests.size() - failureCount;
      String summary = String.format(Locale.US, "Passed: %d/%d  Failed: %d", passCount, mTestSuiteSize, failureCount);
      setSummary(mFailedTests.isEmpty() ? GREEN : RED, summary);
    }

    public synchronized void updateOnRotation(Activity ui) {
      mUi = ui;

      // Refill the main text view.
      for (int i = 0; i < mFinishedTests.size(); ++i) {
        appendTestInfo(mFinishedTests.get(i), mTestDurationsMs.get(i));
      }

      updateProgressBar();
      updateSummary();
    }

    public synchronized void updateAtEnd(long durationMs) {
      progressBar().post(new Runnable() {
        public void run() {
          progressBar().setVisibility(View.GONE);
        }
      });

      if (mFailedTests.isEmpty()) {
        setSummary(GREEN, "All " + mFinishedTests.size() + " tests passed in " + prettyDurationMs(durationMs) + ".");
      }
    }

    private void setSummary(final int backgroundColor, final String text) {
      final TextView summary = summary();
      summary.post(new Runnable() {
        public void run() {
          summary.setText(text);
          summary.setBackgroundColor(backgroundColor);
          summary.setTextColor((backgroundColor == RED) ? WHITE : BLACK);
        }
      });
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
          result += lines[i] + "\n<br>";
        }
      }
      return result;
    }

    private static String prettyDurationMs(long ms) {
      return String.format(Locale.US, "%.1f s", ((double) ms) / 1000.0);
    }

    private void appendHtml(final String text) {
      final TextView textView = textView();
      textView.post(new Runnable() {
        public void run() {
          textView.append(Html.fromHtml(text));
          ((ScrollView) textView.getParent()).smoothScrollTo(0, textView.getBottom());
        }
      });
    }
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.level_3_diagnostics);

    mTestState = (TestState) getLastNonConfigurationInstance();
    if (mTestState == null) {
      // TODO: pass the classes in as an Intent extra.
      mTestState = new TestState(this, org.jessies.calc.CalculatorTest.class, org.jessies.calc.UnitsConverterTest.class);
      new Thread(new Runnable() {
        public void run() {
          mTestState.runTests();
        }
      }).start();
    } else {
      mTestState.updateOnRotation(this);
    }
  }

  @Override public Object onRetainNonConfigurationInstance() {
    return mTestState; // Remember our testing progress so far.
  }
}
