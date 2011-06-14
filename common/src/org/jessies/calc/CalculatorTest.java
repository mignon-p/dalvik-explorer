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

import junit.framework.TestCase;

public class CalculatorTest extends TestCase {
    public void testArithmetic() {
        assertEquals("0", new Calculator().evaluate("0"));
        assertEquals("1", new Calculator().evaluate("1"));
        assertEquals("-1", new Calculator().evaluate("-1"));
        assertEquals("1", new Calculator().evaluate("--1"));
        assertEquals("1.00", new Calculator().evaluate("1.00"));
        
        assertEquals("0.2", new Calculator().evaluate(".2"));
        
        assertEquals("1200", new Calculator().evaluate("1.2E3"));
        assertEquals("1000", new Calculator().evaluate("1E3"));
        assertEquals("0.001", new Calculator().evaluate("1E-3"));
        assertEquals("1000", new Calculator().evaluate("1.E3"));
        assertEquals("100", new Calculator().evaluate(".1E3"));
        
        assertEquals(new Calculator().evaluate("1.2E3"), new Calculator().evaluate("1.2e3"));
        
        assertEquals("6", new Calculator().evaluate("1+2+3"));
        assertEquals("-1", new Calculator().evaluate("1+-2"));
        assertEquals("0", new Calculator().evaluate("3-2-1"));
        assertEquals("10000.001", new Calculator().evaluate("10000+0.001"));
        assertEquals("10000.001", new Calculator().evaluate("0.001+10000"));
        assertEquals("9999.999", new Calculator().evaluate("10000-0.001"));
        assertEquals("-9999.999", new Calculator().evaluate("0.001-10000"));
        
        // Check that we're doing decimal rather than binary arithmetic.
        assertEquals("0.1587", new Calculator().evaluate("1-0.8413"));
        assertEquals("131.2", new Calculator().evaluate("328*0.4"));
        
        assertEquals("12", new Calculator().evaluate("3*4"));
        assertEquals("-12", new Calculator().evaluate("-3*4"));
        assertEquals("-12", new Calculator().evaluate("3*-4"));
        assertEquals("12", new Calculator().evaluate("-3*-4"));
        
        assertEquals("7", new Calculator().evaluate("1+2*3"));
        assertEquals("9", new Calculator().evaluate("(1+2)*3"));
        
        assertEquals("0.5", new Calculator().evaluate("1/2"));
        
        assertEquals("3", new Calculator().evaluate("3%4"));
        assertEquals("0", new Calculator().evaluate("4%4"));
        assertEquals("1", new Calculator().evaluate("5%4"));
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
        assertEquals("true", new Calculator().evaluate("1<2"));
        assertEquals("false", new Calculator().evaluate("2<2"));
        assertEquals("false", new Calculator().evaluate("2<1"));
        assertEquals("true", new Calculator().evaluate("1<=2"));
        assertEquals("true", new Calculator().evaluate("2<=2"));
        assertEquals("false", new Calculator().evaluate("2<=1"));
        assertEquals("false", new Calculator().evaluate("1>2"));
        assertEquals("false", new Calculator().evaluate("2>2"));
        assertEquals("true", new Calculator().evaluate("2>1"));
        assertEquals("false", new Calculator().evaluate("1>=2"));
        assertEquals("true", new Calculator().evaluate("2>=2"));
        assertEquals("true", new Calculator().evaluate("2>=1"));
        assertEquals("false", new Calculator().evaluate("1==2"));
        assertEquals("true", new Calculator().evaluate("2==2"));
        assertEquals("false", new Calculator().evaluate("2==1"));
        assertEquals("true", new Calculator().evaluate("1!=2"));
        assertEquals("false", new Calculator().evaluate("2!=2"));
        assertEquals("true", new Calculator().evaluate("2!=1"));
        assertEquals("true", new Calculator().evaluate("true==true"));
        assertEquals("false", new Calculator().evaluate("true==false"));
        assertEquals("false", new Calculator().evaluate("false==true"));
        assertEquals("true", new Calculator().evaluate("false==false"));
        assertEquals("false", new Calculator().evaluate("true!=true"));
        assertEquals("true", new Calculator().evaluate("true!=false"));
        assertEquals("true", new Calculator().evaluate("false!=true"));
        assertEquals("false", new Calculator().evaluate("false!=false"));
        assertEquals("true", new Calculator().evaluate("[]==[]"));
        assertEquals("true", new Calculator().evaluate("[[]]==[[]]"));
        assertEquals("true", new Calculator().evaluate("[[], []]==[[], []]"));
        assertEquals("true", new Calculator().evaluate("[0]==[0]"));
        assertEquals("false", new Calculator().evaluate("[0]==[1]"));
        assertEquals("true", new Calculator().evaluate("[0, 1]==[0, 1]"));
        assertEquals("false", new Calculator().evaluate("[0, 1]==[1, 0]"));
        assertEquals("false", new Calculator().evaluate("[0, 1]==[0, [1]]"));
        assertEquals("true", new Calculator().evaluate("[0, [1]]==[0, [1]]"));
    }
    
