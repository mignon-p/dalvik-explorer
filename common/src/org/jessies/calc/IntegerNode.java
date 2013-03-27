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

public class IntegerNode implements Comparable<IntegerNode>, NumberNode {
    // Cache common values, equivalent to what the JLS mandates for boxed integers in Java.
    private static final IntegerNode[] cache = new IntegerNode[-(-128) + 127 + 1];
    private static final int CACHE_OFFSET = 128;
    static {
        for(int i = 0; i < cache.length; ++i) {
            cache[i] = new IntegerNode(i - CACHE_OFFSET);
        }
    }

    // Insanely popular values.
    public static final IntegerNode MINUS_ONE = IntegerNode.valueOf(-1);
    public static final IntegerNode ZERO = IntegerNode.valueOf(0);
    public static final IntegerNode ONE = IntegerNode.valueOf(1);

    // Used by factorial.
    private static final BigInteger BIG_INTEGER_TWO = BigInteger.valueOf(2);

    // If 'bignum' is null, this IntegerNode's value is 'fixnum'. Otherwise, it's 'bignum' and 'fixnum' is ignored.
    private final long fixnum;
    private final BigInteger bignum;

    // Internally, we often need to distinguish fixnum IntegerNodes from bignum ones.
    boolean isBig() {
        return bignum != null;
    }

    // Internally, we often want to treat an IntegerNode as if it was a bignum, whether it is or not.
    BigInteger big() {
        return (bignum != null) ? bignum : BigInteger.valueOf(fixnum);
    }

    long fix() {
        if (isBig()) {
            throw new RuntimeException("Internal error: trying to use bignum as fixnum");
        }
        return fixnum;
    }

    // Internally, we often want to treat an IntegerNode as if it was a int, whether it is or not.
    int intValue() {
        return isBig() ? big().intValue() : (int) fixnum;
    }

    public static IntegerNode valueOf(long l) {
        if (l >= -128 && l <= 127) {
            return cache[CACHE_OFFSET + (int) l];
        }
        return new IntegerNode(l);
    }

    public IntegerNode(String digits, int base) {
        boolean small = false;
        long smallValue = 0;
        try {
            smallValue = Long.parseLong(digits, base);
            small = true;
        } catch (NumberFormatException ex) {
        }
        if (small) {
            this.bignum = null;
            this.fixnum = smallValue;
        } else {
            this.bignum = new BigInteger(digits, base);
            this.fixnum = 0;
        }
    }

    private IntegerNode(long value) {
        this.bignum = null;
        this.fixnum = value;
    }

    private IntegerNode(BigInteger value) {
        // Collapse to a fixnum if possible.
        if ((value.bitLength() + 1) <= 64) {
            this.fixnum = value.longValue();
            this.bignum = null;
        } else {
            this.bignum = value;
            this.fixnum = 0;
        }
    }

    static IntegerNode valueOf(BigInteger value) {
        return new IntegerNode(value);
    }

