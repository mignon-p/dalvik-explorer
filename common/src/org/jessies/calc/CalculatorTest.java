package org.jessies.calc;

/*
 * This file is part of org.jessies.calc.
 * Copyright (C) 2011 Elliott Hughes <enh@jessies.org>.
 *
 * This is free software; you can redistribute it and/or modify
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

import junit.framework.*;

public class CalculatorTest extends TestCase {
  private void check(Calculator c, boolean formattedString, String expected, String input) {
    Node result = c.evaluate(input);
    String actual = (formattedString ? result.toString() : result.toInputString());
    if (!actual.equals(expected)) {
      throw new AssertionFailedError(input + " should be " + expected + " but was " + actual);
    }
  }

  private void check(Calculator c, String expected, String input) {
    check(c, false, expected, input);
  }

  private void check(String expected, String input) {
    check(new Calculator(), expected, input);
  }

  private void checkFormatted(String expected, String input) {
    check(new Calculator(), true, expected, input);
  }

  private void check(double d, String input, double tolerance) {
    Node result = new Calculator().evaluate(input);
    double actual = ((NumberNode) result).toReal().doubleValue();
    if (Math.abs(actual - d) > tolerance) {
      throw new AssertionFailedError(input + " should be " + d + " but was " + result);
    }
  }

  public void testFormattedOutput() {
    checkFormatted("1", "1");
    checkFormatted("-1", "-1");
    checkFormatted("12", "12");
    checkFormatted("-12", "-12");
    checkFormatted("123", "123");
    checkFormatted("-123", "-123");
    checkFormatted("1,234", "1234");
    checkFormatted("-1,234", "-1234");
    checkFormatted("12,345", "12345");
    checkFormatted("-12,345", "-12345");
  }

  public void testArithmetic() {
    check("0", "0");
    check("1", "1");
    check("-1", "-1");
    check("1", "--1");
    check("1.00", "1.00");

    check("0.2", ".2");

    check("1200", "1.2E3");
    check("1200", "1.2e3");
    check("1000", "1E3");
    check("0.001", "1E-3");
    check("1000", "1.E3");
    check("100", ".1E3");

    check("6", "1+2+3");
    check("-1", "1+-2");
    check("0", "3-2-1");
    check("10000.001", "10000+0.001");
    check("10000.001", "0.001+10000");
    check("9999.999", "10000-0.001");
    check("-9999.999", "0.001-10000");

    // Check that we're doing decimal rather than binary arithmetic.
    check("0.1587", "1-0.8413");
    check("131.2", "328*0.4");

    check("12", "3*4");
    check("-12", "-3*4");
    check("-12", "3*-4");
    check("12", "-3*-4");

    check("7", "1+2*3");
    check("9", "(1+2)*3");

    check("0.5", "1/2");

    check("3", "3%4");
    check("0", "4%4");
    check("1", "5%4");
  }

  public void testRationalArithmetic() {
    // A rational that can represented as an integer will be.
    assertEquals(IntegerNode.valueOf(2), makeRational(2, 1));
    // Rationals are stored in their simplest form, so 6/8 will be 3/4.
    RationalNode r = (RationalNode) makeRational(6, 8);
    assertEquals(IntegerNode.valueOf(3), r.numerator());
    assertEquals(IntegerNode.valueOf(4), r.denominator());
  }

  private static NumberNode makeRational(long numerator, long denominator) {
    return RationalNode.valueOf(IntegerNode.valueOf(numerator), IntegerNode.valueOf(denominator));
  }

  public void testRelationalOperations() {
    check("true", "1<2");
    check("false", "2<2");
    check("false", "2<1");
    check("true", "1<=2");
    check("true", "2<=2");
    check("false", "2<=1");
    check("false", "1>2");
    check("false", "2>2");
    check("true", "2>1");
    check("false", "1>=2");
    check("true", "2>=2");
    check("true", "2>=1");
    check("false", "1==2");
    check("true", "2==2");
    check("false", "2==1");
    check("true", "1!=2");
    check("false", "2!=2");
    check("true", "2!=1");
    check("true", "true==true");
    check("false", "true==false");
    check("false", "false==true");
    check("true", "false==false");
    check("false", "true!=true");
    check("true", "true!=false");
    check("true", "false!=true");
    check("false", "false!=false");
    check("true", "[]==[]");
    check("true", "[[]]==[[]]");
    check("true", "[[], []]==[[], []]");
    check("true", "[0]==[0]");
    check("false", "[0]==[1]");
    check("true", "[0, 1]==[0, 1]");
    check("false", "[0, 1]==[1, 0]");
    check("false", "[0, 1]==[0, [1]]");
    check("true", "[0, [1]]==[0, [1]]");
  }

  public void testLogicalNot() {
    check("true", "!false");
    check("false", "!true");
    check("true", "!(1==2)");
    check("false", "!(2==2)");
    check("true", "!!(2==2)");
  }

  public void testLogicalAnd() {
    check("false", "false&&false");
    check("false", "false&&true");
    check("false", "true&&false");
    check("true", "true&&true");
  }

  public void testLogicalOr() {
    check("false", "false||false");
    check("true", "false||true");
    check("true", "true||false");
    check("true", "true||true");
  }

  public void testShifts() {
    check("16", "1<<4");
    check("12", "(12<<3)>>3");
  }

  public void testBitAnd() {
    check("true", "(0x1234 & 0xff0) == 0x230");
  }

  public void testBitOr() {
    check("true", "(0x1200 | 0x34) == 0x1234");
  }

  public void testBitXor() {
    check("6", "BitXor(5, 3)");
  }

  public void testBitNot() {
    check("true", "((0x1234 & ~0xff) | 0x56) == 0x1256");
    check("-4", "~3");
    check("3", "~~3");
  }

  public void testBitLength() {
    check("0", "BitLength(0)");
    check("6", "BitLength(32)");
    check("8", "BitLength(255)");
    check("9", "BitLength(256)");
    check("9", "BitLength(257)");
    check("8", "BitLength(-255)");
    check("8", "BitLength(-256)");
    check("9", "BitLength(-257)");
  }

  public void testBitGet() {
    check("1", "BitGet(5, 0)");
    check("0", "BitGet(5, 1)");
    check("1", "BitGet(5, 2)");
    check("0", "BitGet(5, 3)");
    check("0", "BitGet(5, 4)");
  }

  public void testBitClear() {
    check("0", "BitClear(0, 0)");
    check("0", "BitClear(1, 0)");
    check("4", "BitClear(5, 0)");
    check("5", "BitClear(5, 16)");
  }

  public void testBitSet() {
    check("1", "BitSet(0, 0)");
    check("5", "BitSet(1, 2)");
  }

  public void testExponentiation() {
    check("8", "2^3");
    check("2417851639229258349412352", "2^3^4");
    check("2.0", "4^0.5");
    check("100", "-10^2");
    check("100", "(-10)^2");
    check("0.01", "10^-2");
    check("0.01", "10^(-2)");
  }

  public void testConstants() {
    check(Math.E, "e", 0.000001);
    check(Math.PI, "pi", 0.000001);
    check("true", "pi == \u03c0");
    try {
      new Calculator().evaluate("pi = 3");
      fail("no exception was thrown when assigning to a constant!");
    } catch (CalculatorError ex) {
      assertEquals("can't assign a new value to the constant pi", ex.getMessage());
    }
  }

  public void testBigIntegers() {
    // Arithmetic tests (from http://www.isthe.com/chongo/tech/comp/calc/calc-whatis.html).
    check("29075426613099201338473141505176993450849249622191102976", "3 * 19^43 - 1");
    check("39614081257132168796771975167", "Mod(2^23209-1, 2^127-1)");

    // Check switches from fix to big.
    check("true", "Abs(-(0x8000)) == 0x8000");
    check("true", "Abs(-(0x8000000000000000)) == 0x8000000000000000");
    check("true", "-(0x8000000000000000) == -0x8000000000000000");
    check("true", "-1 * 0x8000000000000000 == -0x8000000000000000");
    check("true", "0x7fffffffffffffff + 1 == 0x8000000000000000");
    check("true", "(-(0x8000000000000000)) - 1 == -0x8000000000000001");
    check("true", "-1 * 0x8000000000000000 - 1 == -0x8000000000000001");
    check("true", "-(-(0x8000000000000000)) == 0x8000000000000000");
    check("true", "-1 * -1 * 0x8000000000000000 == 0x8000000000000000");
    check("true", "0x8000000000000000/-1 == -0x8000000000000000");
  }

  public void testAbs() {
    check("2", "Abs(2)");
    check("2", "Abs(-2)");
  }

  public void testAcos() {
    check("0.0", "Acos(1)");
    check("true", "Acos(0) == Asin(1)");
  }

  public void testAsin() {
    check("0.0", "Asin(0)");
    check("true", "Asin(1) == Acos(0)");
  }

  public void testAtan() {
    check("0.0", "Atan(0)");
  }

  public void testBoole() {
    check("1", "Boole(true)");
    check("0", "Boole(false)");
  }

  public void testCbrt() {
    check("3.0", "Cbrt(27)");
  }

  public void testCeil() {
    check("2.0", "Ceil(1.2)");
  }

  public void testCos() {
    check("1.0", "Cos(0)");
    check("-1.0", "Cos(pi)");
  }

  public void testCosh() {
    check("1.0", "Cosh(0)");
  }

  public void testExp() {
    check(1.0, "Exp(1)/e", 0.000001);
  }

  public void testFloor() {
    check("1.0", "Floor(1.2)");
  }

  public void testHypot() {
    check("5.0", "Hypot(3, 4)");
  }

  public void testIsPrime() {
    check("false", "IsPrime(0)");
    check("false", "IsPrime(1)");
    check("true", "IsPrime(2)");
    check("true", "IsPrime(3)");
    check("false", "IsPrime(4)");
    check("true", "IsPrime(5)");
    check("false", "IsPrime(-4)");
    check("true", "IsPrime(-5)");
  }

  public void testLog() {
    check("10.0", "Log(2, 1024)");
  }

  public void testLog2() {
    check("10.0", "Log2(1024)");
  }

  public void testLogE() {
    check("4.0", "LogE(exp(4))");
  }

  public void testLog10() {
    check("3.0", "Log10(1000)");
  }

  public void testRound() {
    check("1", "Round(1.2)");
    check("2", "Round(1.8)");
  }

  public void testSin() {
    check("0.0", "Sin(0)");
    check("1.0", "Sin(pi/2)");
  }

  public void testSinh() {
    check("0.0", "Sinh(0)");
  }

  public void testTan() {
    check("0.0", "Tan(0)");
    check("true", "Abs(Tan(pi/4) - 1.0) < 0.01");
  }

  public void testTanh() {
    check("0.0", "Tanh(0)");
  }

  public void testDegreesMode() {
    Calculator c = new Calculator();
    c.setDegreesMode(true);
    check(c, "true", "Abs(Acos(0.5) - 60) < 0.01");
    check(c, "true", "Abs(Asin(0.5) - 30) < 0.01");
    check(c, "0.0", "Atan(0)");
    check(c, "true", "Abs(Cos(60) - 0.5) < 0.01");
    check(c, "true", "Abs(Sin(90) - 1.0) < 0.01");
    check(c, "true", "Abs(Tan(45) - 1.0) < 0.01");
  }

  public void testDigitCount() {
    check("[1, 0, 0, 0, 0, 0, 0, 0, 0, 0]", "DigitCount(0)");
    check("[0, 1, 1, 0, 0, 0, 0, 0, 0, 0]", "DigitCount(-12)");
    check("[0, 1, 1, 0, 0, 0, 0, 0, 0, 0]", "DigitCount(12)");
    check("[0, 1, 1, 1, 1, 1, 1, 1, 1, 1]", "DigitCount(123456789)");
    check("[1, 1, 1, 1, 1, 1, 1, 1, 1, 1]", "DigitCount(1234567890)");
    check("[1, 2, 2, 2, 2, 2, 2, 2, 2, 2]", "DigitCount(9876543210123456789)");
    check("[30, 15, 19, 10, 10, 14, 19, 7, 14, 20]", "DigitCount(100!)");
    // Binary.
    check("[1, 0]", "DigitCount(0, 2)");
    check("[0, 1]", "DigitCount(1, 2)");
    check("[1, 1]", "DigitCount(2, 2)");
    check("[0, 2]", "DigitCount(3, 2)");
    // Octal.
    check("[0, 0, 3, 1, 0, 0, 0, 0]", "DigitCount(1234, 8)");
    // Hex.
    check("[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 2, 3, 1]", "DigitCount(0xdeadbeef, 16)");
  }

  public void testDivideByZero() {
    // Machine integers.
    try {
      new Calculator().evaluate("123/0");
      fail();
    } catch (CalculatorError ex) {
      assertEquals("division by zero", ex.getMessage());
    }
    // Big integers.
    try {
      new Calculator().evaluate("50!/0");
      fail();
    } catch (CalculatorError ex) {
      assertEquals("division by zero", ex.getMessage());
    }
    // Machine integer modulo.
    try {
      new Calculator().evaluate("123%0");
      fail();
    } catch (CalculatorError ex) {
      assertEquals("division by zero", ex.getMessage());
    }
    // Big integer modulo.
    try {
      new Calculator().evaluate("50!%0");
      fail();
    } catch (CalculatorError ex) {
      assertEquals("division by zero", ex.getMessage());
    }
    // Reals.
    try {
      new Calculator().evaluate("123.0/0.0");
      fail();
    } catch (CalculatorError ex) {
      assertEquals("division by zero", ex.getMessage());
    }
  }

  public void testDivisors() {
    check("[1, 3, 41, 123]", "Divisors(-123)");
    check("[1, 2, 617, 1234]", "Divisors(1234)");
    check("[1, 17]", "Divisors(17)");
    check("[1]", "Divisors(1)");
    check("[]", "Divisors(0)");
  }

  public void testFactorial() {
    check("1", "Factorial(0)");
    check("1", "Factorial(1)");
    check("2", "Factorial(2)");
    check("6", "Factorial(3)");
    check("24", "Factorial(4)");
    check("120", "Factorial(5)");

    check("7257415615307998967396728211129263114716991681296451376543577798900561843401706157852350749242617459511490991237838520776666022565442753025328900773207510902400430280058295603966612599658257104398558294257568966313439612262571094946806711205568880457193340212661452800000000000000000000000000000000000000000", "Factorial(170)");

    check("true", "Factorial(5) == 5!");

    check("6", "3!");
    check("720", "3!!");
  }

  public void testFactors() {
    check("[-3, 41]", "Factors(-123)");
    check("[2, 617]", "Factors(1234)");
    check("[17]", "Factors(17)");
    check("[]", "Factors(1)");
    check("[]", "Factors(0)");
  }

  public void testFilter() {
    check("[]", "Filter(IsPrime(x), x, [])");
    check("[]", "Filter(IsPrime(x), x, [1])");
    check("[2]", "Filter(IsPrime(x), x, [2])");
    check("[2, 3, 5, 7]", "Filter(IsPrime(x), x, Range(0, 10))");
  }

  public void testGCD() {
    check("0", "GCD(0, 0)");
    check("12", "GCD(12, 0)");
    check("6", "GCD(12, 18)");
    check("3", "GCD(6, 21)");
    check("3", "GCD(21, 6)");
    check("2", "GCD(-4, 14)");
    check("2", "GCD(4, -14)");
    check("2", "GCD(-4, -14)");
    check("1", "GCD(9, 28)");
    check("3", "GCD(6E100000, 21E100000)/1E100000");
  }

  public void testIntegerLength() {
    check("4", "IntegerLength(1234)");
    check("3", "IntegerLength(100)");
    check("3", "IntegerLength(-100)");
    check("307", "IntegerLength(170!)");
    check("525", "IntegerLength(100!, 2)");
    check("2", "IntegerLength(9, 8)");
    check("2", "IntegerLength(255, 16)");
    check("3", "IntegerLength(256, 16)");
  }

  public void testIntegerPart() {
    check("1", "IntegerPart(1.2)");
    check("1", "IntegerPart(1)");
    check("-2", "IntegerPart(-2.4)");
    check("-2", "IntegerPart(-2)");
  }

  public void testFractionalPart() {
    check("0.2", "FractionalPart(1.2)");
    check("0", "FractionalPart(1)");
    check("0.4", "FractionalPart(-2.4)");
    check("0", "FractionalPart(-2)");

    check("1.2", "IntegerPart(1.2) + FractionalPart(1.2)");
  }

  public void testMax() {
    check("123", "Max(-123, 123)");
    check("123", "Max(123, 123)");
    check("124", "Max(123, 124)");
    check("0.2", "Max(0.1, 0.2)");
    check("123.1", "Max(123, 123.1)");
  }

  public void testMin() {
    check("-123", "Min(-123, 123)");
    check("123", "Min(123, 123)");
    check("123", "Min(123, 124)");
    check("0.1", "Min(0.1, 0.2)");
    check("123", "Min(123, 123.1)");
  }

  public void testSign() {
    check("-1", "Sign(-123)");
    check("-1", "Sign(-123.0)");
    check("0", "Sign(0)");
    check("0", "Sign(0.0)");
    check("1", "Sign(123)");
    check("1", "Sign(123.0)");
  }

  public void testSqrt() {
    check("9.0", "Sqrt(81)");
    check("2.0", "\u221a4");
    // Check √3*2 == 2*√3.
    check(3.464101, "√3*2", 0.000001);
  }

  public void testSum() {
    check("55", "Sum(x, x, 0, 10)");
    check("55", "Sum(x, x, 0, 10.2)");
    check("385", "Sum(i^2, i, 0, 10)");
    check(0.0, "Sum(1/i!, i, 0, 30)-e", 0.000001);
    // FIXME: failure test for min > max.
  }

  public void testPermutations() {
    check("1", "nCr(5, 5)");
    check("120", "nPr(5, 5)");
    check("210", "nCr(10, 4)");
    check("5040", "nPr(10, 4)");
    check("22100", "nCr(52, 3)");
    check("132600", "nPr(52, 3)");
  }

  public void testProduct() {
    check("3628800", "Product(x, x, 1, 10)");
    check("3628800", "Product(x, x, 1, 10.2)");
    check("518400", "Product(i^2, i, 1, 6)");
    // FIXME: failure test for min > max.
  }

  public void testAns() {
    final Calculator c = new Calculator();
    check(c, "0", "0");
    check(c, "1", "1+Ans");
    check(c, "2", "1+Ans");
    check(c, "4", "Ans*2");
    check(c, "8", "ans*2"); // Tests case-insensitivity.
    try {
      new Calculator().evaluate("ans = 3");
      fail("no exception was thrown when assigning to Ans");
    } catch (CalculatorError ex) {
      assertEquals("can't assign a new value to Ans", ex.getMessage());
    }
  }

  public void testVariables() {
    final Calculator c = new Calculator();
    check(c, "2", "a = 2");
    check(c, "2", "a");
    check(c, "4", "2*a");
  }

  public void testLCM() {
    check("0", "LCM(0, 0)");
    check("0", "LCM(12, 0)");
    check("0", "LCM(0, 12)");
    check("12", "LCM(4, 6)");
    check("42", "LCM(6, 21)");
    check("42", "LCM(21, 6)");
    check("21", "LCM(-3, 7)");
    check("21", "LCM(3, -7)");
    check("21", "LCM(-3, -7)");
  }

  public void testLists() {
    // Explicitly constructed.
    check("[]", "List()");
    check("[7]", "List(7)");
    check("[34, 12]", "List(34, 12)");
    check("[34, [24, 12]]", "List(34, List(24, 12))");

    // Implicitly constructed.
    check("[]", "[]");
    check("[7]", "[5 + 2]");
    check("[34, 12]", "[30 + 4, 6 * 2]");
    check("[34, [24, 12]]", "[34, [24, 12]]");
  }

  public void testLength() {
    check("0", "Length([])");
    check("1", "Length([7])");
    check("2", "Length([30 + 4, 6 * 2])");
    check("2", "Length([34, [24, 12]])");
  }

  public void testRange() {
    check("[]", "Range(0)");
    check("[1]", "Range(1)");
    check("[1, 2, 3, 4]", "Range(4)");
    check("[4]", "Range(4, 4)");
    check("[4, 5, 6]", "Range(4, 6)");
    check("[]", "Range(6, 4)");
    check("[-6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4]", "Range(-6, 4)");
    check("[1, 3, 5]", "Range(1, 6, 2)");
    check("[4, 3, 2, 1]", "Range(4, 1, -1)");
    check("[1.2, 1.5, 1.8, 2.1]", "Range(1.2, 2.1, 0.3)");
  }

  public void testReverse() {
    check("[]", "Reverse([])");
    check("[7]", "Reverse([7])");
    check("[4, 3]", "Reverse([3, 4])");
    check("[-1, 0, 1]", "Reverse(Reverse([-1, 0, 1]))");
  }

  public void testTotal() {
    check("0", "Total([])");
    check("123", "Total([123])");
    check("6", "Total([1, 2, 3])");
  }

  public void testMap() {
    check("[]", "Map(x, x, [])");
    check("[1]", "Map(x, x, [1])");
    check("[2]", "Map(2, x, [1])");
    check("[2, 2, 2]", "Map(2, x, [1, 2, 3])");
    check("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", "Map(x, x, Range(0, 10))");
    check("[1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800]", "Map(x!, x, Range(0, 10))");
  }

  public void testIdentityMatrix() {
    check("[]", "IdentityMatrix(-1)");
    check("[]", "IdentityMatrix(0)");
    check("[[1]]", "IdentityMatrix(1)");
    check("[[1, 0], [0, 1]]", "IdentityMatrix(2)");
    check("[[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]", "IdentityMatrix(4)");
  }

  public void testIsMatrix() {
    check("false", "IsMatrix(1)");
    check("true", "IsMatrix([])");
    check("false", "IsMatrix([1])");
    check("true", "IsMatrix([[1]])");
    check("true", "IsMatrix([[1], [2]])");
    check("false", "IsMatrix([[1], [2], [3, 4]])");
    check("true", "IsMatrix([[1, 1], [2, 1], [3, 4]])");
    check("false", "IsMatrix([[1, 1], [2, 1], [3, [4]]])");
  }

  public void testDimensions() {
    check("[0, 0]", "Dimensions([])");
    check("[2, 3]", "Dimensions([[1, 1, 1], [2, 2, 2]])");
    check("[3, 2]", "Dimensions([[1, 1], [2, 2], [3, 3]])");
    check("[4, 4]", "Dimensions(IdentityMatrix(4))");
  }

  public void testMatrixMultiplication() {
    // Scalar multiplication.
    check("[[2, 16, -6], [8, -4, 10]]", "2*[[1, 8, -3], [4, -2, 5]]");
    check("[[2, 16, -6], [8, -4, 10]]", "[[1, 8, -3], [4, -2, 5]]*2");

    // Matrix multiplication.
    check("[[1, 0, 0], [0, 1, 0], [0, 0, 1]]", "IdentityMatrix(3)*IdentityMatrix(3)");
    check("[[0, 1], [0, 3]]", "[[1,2],[3,4]]*[[0,1],[0,0]]");
    check("[[3, 4], [0, 0]]", "[[0,1],[0,0]]*[[1,2],[3,4]]");
    check("[[5, 1], [4, 2]]", "[[1,0,2],[-1,3,1]]*[[3,1],[2,1],[1,0]]");
  }

  public void testMatrixAddition() {
    // Scalar addition.
    check("[[3, 10, -1], [6, 0, 7]]", "2+[[1, 8, -3], [4, -2, 5]]");
    check("[[3, 10, -1], [6, 0, 7]]", "[[1, 8, -3], [4, -2, 5]]+2");

    // Matrix addition.
    check("[[1, 3, 6], [8, 5, 0]]", "[[1,3,1],[1,0,0]] + [[0,0,5],[7,5,0]]");
  }

  public void testMatrixSubtraction() {
    // Scalar subtraction.
    check("[[1, -6, 5], [-2, 4, -3]]", "2-[[1, 8, -3], [4, -2, 5]]");
    check("[[-1, 6, -5], [2, -4, 3]]", "[[1, 8, -3], [4, -2, 5]]-2");

    // Matrix subtraction.
    check("[[1, 3, -4], [-6, -5, 0]]", "[[1,3,1],[1,0,0]] - [[0,0,5],[7,5,0]]");
  }

  public void testMatrixNegation() {
    check("[[-1, -8, 3], [-4, 2, -5]]", "-[[1, 8, -3], [4, -2, 5]]");
  }

  public void testTranspose() {
    check("[]", "Transpose([])");
    check("[[1], [2]]", "Transpose([[1, 2]])");
    check("[[1, 2]]", "Transpose([[1], [2]])");
    check("[[1, 3], [2, 4]]", "Transpose([[1, 2], [3, 4]])");
    check("[[1, 0], [2, -6], [3, 0]]", "Transpose([[1, 2, 3], [0, -6, 0]])");
  }

  private Node parse(String stringExpression) throws CalculatorError {
    // Duplicates Calculator.parse for testing.
    final Calculator calculator = new Calculator();
    final CalculatorParser parser = new CalculatorParser(calculator, stringExpression);
    return parser.parse();
  }

  private Node simplify(Node expression) {
    // Duplicates Calculator.simplify for testing.
    final Calculator calculator = new Calculator();
    return expression.simplify(calculator);
  }

  public void testSimplifier() {
    final Calculator calculator = new Calculator();
    final Node x = new CalculatorVariableNode("x");

    // +0
    assertEquals(IntegerNode.ZERO, simplify(parse("0+0")));
    assertEquals(x, simplify(parse("x+0")));
    assertEquals(x, simplify(parse("0+x")));

    // *1
    assertEquals(IntegerNode.ONE, simplify(parse("1*1")));
    assertEquals(x, simplify(parse("x*1")));
    assertEquals(x, simplify(parse("1*x")));
    assertEquals(x, simplify(parse("(0+1)*x")));

    // *0
    assertEquals(IntegerNode.ZERO, simplify(parse("0*0")));
    assertEquals(IntegerNode.ZERO, simplify(parse("0*1")));
    assertEquals(IntegerNode.ZERO, simplify(parse("x*0")));
    assertEquals(IntegerNode.ZERO, simplify(parse("0*x")));

    // --
    assertEquals(IntegerNode.ONE, simplify(parse("1")));
    assertEquals(IntegerNode.MINUS_ONE, simplify(parse("-1")));
    assertEquals(IntegerNode.ONE, simplify(parse("--1")));
    assertEquals(IntegerNode.MINUS_ONE, simplify(parse("---1")));

    // From "Paradigms of Artificial Intelligence Programming", section 8.2.
    assertEquals(IntegerNode.valueOf(4), simplify(parse("2+2")));
    assertEquals(IntegerNode.valueOf(137), simplify(parse("5*20+30+7")));
    // 5*x-(4+1)*x == 0
    // (y/z*(5*x-(4+1)*x)) == 0
    // ((4-3)*x+(y/y-1)*z) == x
    // 1*f(x)+0 == f(x)
    assertEquals(new CalculatorFunctions.Times().bind(IntegerNode.valueOf(6), x), simplify(parse("3*2*x")));
    //assertEquals(new CalculatorFunctions.Times().bind(IntegerNode.valueOf(6), x), simplify(parse("3*x*2")));
    //assertEquals(new CalculatorFunctions.Times().bind(IntegerNode.valueOf(6), x), simplify(parse("x*3*2")));

    // From "Paradigms of Artificial Intelligence Programming", section 8.3.
    assertEquals(new CalculatorFunctions.Times().bind(IntegerNode.valueOf(6), x), simplify(parse("3*2*x")));
    // 2*x*x*3 == 6*x^2
    // 2*x*3*y*4*z*5*6 == 720*x*y*z
    // 3+x+4+x == 2*x+7
    // 2*x*3*x*4*(1/x)*5*6 == 720*x
    // 3+x+4-x == 7
    // x+y+y+x == 2*x+2*y
    // 3*x+4*x == 7*x

    // 0-0 == 0
    // x-0 == x
    // 0-x == -x
    // x-x == 0
    // x+(-x) == 0
    // (-x)+x == 0

    // 2^sin(x/2) - 2^sin(x/2) == 0
    // 2^sin(x/2) / 2^sin(x/2) == 1

    // x+x == 2*x
    // 3*x+4*x == 7*x

    // x*x == x^2
    // x*x*x == x^3
    // 2*x*3*x == 6*x^2

    // 0/0 == indeterminate
    // 0/x (where x!=0) == 0
    // x/1 == x
    // 0*infinity == indeterminate
    // infinity/infinity == indeterminate
    // infinity-infinity == indeterminate
    // 0^0 == indeterminate
    // infinity^0 == indeterminate
    // x^0 (where x!=0 && x!=infinity) == 1
    // 0^x == 0
    // 1^infinity == indeterminate
    // 1^x (where x!=infinity) == 1
    // x^1 == x
    // 1/infinity == 0
    // x^-1 == 1/x (is this useful?)

    // x+y-x == y
    // x*(y/x) == y
    // (x*y)/x == y
  }
}
