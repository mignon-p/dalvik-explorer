package org.jessies.mathdroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import java.math.*;
import org.jessies.calc.*;

public class PlotView extends View {
    private static final String TAG = "PlotView";
    
    private Calculator calculator;
    private CalculatorPlotData plotData;
    
    public PlotView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void preparePlot(Calculator calculator, CalculatorPlotData plotData) {
        this.calculator = calculator;
        this.plotData = plotData;
    }
    
    @Override protected void onDraw(Canvas cv) {
//        android.os.Debug.startMethodTracing("mathdroid-plot");
        Log.i(TAG, "padding " + getPaddingTop() + " " + getPaddingRight() + " " + getPaddingBottom() + " " + getPaddingLeft());
        
        // FIXME: for some reason, getHeight includes the space under the status bar, which isn't very useful.
        Rect clipBounds = cv.getClipBounds();
        int width = clipBounds.width();
        int height = clipBounds.height();
        
        float xScale = 300/*width*/ / plotData.xRange;
        float yScale = height / plotData.yRange;
        
        //Log.i(TAG, "width="+width+" height="+height);
        //Log.i(TAG, "matrix="+cv.getMatrix()+" clip="+cv.getClipBounds());
        
        //Log.i(TAG, "yMin="+yMin+" yMax="+yMax);
        //Log.i(TAG, "xRange="+xRange+" yRange="+yRange);
        //Log.i(TAG, "yScale="+yScale);
        
        Paint p = new Paint();
        p.setFlags(p.getFlags() | Paint.ANTI_ALIAS_FLAG);
        
        // Axes.
        p.setColor(Color.WHITE);
        p.setStrokeWidth(0.0f);
        // X axis.
        if (0 > plotData.yMin && 0 < plotData.yMax) {
            float y = (float)(height - (0 - plotData.yMin) * yScale);
            cv.drawLine(0, y, width, y, p);
        }
        // Y axis.
        if (0 > plotData.xMin && 0 < plotData.xMax) {
            float x = (0 - plotData.xMin) * xScale;
            cv.drawLine(x, 0, x, height, p);
        }
        // FIXME: ticks.
        // FIXME: axis labels.
        
        // Curve.
        p.setColor(Color.GREEN);
        p.setStrokeWidth(2.0f);
        float screenX = 0.0f;
        float screenY = (float) (height - (plotData.data[0] - plotData.yMin) * yScale);
        for (int i = 1; i < plotData.data.length; ++i) {
            float newScreenY = (float) (height - (plotData.data[i] - plotData.yMin) * yScale);
            //System.err.println("drawLine("+screenX+", "+screenY+", "+(screenX + 1)+", "+newScreenY+")");
            if (!Float.isNaN(screenY) && !Float.isNaN(newScreenY)) {
                cv.drawLine(screenX, (float)(height - (plotData.data[i - 1] - plotData.yMin) * yScale), screenX + 1, newScreenY, p);
            }
            screenY = newScreenY;
            ++screenX;
        }
        
        // Title.
        //cv.drawText(expression, 32, 32, p);
//        android.os.Debug.stopMethodTracing();
    }
}
