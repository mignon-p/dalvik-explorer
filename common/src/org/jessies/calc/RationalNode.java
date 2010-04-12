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
//import org.jessies.calc.bigint.*;
import org.jessies.test.*;

public class RationalNode implements Comparable<RationalNode>, NumberNode {
    private final IntegerNode p;
    private final IntegerNode q;
    
    private RationalNode(IntegerNode p, IntegerNode q) {
        if (q.compareTo(IntegerNode.ZERO) < 0) {
            // FIXME: we can cope with this if we fiddle the signs; but should we?
            throw new RuntimeException("Internal error: trying to create rational with negative denominator");
        }
        if (p.isBig() || q.isBig()) {
            final BigInteger bigP = p.big();
            final BigInteger bigQ = q.big();
            final BigInteger bigGcd = bigP.abs().gcd(bigQ.abs());
            this.p = IntegerNode.valueOf(bigP.divide(bigGcd));
            this.q = IntegerNode.valueOf(bigQ.abs().divide(bigGcd));
        } else {
            final long fixP = p.fix();
            final long fixQ = q.fix();
            final long fixGcd = gcd(Math.abs(fixP), Math.abs(fixQ));
            this.p = IntegerNode.valueOf(fixP/fixGcd);
            this.q = IntegerNode.valueOf(Math.abs(fixQ)/fixGcd);
        }
    }
    
    static NumberNode valueOf(IntegerNode p, IntegerNode q) {
        if (q.compareTo(IntegerNode.ONE) == 0) {
            return p;
        } else {
            return new RationalNode(p, q);
        }
    }
    
    static long gcd(long a, long b) {
        if (a == 0) {
            return b;
        }
        while (b != 0) {
            if (a > b) {
                a = a - b;
            } else {
                b = b - a;
            }
        }
        return a;
    }
    
    @Override public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RationalNode)) {
            return false;
        }
        RationalNode rhs = (RationalNode) other;
        return p.equals(rhs.p) && q.equals(rhs.q);
    }
    
    @Override public int hashCode() {
        int result = 17;
        result = 31 * result + p.hashCode();
        result = 31 * result + q.hashCode();
        return result;
    }
    
    public IntegerNode numerator() {
        return p;
    }
    
    public IntegerNode denominator() {
        return q;
    }
    
    public RationalNode abs() {
        return new RationalNode(p.abs(), q);
    }
    
    /**
     * Returns -1, 0 or 1 if this RationalNode is less than, equal to, or greater than rhs.
     * The suggested idiom for performing any boolean comparison 'op' is: (x.compareTo(y) op 0).
     */
    public int compareTo(RationalNode rhs) {
        final IntegerNode ad = (IntegerNode) p.times(rhs.q);
        final IntegerNode bc = (IntegerNode) q.times(rhs.p);
        return ad.compareTo(bc);
    }
    
    public NumberNode divide(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().divide(rhs);
        } else if (rhs instanceof IntegerNode) {
            return valueOf(p, (IntegerNode) q.times(rhs));
        } else {
            return divideRational((RationalNode) rhs);
        }
    }
    
    private NumberNode divideRational(RationalNode rhs) {
        return valueOf((IntegerNode) p.times(rhs.q), (IntegerNode) q.times(rhs.p));
    }
    
    public Node evaluate(Calculator environment) {
        return this;
    }
    
    public RealNode fractionalPart() {
        // FIXME: we could do better.
        return toReal().fractionalPart();
    }
    
    public NumberNode increment() {
        return this.plus(IntegerNode.ONE);
    }
    
    public IntegerNode integerPart() {
        // FIXME: we could do better.
        return toReal().integerPart();
    }
    
    public NumberNode plus(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().plus(rhs);
        } else if (rhs instanceof IntegerNode) {
            return valueOf((IntegerNode) p.plus(rhs.times(q)), q);
        } else {
            return plusRational((RationalNode) rhs);
        }
    }
    
    private NumberNode plusRational(RationalNode rhs) {
        return valueOf((IntegerNode) p.times(rhs.q).plus(q.times(rhs.p)), (IntegerNode) q.times(rhs.q));
    }
    
    public NumberNode power(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().power(rhs);
        } else if (rhs instanceof IntegerNode) {
            return valueOf((IntegerNode) p.power(rhs), (IntegerNode) q.power(rhs));
        } else {
            // FIXME: be clever?
            return toReal().power(rhs);
        }
    }
    
    public IntegerNode sign() {
        return p.sign();
    }
    
    public Node simplify(Calculator environment) {
        return this;
    }
    
    public NumberNode subtract(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().subtract(rhs);
        } else if (rhs instanceof IntegerNode) {
            return valueOf((IntegerNode) p.subtract(rhs.times(q)), q);
        } else {
            return subtractRational((RationalNode) rhs);
        }
    }
    
    private NumberNode subtractRational(RationalNode rhs) {
        return valueOf((IntegerNode) p.times(rhs.q).subtract(q.times(rhs.p)), (IntegerNode) q.times(rhs.q));
    }
    
    public NumberNode times(NumberNode rhs) {
        if (rhs instanceof RealNode) {
            return toReal().times(rhs);
        } else if (rhs instanceof IntegerNode) {
            return valueOf((IntegerNode) p.times(rhs), q);
        } else {
            return timesRational((RationalNode) rhs);
        }
    }
    
    private NumberNode timesRational(RationalNode rhs) {
        return valueOf((IntegerNode) p.times(rhs.p), (IntegerNode) q.times(rhs.q));
    }
    
    public RealNode toReal() {
        return (RealNode) p.toReal().divide(q.toReal());
    }
    
    public String toInputString() {
        return toString();
    }
    
    @Override public String toString() {
        return "(" + p.toString() + "/" + q.toString() + ")";
    }
    
    @Test private static void testRationalArithmetic() {
        // A rational that can represented as an integer will be.
        Assert.equals(makeRational(2, 1), IntegerNode.valueOf(2));
        // Rationals are stored in their simplest form, so 6/8 will be 3/4.
        RationalNode r = (RationalNode) makeRational(6, 8);
        Assert.equals(r.numerator(), IntegerNode.valueOf(3));
        Assert.equals(r.denominator(), IntegerNode.valueOf(4));
    }
    
    @TestHelper private static NumberNode makeRational(long numerator, long denominator) {
        return RationalNode.valueOf(IntegerNode.valueOf(numerator), IntegerNode.valueOf(denominator));
    }
}
