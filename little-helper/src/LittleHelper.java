import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;

public class LittleHelper extends MainFrame {
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
        JFrameUtilities.closeOnEsc(this);
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
        resultList.setVisibleRowCount(8);
        ui.add(new JScrollPane(resultList), BorderLayout.CENTER);
        
        return ui;
    }
    
    private void help() {
        resultList.setModel(new DefaultListModel());
        final String query = textField.getText().trim();
        if (query.length() == 0) {
            return;
        }
        
        // FIXME: do everything from this point down on another thread.
        
        // Do we have a "verb"?
        for (Verb verb : verbs) {
            final String url = verb.getUrl(query);
            if (url != null) {
                try {
                    BrowserLauncher.openURL(url);
                } catch (java.io.IOException ex) {
                    // bark
                }
                // Our work here is done.
                setVisible(false);
                return;
            }
        }
        
        DefaultListModel model = new DefaultListModel();
        
        // Convert bases.
        NumberDecoder numberDecoder = new NumberDecoder(query);
        if (numberDecoder.isValid()) {
            java.util.List<String> items = numberDecoder.toStrings();
            for (String item : items) {
                model.addElement(item);
            }
        }
        
        // Convert units.
        final String unitConversion = convertUnits(query);
        if (unitConversion != null) {
            model.addElement(unitConversion);
        }
        
        // FIXME: Calculator.
        
        resultList.setModel(model);
    }
    
    private static final Pattern TEMPERATURE = Pattern.compile("^([\\d.]+) *([CF])");
    
    private static String convertUnits(String s) {
        // Temperature?
        final Matcher temperatureMatcher = TEMPERATURE.matcher(s);
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
        
        // FIXME: Length?
        // 13.3"
        // 13.3 "
        // 13.3 inches
        // 5'4"
        // 5'4"
        // 5'4"
        // 5 feet 4 inches
        // 1.37m
        // 1m 37cm
        // 2 meters
        // 24.3 cm
        // 90.7 mm
        
        // FIXME: Weight?
        // 5.0 pounds
        // 5lbs
        // 2.27kg
        // 1.3 ounces
        // 1.3 oz
        // 36.8 grams
        // 36.8 g
        
        //   ([\\d+.]+) *(pounds|lb|lbs|ounce|ounces|oz|kg|g|grams)
        
        // FIXME: Currency?
        
        return null;
    }
    
    private static void initVerbs() {
        verbs.add(new Verb("^(?:i(?:mdb)?) +(.*)", "http://www.imdb.com/find?s=all&q=%s&x=0&y=0"));
        verbs.add(new Verb("^(?:g(?:oogle)?) +(.*)", "http://www.google.com/search?hl=en&q=%s&btnG=Google+Search&aq=f&oq="));
        verbs.add(new Verb("^(?:w(?:ikipedia)?) +(.*)", "http://en.wikipedia.org/wiki/Special:Search?search=%s&go=Go"));
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
