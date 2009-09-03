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

public class IntegerNode implements Comparable<IntegerNode>, NumberNode {
    private final BigInteger value;
    
    public static final IntegerNode MINUS_ONE = new IntegerNode(BigInteger.ONE.negate());
    public static final IntegerNode ZERO = new IntegerNode(BigInteger.ZERO);
    public static final IntegerNode ONE = new IntegerNode(BigInteger.ONE);
    
    public IntegerNode(BigInteger value) {
        this.value = value;
    }
    
    public Node evaluate(Calculator environment) {
        return this;
    }
    
    public IntegerNode abs() {
        return new IntegerNode(value.abs());
    }
    
    public IntegerNode bitAnd(IntegerNode rhs) {
        return new IntegerNode(value.and(rhs.value));
    }
    
    public IntegerNode bitNot() {
        return new IntegerNode(value.not());
    }
    
    public IntegerNode bitOr(IntegerNode rhs) {
        return new IntegerNode(value.or(rhs.value));
    }
    
    public IntegerNode bitShiftLeft(IntegerNode rhs) {
        return new IntegerNode(value.shiftLeft(rhs.value.intValue()));
    }
    
    public IntegerNode bitShiftRight(IntegerNode rhs) {
        return new IntegerNode(value.shiftRight(rhs.value.intValue()));
    }
    
    public IntegerNode bitXor(IntegerNode rhs) {
        return new IntegerNode(value.xor(rhs.value));
    }
    
    public int compareTo(IntegerNode rhs) {
        return value.compareTo(rhs.value);
    }
    
    public NumberNode divide(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().divide(rhs);
        }
        BigInteger[] divmod = value.divideAndRemainder(((IntegerNode) rhs).value);
        if (divmod[1].equals(BigInteger.ZERO)) {
            return new IntegerNode(divmod[0]);
        } else {
            return toReal().divide(rhs);
        }
    }
    
    public IntegerNode factorial() {
        final int signum = value.signum();
        if (signum < 0) {
            throw new CalculatorError("factorial requires a non-negative integer argument; got " + value + " instead");
        } else if (signum == 0) {
            return IntegerNode.ONE;
        }
        // Based on fact6 from Richard J Fateman's "Comments on Factorial Programs".
        return new IntegerNode(factorialHelper(value, BigInteger.ONE));
    }
    
    private static BigInteger factorialHelper(BigInteger n, BigInteger m) {
        if (n.compareTo(m) <= 0) {
            return n;
        }
        final BigInteger twoM = BigInteger.valueOf(2).multiply(m); // This seems consistently faster than m.shiftLeft(1)!
        return factorialHelper(n, twoM).multiply(factorialHelper(n.subtract(m), twoM));
    }
    
    public RealNode fractionalPart() {
        return RealNode.ZERO;
    }
    
    public IntegerNode increment() {
        return new IntegerNode(value.add(BigInteger.ONE));
    }
    
    public IntegerNode integerPart() {
        return this;
    }
    
    public BooleanNode isPrime() {
        if (value.bitLength() > 32) {
            throw new CalculatorError("is_prime uses a naive algorithm unsuitable for huge numbers");
        }
        
        // FIXME: replace the naive algorithm with something better.
        int n = Math.abs(value.intValue());
        if (n == 1) {
            return BooleanNode.FALSE;
        }
        if (n == 2) {
            return BooleanNode.TRUE;
        }
        if ((n % 2) == 0) {
            return BooleanNode.FALSE;
        }
        final int max = (int) Math.sqrt(n);
        for (int i = 3; i <= max; i += 2) {
            if (n % i == 0) {
                return BooleanNode.FALSE;
            }
        }
        return BooleanNode.TRUE;
    }
    
    public IntegerNode mod(IntegerNode rhs) {
        return new IntegerNode(value.remainder(rhs.value));
    }
    
    public NumberNode plus(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().plus(rhs);
        }
        return new IntegerNode(value.add(((IntegerNode) rhs).value));
    }
    
    public NumberNode power(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().power(rhs);
        }
        final IntegerNode exponent = (IntegerNode) rhs;
        if (exponent.value.bitLength() > 32) {
            throw new CalculatorError("can't raise " + value + " to the " + exponent + "th power");
        }
        return new IntegerNode(value.pow(exponent.value.intValue()));
    }
    
    public NumberNode subtract(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().subtract(rhs);
        }
        return new IntegerNode(value.subtract(((IntegerNode) rhs).value));
    }
    
    public NumberNode times(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().times(rhs);
        }
        return new IntegerNode(value.multiply(((IntegerNode) rhs).value));
    }
    
    public RealNode toReal() {
        // FIXME: why is MATH_CONTEXT necessary here?
        return new RealNode(new BigDecimal(value, MATH_CONTEXT));
    }
    
    @Override public String toString() {
        return value.toString();
    }
}