    public void testLogicalNot() {
        assertEquals("true", new Calculator().evaluate("!false"));
        assertEquals("false", new Calculator().evaluate("!true"));
        assertEquals("true", new Calculator().evaluate("!(1==2)"));
        assertEquals("false", new Calculator().evaluate("!(2==2)"));
        assertEquals("true", new Calculator().evaluate("!!(2==2)"));
    }
    
    public void testLogicalAnd() {
        assertEquals("false", new Calculator().evaluate("false&&false"));
        assertEquals("false", new Calculator().evaluate("false&&true"));
        assertEquals("false", new Calculator().evaluate("true&&false"));
        assertEquals("true", new Calculator().evaluate("true&&true"));
    }
    
    public void testLogicalOr() {
        assertEquals("false", new Calculator().evaluate("false||false"));
        assertEquals("true", new Calculator().evaluate("false||true"));
        assertEquals("true", new Calculator().evaluate("true||false"));
        assertEquals("true", new Calculator().evaluate("true||true"));
    }
    
    public void testShifts() {
        assertEquals("16", new Calculator().evaluate("1<<4"));
        assertEquals("12", new Calculator().evaluate("(12<<3)>>3"));
    }
    
    public void testBitOperations() {
        assertEquals("true", new Calculator().evaluate("(0x1234 & 0xff0) == 0x230"));
        assertEquals("true", new Calculator().evaluate("(0x1200 | 0x34) == 0x1234"));
        assertEquals("6", new Calculator().evaluate("BitXor(5, 3)"));
        assertEquals("true", new Calculator().evaluate("((0x1234 & ~0xff) | 0x56) == 0x1256"));
        assertEquals("-4", new Calculator().evaluate("~3"));
        assertEquals("3", new Calculator().evaluate("~~3"));
        assertEquals("0", new Calculator().evaluate("BitLength(0)"));
        assertEquals("6", new Calculator().evaluate("BitLength(32)"));
        assertEquals("8", new Calculator().evaluate("BitLength(255)"));
        assertEquals("9", new Calculator().evaluate("BitLength(256)"));
        assertEquals("9", new Calculator().evaluate("BitLength(257)"));
        assertEquals("8", new Calculator().evaluate("BitLength(-255)"));
        assertEquals("8", new Calculator().evaluate("BitLength(-256)"));
        assertEquals("9", new Calculator().evaluate("BitLength(-257)"));
        assertEquals("1", new Calculator().evaluate("BitGet(5, 0)"));
        assertEquals("0", new Calculator().evaluate("BitGet(5, 1)"));
        assertEquals("1", new Calculator().evaluate("BitGet(5, 2)"));
        assertEquals("0", new Calculator().evaluate("BitGet(5, 3)"));
        assertEquals("0", new Calculator().evaluate("BitGet(5, 4)"));
        assertEquals("0", new Calculator().evaluate("BitClear(0, 0)"));
        assertEquals("0", new Calculator().evaluate("BitClear(1, 0)"));
        assertEquals("4", new Calculator().evaluate("BitClear(5, 0)"));
        assertEquals("5", new Calculator().evaluate("BitClear(5, 16)"));
        assertEquals("1", new Calculator().evaluate("BitSet(0, 0)"));
        assertEquals("5", new Calculator().evaluate("BitSet(1, 2)"));
    }
    
