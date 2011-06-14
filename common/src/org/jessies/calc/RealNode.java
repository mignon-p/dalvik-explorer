package org.jessies.calc;

/*
 * This file is part of org.jessies.calc.
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

/**
 * Represents an approximate number.
 * 
 * We would be better off using rational arithmetic until we need to display.
 * User-supplied approximate numbers could be converted to "equivalent" rationals on entry.
 * The downside to such an approach would be worse performance.
 */
public class RealNode implements Comparable<RealNode>, NumberNode {
    public static final RealNode ZERO = new RealNode(0);
    
    private final double value;
    
    public RealNode(double value) {
        this.value = value;
    }
    
    public RealNode(String s) {
        this(Double.parseDouble(s));
    }
    
    public Node evaluate(Calculator environment) {
        return this;
    }
    
    public RealNode abs() {
        return new RealNode(Math.abs(value));
    }
    
    public RealNode acos() {
        return new RealNode(Math.acos(value));
    }
    
    public RealNode asin() {
        return new RealNode(Math.asin(value));
    }
    
    public RealNode atan() {
        return new RealNode(Math.atan(value));
    }
    
    public RealNode atan2(RealNode x) {
        return new RealNode(Math.atan2(value, x.value));
    }
    
    public RealNode cbrt() {
        return new RealNode(Math.cbrt(value));
    }
    
    public RealNode ceiling() {
        return new RealNode(Math.ceil(value));
    }
    
    public int compareTo(RealNode rhs) {
        return Double.compare(value, rhs.value);
    }
    
    public RealNode cos() {
        return new RealNode(Math.cos(value));
    }
    
    public RealNode cosh() {
        return new RealNode(Math.cosh(value));
    }
    
    private static double rhsValue(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return ((RealNode) rhs).value;
        } else if (rhs instanceof BigRealNode) {
            return ((BigRealNode) rhs).doubleValue();
        } else {
            return ((IntegerNode) rhs).fix();
        }
    }
    
    public NumberNode divide(NumberNode rhs) {
        if (!(rhs instanceof RealNode)) {
            rhs = rhs.toReal();
        }
        return new RealNode(value/ ((RealNode) rhs).value);
    }
    
    @Override public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RealNode)) {
            return false;
        }
        RealNode rhs = (RealNode) other;
        return (Double.compare(value, rhs.value) == 0);
    }
    
    @Override public int hashCode() {
        final long bits = Double.doubleToLongBits(value);
        return (int) (bits ^ (bits >>> 32));
    }
    
    public RealNode exp() {
        return new RealNode(Math.exp(value));
    }
    
    public RealNode floor() {
        // FIXME: would be nice to return an IntegerNode.
        return new RealNode(Math.floor(value));
    }
    
    public RealNode fractionalPart() {
        String s = Double.toString(value);
        int i = 0;
        while (s.charAt(i) == '-' || Character.isDigit(s.charAt(i))) {
            ++i;
        }
        s = s.substring(i);
        double fractionalPart = Double.parseDouble(s);
        return (fractionalPart == value) ? this : new RealNode(fractionalPart);
    }
    
    public RealNode hypot(RealNode y) {
        return new RealNode(Math.hypot(value, y.value));
    }
    
    public RealNode increment() {
        return new RealNode(value + 1);
    }
    
    public IntegerNode integerPart() {
        return IntegerNode.valueOf((long) value);
    }
    
    public RealNode log(RealNode base) {
        return new RealNode(Math.log(value) / Math.log(base.value));
    }
    
    public RealNode log2() {
        return new RealNode(Math.log(value) / Math.log(2.0));
    }
    
    public RealNode logE() {
        return new RealNode(Math.log(value));
    }
    
    public RealNode log10() {
        return new RealNode(Math.log10(value));
    }
    
    public NumberNode plus(NumberNode rhs) {
        if (!(rhs instanceof RealNode)) {
            rhs = rhs.toReal();
        }
        return new RealNode(value + ((RealNode) rhs).value);
    }
    
    public NumberNode power(NumberNode rhs) {
        if (!(rhs instanceof RealNode)) {
            rhs = rhs.toReal();
        }
        return new RealNode(Math.pow(value, ((RealNode) rhs).value));
    }
    
    public IntegerNode round() {
        return IntegerNode.valueOf(Math.round(value));
    }
    
    public IntegerNode sign() {
        return IntegerNode.valueOf((long) Math.signum(value));
    }
    
    public Node simplify(Calculator environment) {
        return this;
    }
    
    public RealNode sin() {
        return new RealNode(Math.sin(value));
    }
    
    public RealNode sinh() {
        return new RealNode(Math.sinh(value));
    }
    
    public RealNode sqrt() {
        return new RealNode(Math.sqrt(value));
    }
    
    public NumberNode subtract(NumberNode rhs) {
        if (!(rhs instanceof RealNode)) {
            rhs = rhs.toReal();
        }
        return new RealNode(value - ((RealNode) rhs).value);
    }
    
    public RealNode tan() {
        return new RealNode(Math.tan(value));
    }
    
    public RealNode tanh() {
        return new RealNode(Math.tanh(value));
    }
    
    public NumberNode times(NumberNode rhs) {
        if (!(rhs instanceof RealNode)) {
            rhs = rhs.toReal();
        }
        return new RealNode(value * ((RealNode) rhs).value);
    }
    
    public double doubleValue() {
        return value;
    }
    
    public RealNode toReal() {
        return this;
    }
    
    public String toInputString() {
        return toString();
    }
    
    @Override public String toString() {
        return Double.toString(value);
    }
}
