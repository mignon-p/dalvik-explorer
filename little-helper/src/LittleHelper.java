/*
 * This file is part of LittleHelper.
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
import org.jessies.calc.*;
import org.jessies.test.*;

/**
 * Yet another attempt at the whole Google onebox/Mac OS Spotlight/GNOME Deskbar genre.
 * 
 * Under GNOME, to run little-helper when F5 is pressed:
 * 
 * % gconftool-2 -t string -s /apps/metacity/global_keybindings/run_command_5 F5
 * % gconftool-2 -t string -s /apps/metacity/keybinding_commands/command_5 `which little-helper`
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
    
    private Calculator calculator = new Calculator();
    
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
    
    private String calculate(String s) {
        try {
            return calculator.evaluate(s);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    private static String convertUnits(String s) {
        try {
            return UnitsConverter.convert(s);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
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