    public void testExponentiation() {
        assertEquals("8", new Calculator().evaluate("2^3"));
        assertEquals("2417851639229258349412352", new Calculator().evaluate("2^3^4"));
        assertEquals("2.0", new Calculator().evaluate("4^0.5"));
        assertEquals("100", new Calculator().evaluate("-10^2"));
        assertEquals("100", new Calculator().evaluate("(-10)^2"));
        assertEquals("0.01", new Calculator().evaluate("10^-2"));
        assertEquals("0.01", new Calculator().evaluate("10^(-2)"));
    }
    
    public void testConstants() {
        assertEquals(Math.E, Double.valueOf(new Calculator().evaluate("e")), 0.000001);
        assertEquals(Math.PI, Double.valueOf(new Calculator().evaluate("pi")), 0.000001);
        assertEquals("true", new Calculator().evaluate("pi == \u03c0"));
        try {
            new Calculator().evaluate("pi = 3");
            fail("no exception was thrown when assigning to a constant!");
        } catch (CalculatorError ex) {
            assertEquals("can't assign a new value to the constant pi", ex.getMessage());
        }
    }
    
    public void testBigIntegers() {
        // Arithmetic tests (from http://www.isthe.com/chongo/tech/comp/calc/calc-whatis.html).
        assertEquals("29075426613099201338473141505176993450849249622191102976", new Calculator().evaluate("3 * 19^43 - 1"));
        assertEquals("39614081257132168796771975167", new Calculator().evaluate("Mod(2^23209-1, 2^127-1)"));
        
        // Check switches from fix to big.
        assertEquals("true", new Calculator().evaluate("Abs(-(0x8000)) == 0x8000"));
        assertEquals("true", new Calculator().evaluate("Abs(-(0x8000000000000000)) == 0x8000000000000000"));
        assertEquals("true", new Calculator().evaluate("-(0x8000000000000000) == -0x8000000000000000"));
        assertEquals("true", new Calculator().evaluate("-1 * 0x8000000000000000 == -0x8000000000000000"));
        assertEquals("true", new Calculator().evaluate("0x7fffffffffffffff + 1 == 0x8000000000000000"));
        assertEquals("true", new Calculator().evaluate("(-(0x8000000000000000)) - 1 == -0x8000000000000001"));
        assertEquals("true", new Calculator().evaluate("-1 * 0x8000000000000000 - 1 == -0x8000000000000001"));
        assertEquals("true", new Calculator().evaluate("-(-(0x8000000000000000)) == 0x8000000000000000"));
        assertEquals("true", new Calculator().evaluate("-1 * -1 * 0x8000000000000000 == 0x8000000000000000"));
        assertEquals("true", new Calculator().evaluate("0x8000000000000000/-1 == -0x8000000000000000"));
    }
    
