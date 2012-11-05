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

import java.math.*;

/**
 * Represents an arbitrary precision approximate number.
 */
public class BigRealNode implements Comparable<BigRealNode>, NumberNode {
    public static final BigRealNode ZERO = new BigRealNode(BigDecimal.ZERO);

    private final BigDecimal value;

    public BigRealNode(BigDecimal value) {
        this.value = value;
    }

    public BigRealNode(String s) {
        this(new BigDecimal(s));
    }

    public Node evaluate(Calculator environment) {
        return this;
    }

    public BigRealNode abs() {
        return new BigRealNode(value.abs());
    }

    public int compareTo(BigRealNode rhs) {
        return value.compareTo(rhs.value);
    }

    public NumberNode divide(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().divide(rhs);
        }
        BigDecimal divisor;
        if (rhs instanceof IntegerNode) {
            IntegerNode iRhs = (IntegerNode) rhs;
            divisor = new BigDecimal(iRhs.big());
        } else {
            BigRealNode bRhs = (BigRealNode) rhs;
            divisor = bRhs.value;
        }
        try {
            return new BigRealNode(value.divide(divisor, MathContext.DECIMAL128));
        } catch (ArithmeticException ex) {
            throw new CalculatorError("division by zero");
        }
    }

    @Override public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BigRealNode)) {
            return false;
        }
        BigRealNode rhs = (BigRealNode) other;
        return (value.compareTo(rhs.value) == 0);
    }

    @Override public int hashCode() {
        return value.hashCode();
    }

    public NumberNode fractionalPart() {
        BigDecimal integerPart = new BigDecimal(value.toBigInteger());
        if (value.equals(integerPart)) {
            return IntegerNode.ZERO;
        }
        return new BigRealNode(value.subtract(integerPart).abs());
    }

    public NumberNode increment() {
        return new BigRealNode(value.add(BigDecimal.ONE));
    }

    public IntegerNode integerPart() {
        return IntegerNode.valueOf(value.toBigInteger());
    }

    public NumberNode plus(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().plus(rhs);
        } else if (rhs instanceof IntegerNode) {
            IntegerNode iRhs = (IntegerNode) rhs;
            return new BigRealNode(value.add(new BigDecimal(iRhs.big())));
        } else {
            BigRealNode bRhs = (BigRealNode) rhs;
            return new BigRealNode(value.add(bRhs.value));
        }
    }

    public NumberNode power(NumberNode rhs) {
        if (rhs instanceof IntegerNode) {
            IntegerNode iRhs = (IntegerNode) rhs;
            if (!iRhs.isBig() && iRhs.fix() <= Integer.MAX_VALUE) {
                return new BigRealNode(value.pow((int) iRhs.fix()));
            }
        }
        return toReal().power(rhs);
    }

    public IntegerNode sign() {
        return IntegerNode.valueOf(value.signum());
    }

    public Node simplify(Calculator environment) {
        return this;
    }

    public NumberNode subtract(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().subtract(rhs);
        } else if (rhs instanceof IntegerNode) {
            IntegerNode iRhs = (IntegerNode) rhs;
            return new BigRealNode(value.subtract(new BigDecimal(iRhs.big())));
        } else {
            BigRealNode bRhs = (BigRealNode) rhs;
            return new BigRealNode(value.subtract(bRhs.value));
        }
    }

    public NumberNode times(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().times(rhs);
        } else if (rhs instanceof IntegerNode) {
            IntegerNode iRhs = (IntegerNode) rhs;
            return new BigRealNode(value.multiply(new BigDecimal(iRhs.big())));
        } else {
            BigRealNode bRhs = (BigRealNode) rhs;
            return new BigRealNode(value.multiply(bRhs.value));
        }
    }

    public double doubleValue() {
        return value.doubleValue();
    }

    public RealNode toReal() {
        return new RealNode(doubleValue());
    }

    public String toInputString() {
        return toString();
    }

    @Override public String toString() {
        return value.toPlainString();
    }
}
