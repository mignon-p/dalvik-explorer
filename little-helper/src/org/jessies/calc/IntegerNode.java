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

public class IntegerNode implements Comparable<IntegerNode>, NumberNode {
    // Cache common values, equivalent to what the JLS mandates for boxed integers in Java.
    private static final IntegerNode[] cache = new IntegerNode[-(-128) + 127 + 1];
    private static final int CACHE_OFFSET = 128;
    static {
        for(int i = 0; i < cache.length; ++i) {
            cache[i] = new IntegerNode(i - CACHE_OFFSET);
        }
    }
    
    // Used by factorial.
    private static final BigInteger BIG_INTEGER_TWO = BigInteger.valueOf(2);
    
    // If 'bignum' is null, this IntegerNode's value is 'fixnum'. Otherwise, it's 'bignum' and 'fixnum' is ignored.
    private long fixnum;
    private BigInteger bignum;
    
    // Internally, we often need to distinguish fixnum IntegerNodes from bignum ones.
    boolean isBig() {
        return bignum != null;
    }
    
    // Internally, we often want to treat an IntegerNode as if it was a bignum, whether it is or not.
    private BigInteger big() {
        return (bignum != null) ? bignum : BigInteger.valueOf(fixnum);
    }
    
    // Internally, we often want to treat an IntegerNode as if it was a fixnum, whether it is or not.
    private int intValue() {
        return isBig() ? big().intValue() : (int) fixnum;
    }
    
    public static IntegerNode valueOf(long l) {
        if (l >= -128 && l <= 127) {
            return cache[CACHE_OFFSET + (int) l];
        }
        return new IntegerNode(l);
    }
    
    public IntegerNode(String digits, int base) {
        try {
            this.fixnum = Long.parseLong(digits, base);
        } catch (NumberFormatException ex) {
            this.bignum = new BigInteger(digits, base);
        }
    }
    
    private IntegerNode(long value) {
        this.fixnum = value;
    }
    
    private IntegerNode(BigInteger value) {
        // Collapse to a fixnum if possible.
        if ((value.bitLength() + 1) <= 64) {
            this.fixnum = value.longValue();
        } else {
            this.bignum = value;
        }
    }
    
    private static IntegerNode valueOf(BigInteger value) {
        return new IntegerNode(value);
    }
    
    public Node evaluate(Calculator environment) {
        return this;
    }
    
    public IntegerNode abs() {
        if (isBig() || fixnum == Long.MIN_VALUE) {
            return new IntegerNode(big().abs());
        } else {
            return IntegerNode.valueOf(Math.abs(fixnum));
        }
    }
    
    public IntegerNode bitAnd(IntegerNode rhs) {
        if (isBig() || rhs.isBig()) {
            return IntegerNode.valueOf(big().and(rhs.big()));
        } else {
            return IntegerNode.valueOf(fixnum & rhs.fixnum);
        }
    }
    
    public IntegerNode bitNot() {
        if (isBig()) {
            return IntegerNode.valueOf(bignum.not());
        } else {
            return IntegerNode.valueOf(~fixnum);
        }
    }
    
    public IntegerNode bitOr(IntegerNode rhs) {
        if (isBig() || rhs.isBig()) {
            return IntegerNode.valueOf(big().or(rhs.big()));
        } else {
            return IntegerNode.valueOf(fixnum | rhs.fixnum);
        }
    }
    
    public IntegerNode bitShiftLeft(IntegerNode rhs) {
        // FIXME: check that rhs not too large?
        if (isBig() || rhs.isBig()) {
            return new IntegerNode(big().shiftLeft(rhs.intValue()));
        } else {
            return IntegerNode.valueOf(fixnum << rhs.fixnum);
        }
    }
    
    public IntegerNode bitShiftRight(IntegerNode rhs) {
        // FIXME: check that rhs not too large?
        if (isBig() || rhs.isBig()) {
            return IntegerNode.valueOf(big().shiftRight(rhs.intValue()));
        } else {
            return IntegerNode.valueOf(fixnum >> rhs.fixnum);
        }
    }
    
    public IntegerNode bitXor(IntegerNode rhs) {
        if (isBig() || rhs.isBig()) {
            return IntegerNode.valueOf(big().xor(rhs.big()));
        } else {
            return IntegerNode.valueOf(fixnum ^ rhs.fixnum);
        }
    }
    
    /**
     * Returns -1, 0 or 1 if this IntegerNode is less than, equal to, or greater than rhs.
     * The suggested idiom for performing any boolean comparison 'op' is: (x.compareTo(y) op 0).
     */
    public int compareTo(IntegerNode rhs) {
        if (isBig() || rhs.isBig()) {
            return big().compareTo(rhs.big());
        } else {
            if (fixnum < rhs.fixnum) {
                return -1;
            } else if (fixnum == rhs.fixnum) {
                return 0;
            } else {
                return 1;
            }
        }
    }
    
    public NumberNode divide(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().divide(rhs);
        }
        