    public void testFunctions() {
        // FIXME: better tests?
        assertEquals("2", new Calculator().evaluate("Abs(2)"));
        assertEquals("2", new Calculator().evaluate("Abs(-2)"));
        assertEquals("0.0", new Calculator().evaluate("Acos(1)"));
        assertEquals("0.0", new Calculator().evaluate("Asin(0)"));
        assertEquals("true", new Calculator().evaluate("Acos(0) == Asin(1)"));
        assertEquals("0.0", new Calculator().evaluate("Atan(0)"));
        assertEquals("1", new Calculator().evaluate("Boole(true)"));
        assertEquals("0", new Calculator().evaluate("Boole(false)"));
        assertEquals("3.0", new Calculator().evaluate("Cbrt(27)"));
        assertEquals("2.0", new Calculator().evaluate("Ceil(1.2)"));
        assertEquals("1.0", new Calculator().evaluate("Cos(0)"));
        assertEquals("-1.0", new Calculator().evaluate("Cos(pi)"));
        assertEquals("1.0", new Calculator().evaluate("Cosh(0)"));
        assertEquals(1.0, Double.valueOf(new Calculator().evaluate("Exp(1)/e")), 0.000001);
        assertEquals("1.0", new Calculator().evaluate("Floor(1.2)"));
        assertEquals("5.0", new Calculator().evaluate("Hypot(3, 4)"));
        
        assertEquals("false", new Calculator().evaluate("IsPrime(0)"));
        assertEquals("false", new Calculator().evaluate("IsPrime(1)"));
        assertEquals("true", new Calculator().evaluate("IsPrime(2)"));
        assertEquals("true", new Calculator().evaluate("IsPrime(3)"));
        assertEquals("false", new Calculator().evaluate("IsPrime(4)"));
        assertEquals("true", new Calculator().evaluate("IsPrime(5)"));
        assertEquals("false", new Calculator().evaluate("IsPrime(-4)"));
        assertEquals("true", new Calculator().evaluate("IsPrime(-5)"));
        
        assertEquals("10.0", new Calculator().evaluate("Log(2, 1024)"));
        assertEquals("10.0", new Calculator().evaluate("Log2(1024)"));
        assertEquals("4.0", new Calculator().evaluate("LogE(exp(4))"));
        assertEquals("3.0", new Calculator().evaluate("Log10(1000)"));
        assertEquals("1", new Calculator().evaluate("Round(1.2)"));
        assertEquals("2", new Calculator().evaluate("Round(1.8)"));
        assertEquals("0.0", new Calculator().evaluate("Sin(0)"));
        assertEquals("1.0", new Calculator().evaluate("Sin(pi/2)"));
        assertEquals("0.0", new Calculator().evaluate("Sinh(0)"));
        assertEquals("9.0", new Calculator().evaluate("Sqrt(81)"));
        assertEquals("0.0", new Calculator().evaluate("Tan(0)"));
        assertEquals("true", new Calculator().evaluate("Abs(Tan(pi/4) - 1.0) < 0.01"));
        assertEquals("0.0", new Calculator().evaluate("Tanh(0)"));
    }
    
    public void testDegreesMode() {
        Calculator c = new Calculator();
        c.setDegreesMode(true);
        assertEquals("true", c.evaluate("Abs(Acos(0.5) - 60) < 0.01"));
        assertEquals("true", c.evaluate("Abs(Asin(0.5) - 30) < 0.01"));
        assertEquals("0.0", c.evaluate("Atan(0)"));
        assertEquals("true", c.evaluate("Abs(Cos(60) - 0.5) < 0.01"));
        assertEquals("true", c.evaluate("Abs(Sin(90) - 1.0) < 0.01"));
        assertEquals("true", c.evaluate("Abs(Tan(45) - 1.0) < 0.01"));
    }
    
    public void testDigitCount() {
        assertEquals("[1, 0, 0, 0, 0, 0, 0, 0, 0, 0]", new Calculator().evaluate("DigitCount(0)"));
        assertEquals("[0, 1, 1, 0, 0, 0, 0, 0, 0, 0]", new Calculator().evaluate("DigitCount(-12)"));
        assertEquals("[0, 1, 1, 0, 0, 0, 0, 0, 0, 0]", new Calculator().evaluate("DigitCount(12)"));
        assertEquals("[0, 1, 1, 1, 1, 1, 1, 1, 1, 1]", new Calculator().evaluate("DigitCount(123456789)"));
        assertEquals("[1, 1, 1, 1, 1, 1, 1, 1, 1, 1]", new Calculator().evaluate("DigitCount(1234567890)"));
        assertEquals("[1, 2, 2, 2, 2, 2, 2, 2, 2, 2]", new Calculator().evaluate("DigitCount(9876543210123456789)"));
        assertEquals("[30, 15, 19, 10, 10, 14, 19, 7, 14, 20]", new Calculator().evaluate("DigitCount(100!)"));
        assertEquals("[1, 0]", new Calculator().evaluate("DigitCount(0, 2)"));
        assertEquals("[0, 1]", new Calculator().evaluate("DigitCount(1, 2)"));
        assertEquals("[1, 1]", new Calculator().evaluate("DigitCount(2, 2)"));
        assertEquals("[0, 2]", new Calculator().evaluate("DigitCount(3, 2)"));
    }
    
