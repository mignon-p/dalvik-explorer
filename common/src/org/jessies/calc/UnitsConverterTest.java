package org.jessies.calc;

/*
 * This file is part of org.jessies.calc.
 * Copyright (C) 2011 Elliott Hughes <enh@jessies.org>.
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

import junit.framework.TestCase;
import static org.jessies.calc.UnitsConverter.*;

public class UnitsConverterTest extends TestCase {
    public void testTemperatureConversion() {
        assertEquals("32.0 F", convert("0C"));
        assertEquals("-40.0 F", convert("-40C"));
        assertEquals("212.0 F", convert("100C"));
        assertEquals("212.0 F", convert("100.0C"));
        assertEquals("212.0 F", convert("100 C"));
        assertEquals("212.0 F", convert("100.0 C"));
        assertEquals("0.0 C", convert("32 F"));
        assertEquals("25.6 C", convert("78F"));
    }
    
    public void testImperialLengthConversion() {
        assertEquals("0.34 m", convert("13.3\""));
        assertEquals("0.34 m", convert("13.3 \""));
        assertEquals("0.34 m", convert("13.3 in"));
        assertEquals("0.34 m", convert("13.3 inch"));
        assertEquals("0.34 m", convert("13.3 inches"));
        
        assertEquals("1.83 m", convert("6 '"));
        assertEquals("1.83 m", convert("6 foot"));
        assertEquals("1.83 m", convert("6 feet"));
        assertEquals("1.83 m", convert("6 ft"));
        assertEquals("1.83 m", convert("6 ft 0 in"));
        
        assertEquals("1.63 m", convert("5'4\""));
        assertEquals("1.63 m", convert("5' 4\""));
        assertEquals("1.63 m", convert("5 ' 4\""));
        assertEquals("1.63 m", convert("5 ' 4 \""));
        assertEquals("1.63 m", convert("5 feet 4 inches"));
        assertEquals("1.63 m", convert("5feet 4inches"));
        assertEquals("1.63 m", convert("5feet4inches"));
        assertEquals("1.63 m", convert("5ft 4in"));
        assertEquals("1.63 m", convert("5 ft 4 in"));
    }
    
    public void testImperialWeightConversion() {
        assertEquals("2.27 kg", convert("5.0 pound"));
        assertEquals("2.27 kg", convert("5.0pound"));
        assertEquals("2.27 kg", convert("5.0 pounds"));
        assertEquals("2.27 kg", convert("5.0pounds"));
        assertEquals("2.27 kg", convert("5 lb"));
        assertEquals("2.27 kg", convert("5lb"));
        assertEquals("2.27 kg", convert("5 lbs"));
        assertEquals("2.27 kg", convert("5lbs"));
        
        assertEquals("0.04 kg", convert("1.3 ounces"));
        assertEquals("0.04 kg", convert("1.3 oz"));
    }
}