        IntegerNode iRhs = (IntegerNode) rhs;
        if (isBig() || iRhs.isBig()) {
            BigInteger[] divmod = big().divideAndRemainder(iRhs.big());
            if (divmod[1].equals(BigInteger.ZERO)) {
                return new IntegerNode(divmod[0]);
            } else {
                return toReal().divide(rhs);
            }
        } else {
            final long a = fixnum;
            final long b = iRhs.fixnum;
            // https://www.securecoding.cert.org/confluence/display/seccode/INT32-C.+Ensure+that+operations+on+signed+integers+do+not+result+in+overflow?showComments=false
            if ((b == 0) || (a == Long.MIN_VALUE && b == -1)) {
                return IntegerNode.valueOf(a / b);
            } else {
                return toReal().divide(rhs);
            }
        }
    }
    
    public IntegerNode factorial() {
        final BigInteger n = big();
        final int signum = n.signum();
        if (signum < 0) {
            throw new IllegalArgumentException("factorial requires a non-negative integer argument; got " + this + " instead");
        } else if (signum == 0) {
            return IntegerNode.valueOf(1);
        }
        // Based on fact6 from Richard J Fateman's "Comments on Factorial Programs".
        return IntegerNode.valueOf(factorialHelper(n, BigInteger.ONE));
    }
    
    private static BigInteger factorialHelper(BigInteger n, BigInteger m) {
        if (n.compareTo(m) <= 0) {
            return n;
        }
        final BigInteger twoM = BIG_INTEGER_TWO.multiply(m); // This seems consistently faster than m.shiftLeft(1)!
        return factorialHelper(n, twoM).multiply(factorialHelper(n.subtract(m), twoM));
    }
    
    public RealNode fractionalPart() {
        return RealNode.ZERO;
    }
    
    public IntegerNode increment() {
        if (isBig() || fixnum == Long.MAX_VALUE) {
            return new IntegerNode(big().add(BigInteger.ONE));
        } else {
            return IntegerNode.valueOf(fixnum + 1);
        }
    }
    
    public IntegerNode integerPart() {
        return this;
    }
    
    public BooleanNode isPrime() {
        if (isBig() || fixnum > Integer.MAX_VALUE || fixnum <= Integer.MIN_VALUE) {
            throw new CalculatorError("IsPrime uses a naive algorithm unsuitable for huge numbers");
        }
        
        // FIXME: replace the naive algorithm with something better.
        int n = Math.abs((int) fixnum);
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
        if (isBig() || rhs.isBig()) {
            return IntegerNode.valueOf(big().remainder(rhs.big()));
        } else {
            return IntegerNode.valueOf(fixnum % rhs.fixnum);
        }
    }
    
    public NumberNode plus(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().plus(rhs);
        }
        
        IntegerNode iRhs = (IntegerNode) rhs;
        if (isBig() || iRhs.isBig()) {
            return IntegerNode.valueOf(big().add(iRhs.big()));
        } else {
            final long a = fixnum;
            final long b = iRhs.fixnum;
            final long c = a + b;
            // https://www.securecoding.cert.org/confluence/display/seccode/INT32-C.+Ensure+that+operations+on+signed+integers+do+not+result+in+overflow?showComments=false
            if ((a > 0 && c < 0) || (a < 0 && c > 0)) {
                return new IntegerNode(big().add(iRhs.big()));
            } else {
                return IntegerNode.valueOf(c);
            }
        }
    }
    
    public NumberNode power(NumberNode rhs) {
        if (rhs instanceof RealNode || rhs.sign().compareTo(IntegerNode.valueOf(-1)) == 0) {
            return toReal().power(rhs);
        }
        // FIXME: special-case small enough fixnums?
        final IntegerNode exponent = (IntegerNode) rhs;
        if (exponent.isBig() || exponent.fixnum > Integer.MAX_VALUE) {
            throw new CalculatorError("can't raise " + this + " to the " + exponent + "th power");
        }
        return new IntegerNode(big().pow(exponent.intValue()));
        
    }
    
    public IntegerNode sign() {
        if (isBig()) {
            return IntegerNode.valueOf(bignum.signum());
        }
        if (fixnum < 0) {
            return IntegerNode.valueOf(-1);
        } else if (fixnum > 0) {
            return IntegerNode.valueOf(1);
        } else {
            return IntegerNode.valueOf(0);
        }
    }
    
    public NumberNode subtract(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().subtract(rhs);
        }
        
        IntegerNode iRhs = (IntegerNode) rhs;
        if (isBig() || iRhs.isBig()) {
            return IntegerNode.valueOf(big().subtract(iRhs.big()));
        } else {
            final long a = fixnum;
            final long b = iRhs.fixnum;
            final long c = a - b;
            if ((c ^ a) < 0 && (c ^ ~b) < 0) {
                return new IntegerNode(big().subtract(iRhs.big()));
            } else {
                return IntegerNode.valueOf(c);
            }
        }
    }
    
    public NumberNode times(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().times(rhs);
        }
        
        IntegerNode iRhs = (IntegerNode) rhs;
        if (isBig() || iRhs.isBig()) {
            return IntegerNode.valueOf(big().multiply(iRhs.big()));
        } else {
            final long a = fixnum;
            final long b = iRhs.fixnum;
            // https://www.securecoding.cert.org/confluence/display/seccode/INT32-C.+Ensure+that+operations+on+signed+integers+do+not+result+in+overflow?showComments=false
            boolean overflow;
            if (a > 0) {
                overflow = ((b > 0) && (a > Long.MAX_VALUE / b)) || ((b <= 0) && (b < Long.MIN_VALUE / a));
            } else {
                overflow = ((b > 0) && (a < Long.MIN_VALUE / b)) || ((b <= 0) && (a != 0 && b < (Long.MAX_VALUE / a)));
            }
            if (overflow) {
                return new IntegerNode(big().multiply(iRhs.big()));
            } else {
                return IntegerNode.valueOf(a*b);
            }
        }
    }
    
    public RealNode toReal() {
        double result = isBig() ? big().doubleValue() : (double) fixnum;
        if (result == Double.NEGATIVE_INFINITY || result == Double.POSITIVE_INFINITY) {
            throw new RuntimeException("Integer value too large");
        }
        return new RealNode(result);
    }
    
    @Override public String toString() {
        if (isBig()) {
            return big().toString();
        } else {
            return Long.toString(fixnum);
        }
    }
}
