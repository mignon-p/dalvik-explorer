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
        if (name.length() == 0) {
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
        
        result.append("Decimal Format Symbols\n\n");
        NumberFormat nf = NumberFormat.getInstance(locale);
        if (nf instanceof DecimalFormat) {
            describeDecimalFormatSymbols(result, ((DecimalFormat) nf).getDecimalFormatSymbols());
        } else {
            result.append("(Didn't expect " + nf.getClass() + ".)");
        }
        
        Date now = new Date(); // FIXME: it might be more useful to always show a time in the afternoon, to make 24-hour patterns more obvious.
        result.append("Date/Time Formatting\n\n");
        describeDateFormat(result, "Full Date", DateFormat.getDateInstance(DateFormat.FULL, locale), now);
        describeDateFormat(result, "Medium Date", DateFormat.getDateInstance(DateFormat.MEDIUM, locale), now);
        describeDateFormat(result, "Short Date", DateFormat.getDateInstance(DateFormat.SHORT, locale), now);
        result.append('\n');
        describeDateFormat(result, "Full Time", DateFormat.getTimeInstance(DateFormat.FULL, locale), now);
        describeDateFormat(result, "Medium Time", DateFormat.getTimeInstance(DateFormat.MEDIUM, locale), now);
        describeDateFormat(result, "Short Time", DateFormat.getTimeInstance(DateFormat.SHORT, locale), now);
        result.append('\n');
        describeDateFormat(result, "Full Date/Time", DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale), now);
        describeDateFormat(result, "Medium Date/Time", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale), now);
        describeDateFormat(result, "Short Date/Time", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale), now);
        result.append('\n');
        result.append('\n');
        
        result.append("Date Format Symbols\n\n");
        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, locale);
        DateFormatSymbols dfs = null;
        if (df instanceof SimpleDateFormat) {
            dfs = ((SimpleDateFormat) df).getDateFormatSymbols();
            result.append("Am/pm: " + Arrays.toString(dfs.getAmPmStrings()) + "\n");
            result.append("Eras: " + Arrays.toString(dfs.getEras()) + "\n");
            result.append("Local Pattern Chars: " + dfs.getLocalPatternChars() + "\n");
            result.append("Months: " + Arrays.toString(dfs.getMonths()) + "\n");
            result.append("Short Months: " + Arrays.toString(dfs.getShortMonths()) + "\n");
            result.append("Weekdays: " + Arrays.toString(dfs.getWeekdays()) + "\n");
            result.append("Short Weekdays: " + Arrays.toString(dfs.getShortWeekdays()) + "\n");
        } else {
            result.append("(Didn't expect " + df.getClass() + ".)");
        }
        result.append('\n');
        result.append('\n');
        
        result.append("Calendar\n\n");
        Calendar c = Calendar.getInstance(locale);
        int firstDayOfWeek = c.getFirstDayOfWeek();
        String firstDayOfWeekString = (dfs != null) ? dfs.getWeekdays()[firstDayOfWeek] : "unknown";
        String englishFirstDayOfWeekString = ENGLISH_WEEKDAYS[firstDayOfWeek];
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
        
        return result.toString();
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
    
    private static final String[] ENGLISH_WEEKDAYS = new String[] { "invalid", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
    
    private static void describeDecimalFormatSymbols(StringBuilder result, DecimalFormatSymbols dfs) {
        result.append("Currency Symbol: " + dfs.getCurrencySymbol() + "\n");
        result.append("International Currency Symbol: " + dfs.getInternationalCurrencySymbol() + "\n");
        result.append('\n');
        
        result.append("Digit: " + dfs.getDigit() + "\n");
        result.append("Pattern Separator: " + dfs.getPatternSeparator() + "\n");
        result.append('\n');
        
        result.append("Decimal Separator: " + dfs.getDecimalSeparator() + "\n");
        result.append("Monetary Decimal Separator: " + dfs.getMonetaryDecimalSeparator() + "\n");
        // 1.6: result.append("Exponent Separator: " + dfs.getExponentSeparator() + "\n");
        result.append("Grouping Separator: " + dfs.getGroupingSeparator() + "\n");
        
        result.append("Infinity: " + dfs.getInfinity() + "\n");
        result.append("Minus Sign: " + dfs.getMinusSign() + "\n");
        result.append("NaN: " + dfs.getNaN() + "\n");
        result.append("Percent: " + dfs.getPercent() + "\n");
        result.append("Per Mille: " + dfs.getPerMill() + "\n");
        result.append("Zero Digit: " + dfs.getZeroDigit() + "\n");
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
