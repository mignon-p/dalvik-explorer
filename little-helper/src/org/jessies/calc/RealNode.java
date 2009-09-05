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
import static org.jessies.calc.BigDecimals.*;

public class RealNode implements Comparable<RealNode>, NumberNode {
    public static final RealNode ZERO = new RealNode(0);
    
    private final BigDecimal value;
    
    public RealNode(BigDecimal value) {
        this.value = value;
    }
    
    public RealNode(double value) {
        this(new BigDecimal(value, MATH_CONTEXT));
    }
    
    public Node evaluate(Calculator environment) {
        return this;
    }
    
    public RealNode abs() {
        return new RealNode(value.abs());
    }
    
    public RealNode acos() {
        return new RealNode(Math.acos(value.doubleValue()));
    }
    
    public RealNode asin() {
        return new RealNode(Math.asin(value.doubleValue()));
    }
    
    public RealNode atan() {
        return new RealNode(Math.atan(value.doubleValue()));
    }
    
    public RealNode atan2(RealNode x) {
        return new RealNode(Math.atan2(value.doubleValue(), x.value.doubleValue()));
    }
    
    public RealNode cbrt() {
        return new RealNode(Math.cbrt(value.doubleValue()));
    }
    
    public RealNode ceiling() {
        return new RealNode(Math.ceil(value.doubleValue()));
    }
    
    public int compareTo(RealNode rhs) {
        return value.compareTo(rhs.value);
    }
    
    public RealNode cos() {
        return new RealNode(Math.cos(value.doubleValue()));
    }
    
    public RealNode cosh() {
        return new RealNode(Math.cosh(value.doubleValue()));
    }
    
    public NumberNode divide(NumberNode rhs) {
        if (rhs instanceof IntegerNode) {
            rhs = rhs.toReal();
        }
        return new RealNode(value.divide(((RealNode) rhs).value, MATH_CONTEXT));
    }
    
    public RealNode exp() {
        return new RealNode(Math.exp(value.doubleValue()));
    }
    
    public RealNode floor() {
        return new RealNode(Math.floor(value.doubleValue()));
    }
    
    public RealNode fractionalPart() {
        RealNode result = new RealNode(value.remainder(BigDecimal.ONE));
        return (value.signum() < 0) ? new RealNode(result.value.negate()) : result;
    }
    
    public RealNode hypot(RealNode y) {
        return new RealNode(Math.hypot(value.doubleValue(), y.value.doubleValue()));
    }
    
    public RealNode increment() {
        return new RealNode(value.add(BigDecimal.ONE));
    }
    
    public IntegerNode integerPart() {
        String s = value.toPlainString();
        int decimalPoint = s.indexOf('.');
        if (decimalPoint != -1) {
            s = s.substring(0, decimalPoint);
        }
        return new IntegerNode(s, 10);
    }
    
    public RealNode log(RealNode base) {
        return new RealNode(Math.log(value.doubleValue()) / Math.log(base.value.doubleValue()));
    }
    
    public RealNode log2() {
        return new RealNode(Math.log(value.doubleValue()) / Math.log(2.0));
    }
    
    public RealNode logE() {
        return new RealNode(Math.log(value.doubleValue()));
    }
    
    public RealNode log10() {
        return new RealNode(Math.log10(value.doubleValue()));
    }
    
    public NumberNode plus(NumberNode rhs) {
        if (rhs instanceof IntegerNode) {
            rhs = rhs.toReal();
        }
        return new RealNode(value.add(((RealNode) rhs).value));
    }
    
    public NumberNode power(NumberNode rhs) {
        if (rhs instanceof IntegerNode) {
            rhs = rhs.toReal();
        }
        return new RealNode(Math.pow(value.doubleValue(), ((RealNode) rhs).value.doubleValue()));
    }
    
    public RealNode round() {
        return new RealNode(Math.round(value.doubleValue()));
    }
    
    public RealNode sin() {
        return new RealNode(Math.sin(value.doubleValue()));
    }
    
    public RealNode sinh() {
        return new RealNode(Math.sinh(value.doubleValue()));
    }
    
    public RealNode sqrt() {
        return new RealNode(Math.sqrt(value.doubleValue()));
    }
    
    public NumberNode subtract(NumberNode rhs) {
        if (rhs instanceof IntegerNode) {
            rhs = rhs.toReal();
        }
        return new RealNode(value.subtract(((RealNode) rhs).value));
    }
    
    public RealNode tan() {
        return new RealNode(Math.tan(value.doubleValue()));
    }
    
    public RealNode tanh() {
        return new RealNode(Math.tanh(value.doubleValue()));
    }
    
    public NumberNode times(NumberNode rhs) {
        if (rhs instanceof IntegerNode) {
            rhs = rhs.toReal();
        }
        return new RealNode(value.multiply(((RealNode) rhs).value));
    }
    
    public RealNode toReal() {
        return this;
    }
    
    @Override public String toString() {
        return value.toString();
    }
}
