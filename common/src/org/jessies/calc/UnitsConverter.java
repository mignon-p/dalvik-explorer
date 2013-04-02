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

import java.util.Locale;
import java.util.regex.*;

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
                return String.format(Locale.US, "%.1f F", 32.0 + (9.0 * originalValue / 5.0));
            case 'F':
                return String.format(Locale.US, "%.1f C", 5.0 * (originalValue - 32.0) / 9.0);
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
        return String.format(Locale.US, "%.2f %s", toMetric * value, metricUnit);
    }
}
