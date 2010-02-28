/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jessies.icalendar;

import java.util.*;
import java.util.regex.*;
import org.jessies.test.*;

public class IcsUtils {
    private IcsUtils() {}
    
    /**
     * Parses an RFC 5545 UTC offset and returns milliseconds.
     * 
     * Grammar:
     *        utc-offset = time-numzone
     *        time-numzone = ("+" / "-") time-hour time-minute [time-second]
     */
    public static int parseUtcOffset(String value) {
        final Pattern utcOffsetPattern = Pattern.compile("([-+])(\\d{2})(\\d{2})(?:\\d{2})?");
        final Matcher matcher = utcOffsetPattern.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(value);
        }
        int sign = matcher.group(1).equals("+") ? 1 : -1;
        int hours = Integer.parseInt(matcher.group(2));
        int minutes = Integer.parseInt(matcher.group(3));
        int offsetSeconds = sign * (hours*60 + minutes)*60;
        return offsetSeconds * 1000;
    }
    
    @Test private static void testParseUtcOffset() {
        Assert.equals(-18000000, parseUtcOffset("-0500"));
        Assert.equals(-18000000, parseUtcOffset("-050000"));
        Assert.equals(3600000, parseUtcOffset("+0100"));
    }
    
    /**
     * Returns the constant from java.util.Calendar corresponding to the given two-letter day of the week.
     */
    public static int parseDayName(String twoLetterDayName) {
        if (twoLetterDayName.equals("SU")) {
            return Calendar.SUNDAY;
        } else if (twoLetterDayName.equals("MO")) {
            return Calendar.MONDAY;
        } else if (twoLetterDayName.equals("TU")) {
            return Calendar.TUESDAY;
        } else if (twoLetterDayName.equals("WE")) {
            return Calendar.WEDNESDAY;
        } else if (twoLetterDayName.equals("TH")) {
            return Calendar.THURSDAY;
        } else if (twoLetterDayName.equals("FR")) {
            return Calendar.FRIDAY;
        } else if (twoLetterDayName.equals("SA")) {
            return Calendar.SATURDAY;
        } else {
            throw new IllegalArgumentException(twoLetterDayName);
        }
    }
    
    /**
     * Removes escaping from an RFC 5545 "text".
     * 
     *        ESCAPED-CHAR = ("\\" / "\;" / "\," / "\N" / "\n")
     *        ; \\ encodes \
     *        ; \N or \n encodes newline
     *        ; \; encodes ;
     *        ; \, encodes ,
     */
    public static String unescape(String s) {
        if (s.indexOf('\\') == -1) {
            return s;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            if (ch == '\\' && i < s.length() - 1) {
                char ch2 = s.charAt(++i);
                if (ch2 == 'n' || ch2 == 'N') {
                    ch2 = '\n';
                }
                result.append(ch2);
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
    
    @Test private static void testUnescape() {
        Assert.equals("hello, world!", unescape("hello, world!"));     // unchanged
        Assert.equals("hello\n world!", unescape("hello\n world!"));   // unchanged
        // These are the only special cases:
        Assert.equals("hello\n world!", unescape("hello\\n world!"));  // \n
        Assert.equals("hello\n world!", unescape("hello\\N world!"));  // \N
        // These are the only valid escapes:
        Assert.equals("hello\\ world!", unescape("hello\\\\ world!")); // \\
        Assert.equals("hello, world!", unescape("hello\\, world!"));   // \,
        Assert.equals("hello; world!", unescape("hello\\; world!"));   // \;
        // The RFC says any other character after a '\' is an error, but we just let the character through:
        Assert.equals("invalid", unescape("in\\valid"));
    }
    
    /**
     * Returns the value of the first property with name 'propertyName' in 'component', or null.
     * The value will have had any escaped characters unescaped.
     */
    public static String getFirstPropertyText(ICalendar.Component component, String propertyName) {
        ICalendar.Property property = component.getFirstProperty(propertyName);
        return (property != null) ? IcsUtils.unescape(property.getValue()) : null;
    }
}