    public void testFactorial() {
        assertEquals("1", new Calculator().evaluate("Factorial(0)"));
        assertEquals("1", new Calculator().evaluate("Factorial(1)"));
        assertEquals("2", new Calculator().evaluate("Factorial(2)"));
        assertEquals("6", new Calculator().evaluate("Factorial(3)"));
        assertEquals("24", new Calculator().evaluate("Factorial(4)"));
        assertEquals("120", new Calculator().evaluate("Factorial(5)"));
        
        assertEquals("7257415615307998967396728211129263114716991681296451376543577798900561843401706157852350749242617459511490991237838520776666022565442753025328900773207510902400430280058295603966612599658257104398558294257568966313439612262571094946806711205568880457193340212661452800000000000000000000000000000000000000000", new Calculator().evaluate("Factorial(170)"));
        
        assertEquals("true", new Calculator().evaluate("Factorial(5) == 5!"));
        
        assertEquals("6", new Calculator().evaluate("3!"));
        assertEquals("720", new Calculator().evaluate("3!!"));
    }
    
    public void testFilter() {
        assertEquals("[]", new Calculator().evaluate("Filter(IsPrime(x), x, [])"));
        assertEquals("[]", new Calculator().evaluate("Filter(IsPrime(x), x, [1])"));
        assertEquals("[2]", new Calculator().evaluate("Filter(IsPrime(x), x, [2])"));
        assertEquals("[2, 3, 5, 7]", new Calculator().evaluate("Filter(IsPrime(x), x, Range(0, 10))"));
    }
    
    public void testGCD() {
        assertEquals("0", new Calculator().evaluate("GCD(0, 0)"));
        assertEquals("12", new Calculator().evaluate("GCD(12, 0)"));
        assertEquals("6", new Calculator().evaluate("GCD(12, 18)"));
        assertEquals("3", new Calculator().evaluate("GCD(6, 21)"));
        assertEquals("3", new Calculator().evaluate("GCD(21, 6)"));
        assertEquals("2", new Calculator().evaluate("GCD(-4, 14)"));
        assertEquals("2", new Calculator().evaluate("GCD(4, -14)"));
        assertEquals("2", new Calculator().evaluate("GCD(-4, -14)"));
        assertEquals("1", new Calculator().evaluate("GCD(9, 28)"));
        assertEquals("3", new Calculator().evaluate("GCD(6E100000, 21E100000)/1E100000"));
    }
    
    public void testIntegerLength() {
        assertEquals("4", new Calculator().evaluate("IntegerLength(1234)"));
        assertEquals("3", new Calculator().evaluate("IntegerLength(100)"));
        assertEquals("3", new Calculator().evaluate("IntegerLength(-100)"));
        assertEquals("307", new Calculator().evaluate("IntegerLength(170!)"));
        assertEquals("525", new Calculator().evaluate("IntegerLength(100!, 2)"));
        assertEquals("2", new Calculator().evaluate("IntegerLength(255, 16)"));
        assertEquals("3", new Calculator().evaluate("IntegerLength(256, 16)"));
    }
    
    public void testIntegerPartAndFractionalPart() {
        assertEquals("1", new Calculator().evaluate("IntegerPart(1.2)"));
        assertEquals("1", new Calculator().evaluate("IntegerPart(1)"));
        assertEquals("-2", new Calculator().evaluate("IntegerPart(-2.4)"));
        assertEquals("-2", new Calculator().evaluate("IntegerPart(-2)"));
        
        assertEquals("0.2", new Calculator().evaluate("FractionalPart(1.2)"));
        assertEquals("0", new Calculator().evaluate("FractionalPart(1)"));
        assertEquals("0.4", new Calculator().evaluate("FractionalPart(-2.4)"));
        assertEquals("0", new Calculator().evaluate("FractionalPart(-2)"));
        
        assertEquals("1.2", new Calculator().evaluate("IntegerPart(1.2) + FractionalPart(1.2)"));
    }
    