    @Override public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IntegerNode)) {
            return false;
        }
        IntegerNode rhs = (IntegerNode) other;
        if (fixnum != rhs.fixnum) {
            return false;
        }
        return (bignum == null ? rhs.bignum == null : bignum.equals(rhs.bignum));
    }

    @Override public int hashCode() {
        int result = 17;
        result = 31 * result + (int) (fixnum ^ (fixnum >>> 32));
        result = 31 * result + (bignum == null ? 0 : bignum.hashCode());
        return result;
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

    private static int bitIndex(IntegerNode k) {
        if (k.isBig() || k.fix() > Integer.MAX_VALUE) {
            throw new CalculatorError("bit index too large");
        } else if (k.compareTo(IntegerNode.ZERO) < 0) {
            throw new CalculatorError("bit index negative");
        }
        return (int) k.fix();
    }

    public IntegerNode bitClear(IntegerNode k) {
        return IntegerNode.valueOf(big().clearBit(bitIndex(k)));
    }

    public IntegerNode bitGet(IntegerNode k) {
        return big().testBit(bitIndex(k)) ? IntegerNode.ONE : IntegerNode.ZERO;
    }

    public IntegerNode bitLength() {
        return IntegerNode.valueOf(big().bitLength());
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

    public IntegerNode bitSet(IntegerNode k) {
        return IntegerNode.valueOf(big().setBit(bitIndex(k)));
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
        if (rhs instanceof BigRealNode) {
            return toBigReal().divide(rhs);
        } else if (rhs instanceof RealNode) {
            return toReal().divide(rhs);
        }

        try {
            IntegerNode iRhs = (IntegerNode) rhs;
            BigInteger[] divmod = big().divideAndRemainder(iRhs.big());
            if (divmod[1].equals(BigInteger.ZERO)) {
                return new IntegerNode(divmod[0]);
            } else {
                return toReal().divide(rhs);
            }
        } catch (ArithmeticException ex) {
            throw new CalculatorError("division by zero");
        }
    }

    public IntegerNode factorial() {
        final BigInteger n = big();
        final int signum = n.signum();
        if (signum < 0) {
            throw new IllegalArgumentException("factorial requires a non-negative integer argument; got " + this + " instead");
        } else if (signum == 0) {
            return IntegerNode.ONE;
        }
        // Based on fact6 from Richard J Fateman's "Comments on Factorial Programs".
        return IntegerNode.valueOf(factorialHelper(n, BigInteger.ONE));
    }

    public static IntegerNode gcd(IntegerNode a, IntegerNode b) {
        // If you special-case !isBig, don't forget to test Long.MIN_VALUE!
        return IntegerNode.valueOf(a.big().abs().gcd(b.big().abs()));
    }

    private static BigInteger factorialHelper(BigInteger n, BigInteger m) {
        if (n.compareTo(m) <= 0) {
            return n;
        }
        // shiftLeft(1) - 550145094 ns
        // multiply()   - 535161265 ns
        final BigInteger twoM = BIG_INTEGER_TWO.multiply(m); // This seems consistently faster than m.shiftLeft(1)!
        return factorialHelper(n, twoM).multiply(factorialHelper(n.subtract(m), twoM));
    }

    public NumberNode fractionalPart() {
        return IntegerNode.ZERO;
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
        return isPrime((int) fixnum);
    }

    private static BooleanNode isPrime(int n) {
      // FIXME: replace the naive algorithm with something better.
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

    public ListNode primeFactors() {
      if (isBig() || fixnum > Integer.MAX_VALUE || fixnum <= Integer.MIN_VALUE) {
        throw new CalculatorError("Factors uses a naive algorithm unsuitable for huge numbers");
      }

      // FIXME: replace the naive algorithm with something better.
      int n = Math.abs((int) fixnum);

      final ListNode result = new ListNode();
      for (int factor = 2; factor <= n; ++factor) {
        if ((n % factor) == 0) {
          result.add(IntegerNode.valueOf(factor));
          n /= factor;
          --factor; // Try this factor again.
        }
      }
      if (fixnum < 0) {
        result.set(0, IntegerNode.ZERO.subtract((IntegerNode) result.get(0)));
      }
      return result;
    }

    public ListNode divisors() {
      if (isBig() || fixnum > Integer.MAX_VALUE || fixnum <= Integer.MIN_VALUE) {
        throw new CalculatorError("Factors uses a naive algorithm unsuitable for huge numbers");
      }

      // FIXME: replace the naive algorithm with something better.
      int n = Math.abs((int) fixnum);

      final ListNode result = new ListNode();

      // TODO: only need to go to sqrt(n) if we build the list from both ends by actually dividing.
      for (int i = 1; i <= n; ++i) {
        if ((n % i) == 0) {
          result.add(IntegerNode.valueOf(i));
        }
      }
      return result;
    }

    public IntegerNode mod(IntegerNode rhs) {
        try {
            if (isBig() || rhs.isBig()) {
                return IntegerNode.valueOf(big().remainder(rhs.big()));
            } else {
                return IntegerNode.valueOf(fixnum % rhs.fixnum);
            }
        } catch (ArithmeticException ex) {
            throw new CalculatorError("division by zero");
        }
    }

    public NumberNode plus(NumberNode rhs) {
        if (rhs instanceof BigRealNode) {
            return toBigReal().plus(rhs);
        } else if (rhs instanceof RealNode) {
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
        if (rhs instanceof BigRealNode || rhs instanceof RealNode || rhs.sign().compareTo(MINUS_ONE) == 0) {
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
            return IntegerNode.MINUS_ONE;
        } else if (fixnum > 0) {
            return IntegerNode.ONE;
        } else {
            return IntegerNode.ZERO;
        }
    }

    public Node simplify(Calculator environment) {
        return this;
    }

    public NumberNode subtract(NumberNode rhs) {
        if (rhs instanceof BigRealNode) {
            return toBigReal().subtract(rhs);
        } else if (rhs instanceof RealNode) {
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
        if (rhs instanceof BigRealNode) {
            return toBigReal().times(rhs);
        } else if (rhs instanceof RealNode) {
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

    public BigRealNode toBigReal() {
        return new BigRealNode(new BigDecimal(big()));
    }

    public RealNode toReal() {
        double result = isBig() ? big().doubleValue() : (double) fixnum;
        if (result == Double.NEGATIVE_INFINITY || result == Double.POSITIVE_INFINITY) {
            throw new RuntimeException("Integer value too large");
        }
        return new RealNode(result);
    }

  public String toInputString() {
    return toInputString(10);
  }

  public String toInputString(int base) {
    String result;
    if (isBig()) {
      result = big().toString(base);
    } else {
      result = Long.toString(fixnum, base);
    }
    if (base != 10) {
      result = prefix(base) + result;
    }
    return result;
  }

  public String toString(int base) {
    String result = toInputString(base);
    if (base == 10) {
      result = insertCharEveryNDigits(result, ',', 3);
    }
    return result;
  }

  private static String insertCharEveryNDigits(String s, char ch, int n) {
    StringBuilder result = new StringBuilder(s);
    int end = s.length();
    if (s.charAt(0) == '-') {
      --end;
    }
    for (int i = 1; i < end; ++i) {
      if ((i % n) == 0) {
        result.insert(s.length() - i, ch);
      }
    }
    return result.toString();
  }

  private static String prefix(int base) {
    if (base == 2) {
      return "0b";
    } else if (base == 8) {
      return "0o";
    } else if (base == 10) {
      return "";
    } else if (base == 16) {
      return "0x";
    } else {
      throw new CalculatorError("Unsupported base " + base);
    }
  }

  @Override public String toString() {
    return toString(Calculator.getOutputBase());
  }
}
