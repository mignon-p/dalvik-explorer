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

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.imageio.*;
import javax.swing.*;
import org.jessies.test.*;

/**
 * Yet another attempt at the whole Google onebox/Mac OS Spotlight/GNOME Deskbar genre.
 * 
 * Under GNOME, to run little-helper when F4 (the Dashboard key on Apple keyboards) is pressed:
 * 
 * % gconftool-2 -t string -s /apps/metacity/global_keybindings/run_command_4 F4
 * % gconftool-2 -t string -s /apps/metacity/keybinding_commands/command_4 `which little-helper`
 * 
 * You can also use gconf-editor(1) to edit these from the GUI.
 */
public class LittleHelper extends JFrame {
    private static class Verb {
        private final Pattern pattern;
        private final String urlTemplate;
        
        public Verb(String regularExpression, String urlTemplate) {
            this.pattern = Pattern.compile(regularExpression);
            this.urlTemplate = urlTemplate;
        }
        
        public String getUrl(String query) {
            final Matcher matcher = pattern.matcher(query);
            if (matcher.matches()) {
                final String urlEncodedQuery = StringUtilities.urlEncode(matcher.group(1));
                return new Formatter().format(urlTemplate, urlEncodedQuery).toString();
            }
            return null;
        }
    }
    
    private static final List<Verb> verbs = new ArrayList<Verb>();
    
    private JTextField textField;
    private JList resultList;
    
    public LittleHelper() {
        super("Little Helper");
        
        setContentPane(makeUi());
        pack();
        
        setLocationRelativeTo(null);
        
        // FIXME: this is a bit harsh (but effective: MainFrame doesn't seem to work right, and even when it does it's slow).
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JFrameUtilities.closeOnEsc(this);
        
        GuiUtilities.finishGnomeStartup();
    }
    
