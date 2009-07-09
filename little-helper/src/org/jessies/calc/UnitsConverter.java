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

import java.util.regex.*;
import org.jessies.test.*;

public class UnitsConverter {
    
    public UnitsConverter() {
    }
    
    public static String convert(String s) {
        // Temperature?
        final Matcher temperatureMatcher = Pattern.compile("^(-?[\\d.]+) *([CF])").matcher(s);
        if (temperatureMatcher.matches()) {
            final double originalValue = Double.parseDouble(temperatureMatcher.group(1));
            final char originalUnit = temperatureMatcher.group(2).charAt(0);
            switch (originalUnit) {
            case 'C':
                return String.format("%.1f F", 32.0 + (9.0 * originalValue / 5.0));
            case 'F':
                return String.format("%.1f C", 5.0 * (originalValue - 32.0) / 9.0);
            }
        }
        
        // Imperial length?
        // First try to normalize.
        final String maybeImperialLength = s.replaceAll("f(?:eet|oot|t)", "'").replaceAll("in(?:ches|ch)?", "\"");
        final String maybeMetricLength = convertImperial("'", 12.0, "\"", 0.0254, "m", maybeImperialLength);
        if (maybeMetricLength != null) {
            return maybeMetricLength;
        }
        
        // FIXME: Imperial distances?
        // 200 miles / 200 mi
        
        // FIXME: Metric length?
        // 1.37m
        // 1m 37cm
        // 2 meters
        // 24.3 cm
        // 90.7 mm
        // 200 km
        
        // Imperial Weight?
        // First try to normalize.
        final String maybeImperialWeight = s.replaceAll("(?:pound|lb)s?", "lb").replaceAll("(?:ounces|ounce|oz)", "oz");
        final String maybeMetricWeight = convertImperial("lb", 16.0, "oz", 0.0283495231, "kg", maybeImperialWeight);
        if (maybeMetricWeight != null) {
            return maybeMetricWeight;
        }
        
        // 2.27kg
        // 36.8 grams
        // 36.8 g
        
        // FIXME: Currency?
        
        return null;
    }
    
    private static String convertImperial(String bigUnit, double smallUnitsPerBigUnit, String smallUnit, double toMetric, String metricUnit, String input) {
        final Matcher imperialMatcher = Pattern.compile("^(?:([\\d.]+) *" + bigUnit + ")? *(?:(([\\d.]+)) *" + smallUnit + ")?").matcher(input);
        if (!imperialMatcher.matches()) {
            return null;
        }
        String bigValue = imperialMatcher.group(1);
        if (bigValue == null) {
            bigValue = "0";
        }
        String smallValue = imperialMatcher.group(2);
        if (smallValue == null) {
            smallValue = "0";
        }
        final double value = (smallUnitsPerBigUnit * Double.parseDouble(bigValue)) + Double.parseDouble(smallValue);
        // FIXME: choose an appropriate SI prefix and precision based on the input.
        return String.format("%.2f %s", toMetric * value, metricUnit);
    }
    
    @Test private static void testTemperatureConversion() {
        Assert.equals(convert("0C"), "32.0 F");
        Assert.equals(convert("-40C"), "-40.0 F");
        Assert.equals(convert("100C"), "212.0 F");
        Assert.equals(convert("100.0C"), "212.0 F");
        Assert.equals(convert("100 C"), "212.0 F");
        Assert.equals(convert("100.0 C"), "212.0 F");
        Assert.equals(convert("32 F"), "0.0 C");
        Assert.equals(convert("78F"), "25.6 C");
    }
    
    @Test private static void testImperialLengthConversion() {
        Assert.equals(convert("13.3\""), "0.34 m");
        Assert.equals(convert("13.3 \""), "0.34 m");
        Assert.equals(convert("13.3 in"), "0.34 m");
        Assert.equals(convert("13.3 inch"), "0.34 m");
        Assert.equals(convert("13.3 inches"), "0.34 m");
        
        Assert.equals(convert("6 '"), "1.83 m");
        Assert.equals(convert("6 foot"), "1.83 m");
        Assert.equals(convert("6 feet"), "1.83 m");
        Assert.equals(convert("6 ft"), "1.83 m");
        Assert.equals(convert("6 ft 0 in"), "1.83 m");
        
        Assert.equals(convert("5'4\""), "1.63 m");
        Assert.equals(convert("5' 4\""), "1.63 m");
        Assert.equals(convert("5 ' 4\""), "1.63 m");
        Assert.equals(convert("5 ' 4 \""), "1.63 m");
        Assert.equals(convert("5 feet 4 inches"), "1.63 m");
        Assert.equals(convert("5feet 4inches"), "1.63 m");
        Assert.equals(convert("5feet4inches"), "1.63 m");
        Assert.equals(convert("5ft 4in"), "1.63 m");
        Assert.equals(convert("5 ft 4 in"), "1.63 m");
    }
    
    @Test private static void testImperialWeightConversion() {
        Assert.equals(convert("5.0 pound"), "2.27 kg");
        Assert.equals(convert("5.0pound"), "2.27 kg");
        Assert.equals(convert("5.0 pounds"), "2.27 kg");
        Assert.equals(convert("5.0pounds"), "2.27 kg");
        Assert.equals(convert("5 lb"), "2.27 kg");
        Assert.equals(convert("5lb"), "2.27 kg");
        Assert.equals(convert("5 lbs"), "2.27 kg");
        Assert.equals(convert("5lbs"), "2.27 kg");
        
        Assert.equals(convert("1.3 ounces"), "0.04 kg");
        Assert.equals(convert("1.3 oz"), "0.04 kg");
    }
}