    public void testMaxAndMin() {
        assertEquals("123", new Calculator().evaluate("Max(-123, 123)"));
        assertEquals("123", new Calculator().evaluate("Max(123, 123)"));
        assertEquals("124", new Calculator().evaluate("Max(123, 124)"));
        assertEquals("0.2", new Calculator().evaluate("Max(0.1, 0.2)"));
        assertEquals("123.1", new Calculator().evaluate("Max(123, 123.1)"));
        
        assertEquals("-123", new Calculator().evaluate("Min(-123, 123)"));
        assertEquals("123", new Calculator().evaluate("Min(123, 123)"));
        assertEquals("123", new Calculator().evaluate("Min(123, 124)"));
        assertEquals("0.1", new Calculator().evaluate("Min(0.1, 0.2)"));
        assertEquals("123", new Calculator().evaluate("Min(123, 123.1)"));
    }
    
    public void testSign() {
        assertEquals("-1", new Calculator().evaluate("Sign(-123)"));
        assertEquals("-1", new Calculator().evaluate("Sign(-123.0)"));
        assertEquals("0", new Calculator().evaluate("Sign(0)"));
        assertEquals("0", new Calculator().evaluate("Sign(0.0)"));
        assertEquals("1", new Calculator().evaluate("Sign(123)"));
        assertEquals("1", new Calculator().evaluate("Sign(123.0)"));
    }
    
    public void testSqrt() {
        assertEquals("2.0", new Calculator().evaluate("\u221a4"));
        // Check √3*2 == 2*√3.
        assertTrue(new Calculator().evaluate("\u221a3*2").startsWith("3.464"));
    }
    
    public void testSum() {
        assertEquals("55", new Calculator().evaluate("Sum(i, 0, 10)"));
        assertEquals("55", new Calculator().evaluate("Sum(i, 0, 10.2)"));
        assertEquals("385", new Calculator().evaluate("Sum(i^2, 0, 10)"));
        assertEquals(0.0, Double.valueOf(new Calculator().evaluate("Sum(1/i!, 0, 30)-e")), 0.000001);
        // FIXME: failure test for min > max.
    }
    
    public void testPermutations() {
        assertEquals("1", new Calculator().evaluate("nCr(5, 5)"));
        assertEquals("120", new Calculator().evaluate("nPr(5, 5)"));
        assertEquals("210", new Calculator().evaluate("nCr(10, 4)"));
        assertEquals("5040", new Calculator().evaluate("nPr(10, 4)"));
        assertEquals("22100", new Calculator().evaluate("nCr(52, 3)"));
        assertEquals("132600", new Calculator().evaluate("nPr(52, 3)"));
    }
    
    public void testProduct() {
        assertEquals("3628800", new Calculator().evaluate("Product(i, 1, 10)"));
        assertEquals("3628800", new Calculator().evaluate("Product(i, 1, 10.2)"));
        assertEquals("518400", new Calculator().evaluate("Product(i^2, 1, 6)"));
        // FIXME: failure test for min > max.
    }
    
    public void testAns() {
        final Calculator calculator = new Calculator();
        assertEquals("0", calculator.evaluate("0"));
        assertEquals("1", calculator.evaluate("1+Ans"));
        assertEquals("2", calculator.evaluate("1+Ans"));
        assertEquals("4", calculator.evaluate("Ans*2"));
        assertEquals("8", calculator.evaluate("ans*2")); // Tests case-insensitivity.
        try {
            new Calculator().evaluate("ans = 3");
            fail("no exception was thrown when assigning to Ans");
        } catch (CalculatorError ex) {
            assertEquals("can't assign a new value to Ans", ex.getMessage());
        }
    }
    
    public void testVariables() {
        final Calculator calculator = new Calculator();
        assertEquals("2", calculator.evaluate("a = 2"));
        assertEquals("2", calculator.evaluate("a"));
        assertEquals("4", calculator.evaluate("2*a"));
    }
    
    public void testLCM() {
        assertEquals("0", new Calculator().evaluate("LCM(0, 0)"));
        assertEquals("0", new Calculator().evaluate("LCM(12, 0)"));
        assertEquals("0", new Calculator().evaluate("LCM(0, 12)"));
        assertEquals("12", new Calculator().evaluate("LCM(4, 6)"));
        assertEquals("42", new Calculator().evaluate("LCM(6, 21)"));
        assertEquals("42", new Calculator().evaluate("LCM(21, 6)"));
        assertEquals("21", new Calculator().evaluate("LCM(-3, 7)"));
        assertEquals("21", new Calculator().evaluate("LCM(3, -7)"));
        assertEquals("21", new Calculator().evaluate("LCM(-3, -7)"));
    }
    
