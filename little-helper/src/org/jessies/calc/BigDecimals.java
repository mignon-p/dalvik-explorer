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
 * This is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.math.*;

public class BigDecimals {
    public static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);
    
    public static BigDecimal fromString(String s) {
        return new BigDecimal(s);
    }
    
    public static BigDecimal fromBigInteger(BigInteger i) {
        return new BigDecimal(i, MATH_CONTEXT);
    }
    
    public static BigDecimal fromDouble(double d) {
        return new BigDecimal(d, MATH_CONTEXT);
    }
    
    public static BigDecimal fromBoolean(boolean b) {
        return b ? BigDecimal.ONE : BigDecimal.ZERO;
    }
}
