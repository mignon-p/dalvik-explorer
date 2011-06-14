package org.jessies.calc;

/*
 * This file is part of org.jessies.calc.
 * Copyright (C) 2009 Elliott Hughes <enh@jessies.org>.
 * 
 * LittleHelper is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class CalculatorPlotData {
    public double[] data;
    
    public float xMin;
    public float xMax;
    public float xRange;
    
    public double yMin;
    public double yMax;
    public float yRange;
    
    public CalculatorPlotData(int pixelWidth, RealNode xMin, RealNode xMax) {
        this.data = new double[pixelWidth];
        this.xMin = (float) xMin.doubleValue();
        this.xMax = (float) xMax.doubleValue();
        
        this.xRange = this.xMax - this.xMin;
    }
    
    private CalculatorPlotData() {
    }
    
    public void calculateRange() {
        yMin = Float.POSITIVE_INFINITY;
        yMax = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < data.length; ++i) {
            double value = data[i];
            //System.err.println(value);
            if (value < yMin) {
                yMin = value;
            }
            if (value > yMax) {
                yMax = value;
            }
        }
        // FIXME: scale to nice round numbers.
        // FIXME: clip really large (positive or negative) values; (x+1)^2/(x-1) near x==1, for instance.
        yRange = (float)(yMax - yMin);
    }
    
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(data.length);
        result.append('\n');
        for (int i = 0; i < data.length; ++i) {
            result.append(data[i]);
            result.append('\n');
        }
        result.append(xMin);
        result.append('\n');
        result.append(xMax);
        result.append('\n');
        result.append(xRange);
        result.append('\n');
        result.append(yMin);
        result.append('\n');
        result.append(yMax);
        result.append('\n');
        result.append(yRange);
        result.append('\n');
        return result.toString();
    }
    
    public static CalculatorPlotData fromString(String s) {
        CalculatorPlotData result = new CalculatorPlotData();
        String[] fields = s.split("\n");
        int field = 0;
        final int length = Integer.parseInt(fields[field++]);
        result.data = new double[length];
        for (int i = 0; i < length; ++i) {
            result.data[i] = Double.parseDouble(fields[field++]);
        }
        result.xMin = Float.parseFloat(fields[field++]);
        result.xMax = Float.parseFloat(fields[field++]);
        result.xRange = Float.parseFloat(fields[field++]);
        result.yMin = Double.parseDouble(fields[field++]);
        result.yMax = Double.parseDouble(fields[field++]);
        result.yRange = Float.parseFloat(fields[field++]);
        return result;
    }
}