    public void testLists() {
        final Calculator calculator = new Calculator();
        
        // Explicitly constructed.
        assertEquals("[]", calculator.evaluate("List()"));
        assertEquals("[7]", calculator.evaluate("List(7)"));
        assertEquals("[34, 12]", calculator.evaluate("List(34, 12)"));
        assertEquals("[34, [24, 12]]", calculator.evaluate("List(34, List(24, 12))"));
        
        // Implicitly constructed.
        assertEquals("[]", calculator.evaluate("[]"));
        assertEquals("[7]", calculator.evaluate("[5 + 2]"));
        assertEquals("[34, 12]", calculator.evaluate("[30 + 4, 6 * 2]"));
        assertEquals("[34, [24, 12]]", calculator.evaluate("[34, [24, 12]]"));
        
        // Length.
        assertEquals("0", calculator.evaluate("Length([])"));
        assertEquals("1", calculator.evaluate("Length([7])"));
        assertEquals("2", calculator.evaluate("Length([30 + 4, 6 * 2])"));
        assertEquals("2", calculator.evaluate("Length([34, [24, 12]])"));
        
        // Range.
        assertEquals("[]", calculator.evaluate("Range(0)"));
        assertEquals("[1]", calculator.evaluate("Range(1)"));
        assertEquals("[1, 2, 3, 4]", calculator.evaluate("Range(4)"));
        assertEquals("[4]", calculator.evaluate("Range(4, 4)"));
        assertEquals("[4, 5, 6]", calculator.evaluate("Range(4, 6)"));
        assertEquals("[]", calculator.evaluate("Range(6, 4)"));
        assertEquals("[-6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4]", calculator.evaluate("Range(-6, 4)"));
        assertEquals("[1, 3, 5]", calculator.evaluate("Range(1, 6, 2)"));
        assertEquals("[4, 3, 2, 1]", calculator.evaluate("Range(4, 1, -1)"));
        assertEquals("[1.2, 1.5, 1.8, 2.1]", calculator.evaluate("Range(1.2, 2.1, 0.3)"));
        
        // Reverse.
        assertEquals("[]", calculator.evaluate("Reverse([])"));
        assertEquals("[7]", calculator.evaluate("Reverse([7])"));
        assertEquals("[4, 3]", calculator.evaluate("Reverse([3, 4])"));
        assertEquals("[-1, 0, 1]", calculator.evaluate("Reverse(Reverse([-1, 0, 1]))"));
        
        // Total.
        assertEquals("0", calculator.evaluate("Total([])"));
        assertEquals("123", calculator.evaluate("Total([123])"));
        assertEquals("6", calculator.evaluate("Total([1, 2, 3])"));
    }
    
    public void testMap() {
        assertEquals("[]", new Calculator().evaluate("Map(x, x, [])"));
        assertEquals("[1]", new Calculator().evaluate("Map(x, x, [1])"));
        assertEquals("[2]", new Calculator().evaluate("Map(2, x, [1])"));
        assertEquals("[2, 2, 2]", new Calculator().evaluate("Map(2, x, [1, 2, 3])"));
        assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", new Calculator().evaluate("Map(x, x, Range(0, 10))"));
        assertEquals("[1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800]", new Calculator().evaluate("Map(x!, x, Range(0, 10))"));
    }
    