    private JPanel makeUi() {
        final JPanel ui = new JPanel(new BorderLayout());
        ui.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        this.textField = new JTextField(40);
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });
        ui.add(textField, BorderLayout.NORTH);
        
        this.resultList = new JList();
        resultList.setCellRenderer(new CustomListCellRenderer());
        resultList.setVisibleRowCount(8);
        ui.add(new JScrollPane(resultList), BorderLayout.CENTER);
        
        // There's no point being able to "scroll" a single-line text field, but scrolling a multi-row list is useful.
        ComponentUtilities.divertPageScrollingFromTo(textField, resultList);
        
        return ui;
    }
    
    private void help() {
        resultList.setModel(new DefaultListModel());
        final String query = textField.getText().trim();
        if (query.length() == 0) {
            return;
        }
        
        // FIXME: do everything from this point down on another thread.
        
        // Sometimes a URL is just a URL...
        if (query.startsWith("http://")) {
            openUrl(query);
        }
        
        // Do we have a "verb"?
        for (Verb verb : verbs) {
            final String url = verb.getUrl(query);
            if (url != null) {
                openUrl(url);
            }
        }
        
        DefaultListModel model = new DefaultListModel();
        
        // Weather is popular...
        if (query.equals("weather")) {
            showWeatherForecast(model);
        }
        
        // Convert bases.
        NumberDecoder numberDecoder = new NumberDecoder(query);
        if (numberDecoder.isValid()) {
            for (String item : numberDecoder.toStrings()) {
                model.addElement(item);
            }
        }
        
        // Convert units.
        final String unitConversion = convertUnits(query);
        if (unitConversion != null) {
            model.addElement(unitConversion);
        }
        
        // Evaluate mathematical expressions.
        final String calculatorResult = calculate(query);
        if (calculatorResult != null) {
            model.addElement(calculatorResult);
        }
        
        resultList.setModel(model);
    }
    
    private void openUrl(String url) {
        try {
            System.err.println(url);
            BrowserLauncher.openURL(url);
            // Our work here is done.
            // FIXME: this is a bit harsh (but effective: MainFrame doesn't seem to work right, and even when it does it's slow).
            System.exit(0);
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            // bark
        }
    }
    
    private static String calculate(String s) {
        try {
            return new Calculator(s).evaluate();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    @Test private static void testTemperatureConversion() {
        Assert.equals(convertUnits("0C"), "32.0 F");
        Assert.equals(convertUnits("-40C"), "-40.0 F");
        Assert.equals(convertUnits("100C"), "212.0 F");
        Assert.equals(convertUnits("100.0C"), "212.0 F");
        Assert.equals(convertUnits("100 C"), "212.0 F");
        Assert.equals(convertUnits("100.0 C"), "212.0 F");
        Assert.equals(convertUnits("32 F"), "0.0 C");
        Assert.equals(convertUnits("78F"), "25.6 C");
    }
    
    @Test private static void testImperialLengthConversion() {
        Assert.equals(convertUnits("13.3\""), "0.34 m");
        Assert.equals(convertUnits("13.3 \""), "0.34 m");
        Assert.equals(convertUnits("13.3 in"), "0.34 m");
        Assert.equals(convertUnits("13.3 inch"), "0.34 m");
        Assert.equals(convertUnits("13.3 inches"), "0.34 m");
        
        Assert.equals(convertUnits("6 '"), "1.83 m");
        Assert.equals(convertUnits("6 foot"), "1.83 m");
        Assert.equals(convertUnits("6 feet"), "1.83 m");
        Assert.equals(convertUnits("6 ft"), "1.83 m");
        Assert.equals(convertUnits("6 ft 0 in"), "1.83 m");
        
        Assert.equals(convertUnits("5'4\""), "1.63 m");
        Assert.equals(convertUnits("5' 4\""), "1.63 m");
        Assert.equals(convertUnits("5 ' 4\""), "1.63 m");
        Assert.equals(convertUnits("5 ' 4 \""), "1.63 m");
        Assert.equals(convertUnits("5 feet 4 inches"), "1.63 m");
        Assert.equals(convertUnits("5feet 4inches"), "1.63 m");
        Assert.equals(convertUnits("5feet4inches"), "1.63 m");
        Assert.equals(convertUnits("5ft 4in"), "1.63 m");
        Assert.equals(convertUnits("5 ft 4 in"), "1.63 m");
    }
    
    @Test private static void testImperialWeightConversion() {
        Assert.equals(convertUnits("5.0 pound"), "2.27 kg");
        Assert.equals(convertUnits("5.0pound"), "2.27 kg");
        Assert.equals(convertUnits("5.0 pounds"), "2.27 kg");
        Assert.equals(convertUnits("5.0pounds"), "2.27 kg");
        Assert.equals(convertUnits("5 lb"), "2.27 kg");
        Assert.equals(convertUnits("5lb"), "2.27 kg");
        Assert.equals(convertUnits("5 lbs"), "2.27 kg");
        Assert.equals(convertUnits("5lbs"), "2.27 kg");
        
        Assert.equals(convertUnits("1.3 ounces"), "0.04 kg");
        Assert.equals(convertUnits("1.3 oz"), "0.04 kg");
    }
    
    private static String convertUnits(String s) {
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
    
    private static void initVerbs() {
        // Web searches.
        verbs.add(new Verb("^(?:g(?:oogle)?) +(.*)", "http://www.google.com/search?hl=en&q=%s&btnG=Google+Search&aq=f&oq="));
        verbs.add(new Verb("^(?:i(?:mdb)?) +(.*)", "http://www.imdb.com/find?s=all&q=%s&x=0&y=0"));
        verbs.add(new Verb("^(?:p(?:osix)?) +(.*)", "http://opengroup.org/cgi-bin/kman2?value=%s"));
        verbs.add(new Verb("^(?:w(?:ikipedia)?) +(.*)", "http://en.wikipedia.org/wiki/Special:Search?search=%s&go=Go"));
        
        // Tracking numbers.
        verbs.add(new Verb("^(1Z\\d{4}W\\d{11})$", "http://wwwapps.ups.com/WebTracking/processInputRequest?sort_by=status&tracknums_displayed=1&TypeOfInquiryNumber=T&loc=en_US&InquiryNumber1=%s&track.x=0&track.y=0"));
        // FIXME: if there are spaces in this, we need to strip them out before constructing the URL.
        verbs.add(new Verb("^(\\d{4} ?\\d{4} ?\\d{4} ?\\d{4} ?\\d{4} ?\\d{2})$", "http://trkcnfrm1.smi.usps.com/PTSInternetWeb/InterLabelInquiry.do?origTrackNum=%s"));
    }
    
    private static void showWeatherForecast(DefaultListModel model) {
        // "http://www.bbc.co.uk/weather/5day.shtml?world=4079" // Basel, CH.
        // "http://www.bbc.co.uk/weather/5day.shtml?world=0306" // San Jose, CA, US.
        final String location = "http://www.bbc.co.uk/weather/5day.shtml?world=0306";
        final ArrayList<DayForecast> forecast = collectForecastFor(location);
        for (DayForecast day : forecast) {
            model.addElement(day);
        }
    }
    
    private static ArrayList<DayForecast> collectForecastFor(String location) {
        ArrayList<DayForecast> forecast = new ArrayList<DayForecast>();
        
        // FIXME: cache the forecast so we don't re-fetch it more than, say, once per hour.
        String content;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new URL(location).openStream()));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                buffer.append(line);
                buffer.append('\n');
            }
            content = buffer.toString();
        } catch (IOException ex) {
            Log.warn("Failed to read forecast.", ex);
            return null;
        } finally {
            FileUtilities.close(in);
        }
        
        Pattern cityNamePattern = Pattern.compile("^\\s+<title>BBC.* Forecast in .* for (.+),");
        Pattern dayNamePattern = Pattern.compile("class=\"weatherday\".*strong>(\\S+)<br");
        Pattern imageUrlPattern = Pattern.compile("img src=\"(\\S+\\/fiveday_sym\\/\\d+\\S+)\"");
        Pattern dayTemperaturePattern = Pattern.compile("class=\"temptxt\"><strong>(\\d+)<.*Day ");
        Pattern nightTemperaturePattern = Pattern.compile("class=\"temptxt\">.*<strong>(\\d+)<.*Night ");
        String dayName = null;
        String imageUrl = null;
        int dayTemperatureC = 0;
        Matcher matcher;
        for (String line : content.split("\n")) {
            if ((matcher = cityNamePattern.matcher(line)).find()) {
                // FIXME: make some use of the location name.
                //setTitle(matcher.group(1) + " 5 Day Forecast");
            } else if ((matcher = dayNamePattern.matcher(line)).find()) {
                dayName = matcher.group(1);
            } else if ((matcher = imageUrlPattern.matcher(line)).find()) {
                imageUrl = "http://www.bbc.co.uk" + matcher.group(1);
            } else if ((matcher = dayTemperaturePattern.matcher(line)).find()) {
                dayTemperatureC = Integer.parseInt(matcher.group(1));
            } else if ((matcher = nightTemperaturePattern.matcher(line)).find()) {
                int nightTemperatureC = Integer.parseInt(matcher.group(1));
                DayForecast dayForecast = new DayForecast(dayName, imageUrl, dayTemperatureC, nightTemperatureC);
                forecast.add(dayForecast);
            }
        }
        
        if (forecast.size() != 5) {
            System.out.println(content);
            for (DayForecast dayForecast : forecast) {
                System.out.println(dayForecast);
            }
        }
        
        return forecast;
    }
    
    public static class CustomListCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof DayForecast) {
                ((DayForecast) value).configureCellRenderer(this, list, isSelected);
                setEnabled(list.isEnabled());
            }
            return this;
        }
    }
    
    private static class DayForecast {
        private final String day;
        private ImageIcon icon;
        private final int dayTemperatureC;
        private final int nightTemperatureC;
        
        public DayForecast(String day, String imageUrl, int dayTemperatureC, int nightTemperatureC) {
            this.day = day;
            this.dayTemperatureC = dayTemperatureC;
            this.nightTemperatureC = nightTemperatureC;
            
            // FIXME: doesn't GNOME have suitable icons already available?
            // FIXME: we should at least cache these on local disk!
            try {
                BufferedImage image = ImageIO.read(new URL(imageUrl));
                BufferedImage scaledImage = new AffineTransformOp(AffineTransform.getScaleInstance(0.5, 0.5), AffineTransformOp.TYPE_BILINEAR).filter(image, null);
                this.icon = new ImageIcon(scaledImage);
            } catch (IOException ex) {
                Log.warn("Failed to read image \"" + imageUrl + "\".", ex);
            }
        }
        
        private void configureCellRenderer(JLabel renderer, JList list, boolean isSelected) {
            final String DEGREES_C = "\u00b0C";
            renderer.setIcon(icon);
            renderer.setText(day + " " + dayTemperatureC + DEGREES_C + " (" + nightTemperatureC + DEGREES_C + ")");
        }
        
        public String toString() {
            return "DayForecast[day=" + day + ",icon=" + icon + ",dayTemperatureC=" + dayTemperatureC + ",nightTemperatureC=" + nightTemperatureC + "]";
        }
    }
    
    public static void main(String[] args) {
        initVerbs();
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                GuiUtilities.initLookAndFeel();
                new LittleHelper().setVisible(true);
            }
        });
    }
}
