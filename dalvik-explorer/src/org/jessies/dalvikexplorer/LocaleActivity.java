package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;

public class LocaleActivity extends TextViewActivity {
  protected String extraName() {
    return "org.jessies.dalvikexplorer.Locale";
  }

  protected CharSequence title(String localeName) {
    return "Locale \"" + localeName + "\"";
  }

  protected CharSequence content(String localeName) {
    return describeLocale(localeName);
  }

  private static Locale localeByName(String name) {
    if (name.isEmpty()) {
      return new Locale("", "", "");
    }

    int languageEnd = name.indexOf('_');
    if (languageEnd == -1) {
      return new Locale(name, "", "");
    }

    String language = name.substring(0, languageEnd);
    name = name.substring(languageEnd + 1);

    int countryEnd = name.indexOf('_');
    if (countryEnd == -1) {
      return new Locale(language, name, "");
    }

    String country = name.substring(0, countryEnd);
    String variant = name.substring(countryEnd + 1);

    return new Locale(language, country, variant);
  }

  static String describeLocale(String name) {
    final StringBuilder result = new StringBuilder();

    final Locale locale = localeByName(name);

    result.append("Display Name: " + locale.getDisplayName() + "\n");
    result.append("Localized Display Name: " + locale.getDisplayName(locale) + "\n");
    result.append('\n');

    if (locale.getLanguage().length() > 0) {
      result.append("Display Language: " + locale.getDisplayLanguage() + "\n");
      result.append("Localized Display Language: " + locale.getDisplayLanguage(locale) + "\n");
      result.append("2-Letter Language Code: " + locale.getLanguage() + "\n");
      result.append("3-Letter Language Code: " + locale.getISO3Language() + "\n");
      result.append('\n');
    }
    if (locale.getCountry().length() > 0) {
      result.append("Display Country: " + locale.getDisplayCountry() + "\n");
      result.append("Localized Display Country: " + locale.getDisplayCountry(locale) + "\n");
      result.append("2-Letter Country Code: " + locale.getCountry() + "\n");
      result.append("3-Letter Country Code: " + locale.getISO3Country() + "\n");
      result.append('\n');
    }
    if (locale.getVariant().length() > 0) {
      result.append("Display Variant: " + locale.getDisplayVariant() + "\n");
      result.append("Localized Display Variant: " + locale.getDisplayVariant(locale) + "\n");
      result.append("Variant Code: " + locale.getVariant() + "\n");
    }
    result.append('\n');

    result.append("Number Formatting\n\n");
    describeNumberFormat(result, "Decimal", NumberFormat.getInstance(locale), 1234.5, -1234.5);
    describeNumberFormat(result, "Integer", NumberFormat.getIntegerInstance(locale), 1234, -1234);
    describeNumberFormat(result, "Currency", NumberFormat.getCurrencyInstance(locale), 1234.5, -1234.5);
    describeNumberFormat(result, "Percent", NumberFormat.getPercentInstance(locale), 12.3);
    result.append('\n');
    result.append('\n');

    boolean hasLocaleData = hasLocaleData();

    if (!hasLocaleData) {
      result.append("Decimal Format Symbols\n\n");
      NumberFormat nf = NumberFormat.getInstance(locale);
      if (nf instanceof DecimalFormat) {
        describeDecimalFormatSymbols(result, ((DecimalFormat) nf).getDecimalFormatSymbols());
      } else {
        result.append("(Didn't expect " + nf.getClass() + ".)");
      }
    }

    Date now = new Date(); // FIXME: it might be more useful to always show a time in the afternoon, to make 24-hour patterns more obvious.
    result.append("Date/Time Formatting\n\n");
    describeDateFormat(result, "Full Date", DateFormat.getDateInstance(DateFormat.FULL, locale), now);
    describeDateFormat(result, "Long Date", DateFormat.getDateInstance(DateFormat.LONG, locale), now);
    describeDateFormat(result, "Medium Date", DateFormat.getDateInstance(DateFormat.MEDIUM, locale), now);
    describeDateFormat(result, "Short Date", DateFormat.getDateInstance(DateFormat.SHORT, locale), now);
    result.append('\n');
    describeDateFormat(result, "Full Time", DateFormat.getTimeInstance(DateFormat.FULL, locale), now);
    describeDateFormat(result, "Long Time", DateFormat.getTimeInstance(DateFormat.LONG, locale), now);
    describeDateFormat(result, "Medium Time", DateFormat.getTimeInstance(DateFormat.MEDIUM, locale), now);
    describeDateFormat(result, "Short Time", DateFormat.getTimeInstance(DateFormat.SHORT, locale), now);
    result.append('\n');
    describeDateFormat(result, "Full Date/Time", DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale), now);
    describeDateFormat(result, "Long Date/Time", DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale), now);
    describeDateFormat(result, "Medium Date/Time", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale), now);
    describeDateFormat(result, "Short Date/Time", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale), now);
    result.append('\n');
    result.append('\n');

    if (!hasLocaleData) {
      result.append("Date Format Symbols\n\n");
      DateFormat edf = DateFormat.getDateInstance(DateFormat.FULL, Locale.US);
      DateFormatSymbols edfs = ((SimpleDateFormat) edf).getDateFormatSymbols();
      DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, locale);
      DateFormatSymbols dfs = ((SimpleDateFormat) df).getDateFormatSymbols();
      result.append("Local Pattern Chars: " + dfs.getLocalPatternChars() + "\n");
      result.append("Am/pm: " + Arrays.toString(dfs.getAmPmStrings()) + "\n");
      result.append("Eras: " + Arrays.toString(dfs.getEras()) + "\n");
      result.append("Months: " + Arrays.toString(dfs.getMonths()) + "\n");
      result.append("Short Months: " + Arrays.toString(dfs.getShortMonths()) + "\n");
      result.append("Weekdays: " + Arrays.toString(dfs.getWeekdays()) + "\n");
      result.append("Short Weekdays: " + Arrays.toString(dfs.getShortWeekdays()) + "\n");
      result.append('\n');
      result.append('\n');
    }

    result.append("Calendar\n\n");
    Calendar c = Calendar.getInstance(locale);
    int firstDayOfWeek = c.getFirstDayOfWeek();
    String firstDayOfWeekString = new DateFormatSymbols(locale).getWeekdays()[firstDayOfWeek];
    String englishFirstDayOfWeekString = new DateFormatSymbols(Locale.US).getWeekdays()[firstDayOfWeek];
    result.append("First Day of the Week: " + firstDayOfWeek + " '" + firstDayOfWeekString + "'");
    if (!englishFirstDayOfWeekString.equals(firstDayOfWeekString)) {
      result.append(" (" + englishFirstDayOfWeekString + ")");
    }
    result.append('\n');
    result.append("Minimal Days in First Week: " + c.getMinimalDaysInFirstWeek() + "\n");
    result.append('\n');
    result.append('\n');

    // If this locale specifies a country, check out the currency.
    // Languages don't have currencies; countries do.
    if (!locale.getCountry().equals("")) {
      result.append("Currency\n\n");
      Currency currency = Currency.getInstance(locale);
      result.append("ISO 4217 Currency Code: " + currency.getCurrencyCode() + "\n");
      result.append("Currency Symbol: " + currency.getSymbol(locale) + " (" + currency.getSymbol(Locale.US) + ")\n");
      result.append("Default Fraction Digits: " + currency.getDefaultFractionDigits() + "\n");
      result.append('\n');
      result.append('\n');
    }

    result.append("Data Availability\n\n");
    appendAvailability(result, locale, "BreakIterator", BreakIterator.class);
    appendAvailability(result, locale, "Calendar", NumberFormat.class);
    appendAvailability(result, locale, "Collator", Collator.class);
    appendAvailability(result, locale, "DateFormat", DateFormat.class);
    appendAvailability(result, locale, "DateFormatSymbols", DateFormatSymbols.class);
    appendAvailability(result, locale, "DecimalFormatSymbols", DecimalFormatSymbols.class);
    appendAvailability(result, locale, "NumberFormat", NumberFormat.class);

    if (hasLocaleData) {
      result.append("\n\n");
      result.append("libcore.icu.LocaleData\n\n");
      try {
        Object enUsData = getLocaleDataInstance(Locale.US);
        Object localeData = getLocaleDataInstance(locale);

        describeStringArray(result, "amPm", enUsData, localeData, null);
        describeStringArray(result, "eras", enUsData, localeData, null);
        result.append('\n');

        String[] previous;
        previous = describeStringArray(result, "longMonthNames", enUsData, localeData, null);
        describeStringArray(result, "longStandAloneMonthNames", enUsData, localeData, previous);
        previous = describeStringArray(result, "shortMonthNames", enUsData, localeData, null);
        describeStringArray(result, "shortStandAloneMonthNames", enUsData, localeData, previous);
        previous = describeStringArray(result, "tinyMonthNames", enUsData, localeData, null);
        describeStringArray(result, "tinyStandAloneMonthNames", enUsData, localeData, previous);
        result.append('\n');

        previous = describeStringArray(result, "longWeekdayNames", enUsData, localeData, null);
        describeStringArray(result, "longStandAloneWeekdayNames", enUsData, localeData, previous);
        previous = describeStringArray(result, "shortWeekdayNames", enUsData, localeData, null);
        describeStringArray(result, "shortStandAloneWeekdayNames", enUsData, localeData, previous);
        previous = describeStringArray(result, "tinyWeekdayNames", enUsData, localeData, null);
        describeStringArray(result, "tinyStandAloneWeekdayNames", enUsData, localeData, previous);
        result.append('\n');

        describeString(result, "yesterday", enUsData, localeData);
        describeString(result, "today", enUsData, localeData);
        describeString(result, "tomorrow", enUsData, localeData);
        result.append('\n');

        describeChar(result, "zeroDigit", enUsData, localeData);
        describeChar(result, "decimalSeparator", enUsData, localeData);
        describeChar(result, "groupingSeparator", enUsData, localeData);
        describeChar(result, "patternSeparator", enUsData, localeData);
        describeChar(result, "percent", enUsData, localeData);
        describeChar(result, "perMill", enUsData, localeData);
        describeChar(result, "monetarySeparator", enUsData, localeData);
        describeChar(result, "minusSign", enUsData, localeData);
        describeString(result, "exponentSeparator", enUsData, localeData);
        describeString(result, "infinity", enUsData, localeData);
        describeString(result, "NaN", enUsData, localeData);
        result.append('\n');

      } catch (Exception ex) {
        result.append("(" + ex.getClass().getSimpleName() + " thrown: " + ex.getMessage() + ")\n");
        System.err.println(ex);
      }
    }

    return result.toString();
  }

  private static String[] describeStringArray(StringBuilder sb, String fieldName, Object enUsData, Object localeData, String[] previous) {
    try {
      String[] values = (String[]) localeData.getClass().getField(fieldName).get(localeData);

      if (Arrays.equals(values, previous)) {
        return values;
      }

      String[] enUsValues = (String[]) localeData.getClass().getField(fieldName).get(enUsData);
      sb.append(fieldName).append(":\n");
      for (int i = 0; i < values.length; ++i) {
        if (enUsValues[i].isEmpty()) {
          continue; // Java's weekday names array has an empty first element.
        }
        sb.append("    ").append(values[i]);
        if (!values[i].equals(enUsValues[i])) {
          sb.append("  (" + enUsValues[i] + ")");
        }
        sb.append('\n');
      }
      return values;
    } catch (Exception ignored) {
      sb.append(fieldName).append(": <missing>\n");
      return null;
    }
  }

  private static void describeString(StringBuilder sb, String fieldName, Object enUsData, Object localeData) {
    String valueText = "<missing>";
    try {
      valueText = unicodeString((String) localeData.getClass().getField(fieldName).get(localeData));
    } catch (Exception ignored) {
    }

    sb.append(fieldName + ": " + valueText + "\n");
  }

  private static void describeChar(StringBuilder sb, String fieldName, Object enUsData, Object localeData) {
    String valueText = "<missing>";
    try {
      valueText = unicodeString(localeData.getClass().getField(fieldName).getChar(localeData));
    } catch (Exception ignored) {
    }

    sb.append(fieldName + ": " + valueText + "\n");
  }

  private static Object getLocaleDataInstance(Locale locale) {
    try {
      return localeDataClass().getMethod("get", Locale.class).invoke(null, locale);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static boolean hasLocaleData() {
    return localeDataClass() != null;
  }

  private static Class<?> localeDataClass() {
    try {
      return Class.forName("libcore.icu.LocaleData");
    } catch (Exception ignored) {
      return null;
    }
  }

  private static String unicodeString(char ch) {
    return unicodeString(Character.toString(ch));
  }

  private static String unicodeString(String s) {
    if (s.length() > 1) {
      // For actual text (like the Arabic NaN), this isn't obviously useful.
      return s;
    }
    if (!containsNonAscii(s)) {
      // If there are only ASCII characters, don't belabor the point.
      return s;
    }
    StringBuilder sb = new StringBuilder();
    sb.append(s);
    sb.append("  (");
    for (int i = 0; i < s.length(); ++i) {
      sb.append(String.format("U+%04x", (int) s.charAt(i)));
    }
    sb.append(")");
    return sb.toString();
  }

  private static boolean containsNonAscii(String s) {
    for (int i = 0; i < s.length(); ++i) {
      if (s.charAt(i) > 0x7f) {
        return true;
      }
    }
    return false;
  }

  private static void appendAvailability(StringBuilder result, Locale locale, String name, Class<?> c) {
    String state = "unknown";
    try {
      Locale[] locales = (Locale[]) c.getMethod("getAvailableLocales").invoke(null);
      state = Arrays.asList(locales).contains(locale) ? "present" : "missing";
    } catch (Exception ex) {
      // DateFormatSymbols.getAvailableLocales and DecimalFormatSymbols.getAvailableLocales weren't added until Java 6.
    }
    result.append(name + ": " + state + "\n");
  }

  private static void describeDecimalFormatSymbols(StringBuilder result, DecimalFormatSymbols dfs) {
    result.append("Currency Symbol: " + unicodeString(dfs.getCurrencySymbol()) + "\n");
    result.append("International Currency Symbol: " + unicodeString(dfs.getInternationalCurrencySymbol()) + "\n");
    result.append('\n');

    result.append("Digit: " + unicodeString(dfs.getDigit()) + "\n");
    result.append("Pattern Separator: " + unicodeString(dfs.getPatternSeparator()) + "\n");
    result.append('\n');

    result.append("Decimal Separator: " + unicodeString(dfs.getDecimalSeparator()) + "\n");
    result.append("Monetary Decimal Separator: " + unicodeString(dfs.getMonetaryDecimalSeparator()) + "\n");
    // 1.6: result.append("Exponent Separator: " + dfs.getExponentSeparator() + "\n");
    result.append("Grouping Separator: " + unicodeString(dfs.getGroupingSeparator()) + "\n");

    result.append("Infinity: " + unicodeString(dfs.getInfinity()) + "\n");
    result.append("Minus Sign: " + unicodeString(dfs.getMinusSign()) + "\n");
    result.append("NaN: " + unicodeString(dfs.getNaN()) + "\n");
    result.append("Percent: " + unicodeString(dfs.getPercent()) + "\n");
    result.append("Per Mille: " + unicodeString(dfs.getPerMill()) + "\n");
    result.append("Zero Digit: " + unicodeString(dfs.getZeroDigit()) + "\n");
    StringBuilder digits = new StringBuilder();
    for (int i = 0; i <= 9; ++i) {
      digits.append((char) (dfs.getZeroDigit() + i));
    }
    result.append("Digits: " + digits.toString() + "\n");
    result.append('\n');
    result.append('\n');
  }

  private static void describeDateFormat(StringBuilder result, String description, DateFormat dateFormat, Date when) {
    if (dateFormat instanceof SimpleDateFormat) {
      SimpleDateFormat sdf = (SimpleDateFormat) dateFormat;
      result.append(description + ": " + sdf.toPattern() + "\n");
      result.append("    " + sdf.format(when) + "\n");
    }
  }

  private static void describeNumberFormat(StringBuilder result, String description, NumberFormat numberFormat, Number... values) {
    if (numberFormat instanceof DecimalFormat) {
      DecimalFormat df = (DecimalFormat) numberFormat;
      result.append(description + ": " + df.toPattern() + "\n");
      for (Number value : values) {
        result.append("    " + df.format(value) + "\n");
      }
    }
  }
}
