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

public class IcsUtils {
    private IcsUtils() {}
    
    /**
     * Parses an RFC 5545 UTC offset and returns milliseconds.
     * 
     * Grammar:
     *        utc-offset = time-numzone
     *        time-numzone = ("+" / "-") time-hour time-minute [time-second]
     * 
     * Examples:
     * -0500
     * +0100
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
}