    public void testMatrices() {
        final Calculator calculator = new Calculator();
        
        // IdentityMatrix.
        assertEquals("[]", calculator.evaluate("IdentityMatrix(-1)"));
        assertEquals("[]", calculator.evaluate("IdentityMatrix(0)"));
        assertEquals("[[1]]", calculator.evaluate("IdentityMatrix(1)"));
        assertEquals("[[1, 0], [0, 1]]", calculator.evaluate("IdentityMatrix(2)"));
        assertEquals("[[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]", calculator.evaluate("IdentityMatrix(4)"));
        
        // IsMatrix.
        assertEquals("false", calculator.evaluate("IsMatrix(1)"));
        assertEquals("true", calculator.evaluate("IsMatrix([])"));
        assertEquals("false", calculator.evaluate("IsMatrix([1])"));
        assertEquals("true", calculator.evaluate("IsMatrix([[1]])"));
        assertEquals("true", calculator.evaluate("IsMatrix([[1], [2]])"));
        assertEquals("false", calculator.evaluate("IsMatrix([[1], [2], [3, 4]])"));
        assertEquals("true", calculator.evaluate("IsMatrix([[1, 1], [2, 1], [3, 4]])"));
        assertEquals("false", calculator.evaluate("IsMatrix([[1, 1], [2, 1], [3, [4]]])"));
        
        // Dimensions.
        assertEquals("[0, 0]", calculator.evaluate("Dimensions([])"));
        assertEquals("[2, 3]", calculator.evaluate("Dimensions([[1, 1, 1], [2, 2, 2]])"));
        assertEquals("[3, 2]", calculator.evaluate("Dimensions([[1, 1], [2, 2], [3, 3]])"));
        assertEquals("[4, 4]", calculator.evaluate("Dimensions(IdentityMatrix(4))"));
        
        // Scalar multiplication.
        assertEquals("[[2, 16, -6], [8, -4, 10]]", calculator.evaluate("2*[[1, 8, -3], [4, -2, 5]]"));
        assertEquals("[[2, 16, -6], [8, -4, 10]]", calculator.evaluate("[[1, 8, -3], [4, -2, 5]]*2"));
        
        // Matrix multiplication.
        assertEquals("[[1, 0, 0], [0, 1, 0], [0, 0, 1]]", calculator.evaluate("IdentityMatrix(3)*IdentityMatrix(3)"));
        assertEquals("[[0, 1], [0, 3]]", calculator.evaluate("[[1,2],[3,4]]*[[0,1],[0,0]]"));
        assertEquals("[[3, 4], [0, 0]]", calculator.evaluate("[[0,1],[0,0]]*[[1,2],[3,4]]"));
        assertEquals("[[5, 1], [4, 2]]", calculator.evaluate("[[1,0,2],[-1,3,1]]*[[3,1],[2,1],[1,0]]"));
        
        // Scalar addition.
        assertEquals("[[3, 10, -1], [6, 0, 7]]", calculator.evaluate("2+[[1, 8, -3], [4, -2, 5]]"));
        assertEquals("[[3, 10, -1], [6, 0, 7]]", calculator.evaluate("[[1, 8, -3], [4, -2, 5]]+2"));
        
        // Matrix addition.
        assertEquals("[[1, 3, 6], [8, 5, 0]]", calculator.evaluate("[[1,3,1],[1,0,0]] + [[0,0,5],[7,5,0]]"));
        
        // Scalar subtraction.
        assertEquals("[[1, -6, 5], [-2, 4, -3]]", calculator.evaluate("2-[[1, 8, -3], [4, -2, 5]]"));
        assertEquals("[[-1, 6, -5], [2, -4, 3]]", calculator.evaluate("[[1, 8, -3], [4, -2, 5]]-2"));
        
        // Matrix subtraction.
        assertEquals("[[1, 3, -4], [-6, -5, 0]]", calculator.evaluate("[[1,3,1],[1,0,0]] - [[0,0,5],[7,5,0]]"));
        
        // Negation.
        assertEquals("[[-1, -8, 3], [-4, 2, -5]]", calculator.evaluate("-[[1, 8, -3], [4, -2, 5]]"));
        
        // Transposition.
        assertEquals("[]", calculator.evaluate("Transpose([])"));
        assertEquals("[[1], [2]]", calculator.evaluate("Transpose([[1, 2]])"));
        assertEquals("[[1, 2]]", calculator.evaluate("Transpose([[1], [2]])"));
        assertEquals("[[1, 3], [2, 4]]", calculator.evaluate("Transpose([[1, 2], [3, 4]])"));
        assertEquals("[[1, 0], [2, -6], [3, 0]]", calculator.evaluate("Transpose([[1, 2, 3], [0, -6, 0]])"));
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
