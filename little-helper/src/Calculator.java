/*
 * This file is part of LittleHelper.
 * Copyright (C) 2009 Elliott Hughes <enh@jessies.org>.
 * 
 * LittleHelper is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Talc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.math.*;
import java.util.*;
import org.jessies.test.*;

// FIXME: built-in functions (abs, acos, asin, atan, atan2, cbrt, ceil[ing], cos, cosh, exp, floor, hypot, log(value, base), log2, logE, log10, random, sin, sinh, sqrt, tan, tanh).
// FIXME: Mac OS' calculator offers -d variants of all the trig functions for degrees. that, or offer constants to multiply by to convert to degrees/radians?
// FIXME: higher-order built-in functions like http://www.vitanuova.com/inferno/man/1/calc.html (sum, product, integral, differential, solve).
// FIXME: integer division (//).
// FIXME: factorial (postfix !).
// FIXME: logical not (prefix !).
public class Calculator {
    public static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);
    
    private final CalculatorLexer lexer;
    private final Map<String, CalculatorAstNode> namespace;
    
    public Calculator(String expression) {
        this.lexer = new CalculatorLexer(expression);
        this.namespace = new HashMap<String, CalculatorAstNode>();
        
        initBuiltInConstants();
    }
    
    private void initBuiltInConstants() {
        // FIXME: use higher-precision string forms?
        namespace.put("e", new CalculatorNumberNode(new BigDecimal(Math.E)));
        
        final CalculatorAstNode pi = new CalculatorNumberNode(new BigDecimal(Math.PI));
        namespace.put("pi", pi);
        namespace.put("π", pi);
    }
    
    public String evaluate() throws CalculatorError {
        CalculatorAstNode ast = parseExpr();
        //System.err.println(ast);
        BigDecimal value = ast.value();
        return value.toString();
    }
    
    private CalculatorAstNode parseExpr() {
        return parseOrExpression();
    }
    
    // Mathematica operator precedence: http://reference.wolfram.com/mathematica/tutorial/OperatorInputForms.html
    
    // |
    private CalculatorAstNode parseOrExpression() {
        CalculatorAstNode result = parseXorExpression();
        while (lexer.token() == CalculatorToken.B_OR) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseXorExpression());
        }
        return result;
    }
    
    // ^
    private CalculatorAstNode parseXorExpression() {
        CalculatorAstNode result = parseAndExpression();
        while (lexer.token() == CalculatorToken.B_XOR) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseAndExpression());
        }
        return result;
    }
    
    // &
    private CalculatorAstNode parseAndExpression() {
        CalculatorAstNode result = parseRelationalExpression();
        while (lexer.token() == CalculatorToken.B_AND) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseRelationalExpression());
        }
        return result;
    }
    
    // == >= > <= < !=
    private CalculatorAstNode parseRelationalExpression() {
        CalculatorAstNode result = parseShiftExpression();
        while (lexer.token() == CalculatorToken.EQ || lexer.token() == CalculatorToken.GE || lexer.token() == CalculatorToken.GT || lexer.token() == CalculatorToken.LE || lexer.token() == CalculatorToken.LT || lexer.token() == CalculatorToken.NE) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseShiftExpression());
        }
        return result;
    }
    
    // << >>
    private CalculatorAstNode parseShiftExpression() {
        CalculatorAstNode result = parseAdditiveExpression();
        while (lexer.token() == CalculatorToken.SHL || lexer.token() == CalculatorToken.SHR) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseAdditiveExpression());
        }
        return result;
    }
    
    // + -
    private CalculatorAstNode parseAdditiveExpression() {
        CalculatorAstNode result = parseMultiplicativeExpression();
        while (lexer.token() == CalculatorToken.PLUS || lexer.token() == CalculatorToken.MINUS) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseMultiplicativeExpression());
        }
        return result;
    }
    
    // * / %
    private CalculatorAstNode parseMultiplicativeExpression() {
        CalculatorAstNode result = parseUnaryExpression();
        while (lexer.token() == CalculatorToken.MUL || lexer.token() == CalculatorToken.DIV || lexer.token() == CalculatorToken.MOD) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseUnaryExpression());
        }
        return result;
    }
    
    // ~ -
    private CalculatorAstNode parseUnaryExpression() {
        if (lexer.token() == CalculatorToken.MINUS) {
            lexer.nextToken();
            // Convert (-f) to (0-f) for simplicity.
            return new CalculatorOpNode(CalculatorToken.MINUS, new CalculatorNumberNode(new BigDecimal("0", Calculator.MATH_CONTEXT)), parseUnaryExpression());
        } else if (lexer.token() == CalculatorToken.B_NOT) {
            lexer.nextToken();
            return new CalculatorOpNode(CalculatorToken.B_NOT, parseUnaryExpression(), null);
        }
        return parseExponentiationExpression();
    }
    
    // sqrt
    
    // **
    private CalculatorAstNode parseExponentiationExpression() {
        CalculatorAstNode result = parseFactor();
        if (lexer.token() == CalculatorToken.POW) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseExponentiationExpression());
        }
        return result;
    }
    
    // !
    
    private CalculatorAstNode parseFactor() {
        if (lexer.token() == CalculatorToken.OPEN_PARENTHESIS) {
            expect(CalculatorToken.OPEN_PARENTHESIS);
            CalculatorAstNode result = parseExpr();
            expect(CalculatorToken.CLOSE_PARENTHESIS);
            return result;
        } else if (lexer.token() == CalculatorToken.NUMBER) {
            CalculatorAstNode result = new CalculatorNumberNode(lexer.number());
            expect(CalculatorToken.NUMBER);
            return result;
        } else if (lexer.token() == CalculatorToken.IDENTIFIER) {
            CalculatorAstNode result = namespace.get(lexer.identifier());
            if (result == null) {
                // FIXME: support free variables for symbolic computation.
                throw new CalculatorError("undefined function or variable '" + lexer.identifier() + "'");
            }
            expect(CalculatorToken.IDENTIFIER);
            return result;
        } else {
            throw new CalculatorError("unexpected '" + lexer.token() + "'");
        }
    }
    
    private void expect(CalculatorToken what) {
        if (lexer.token() != what) {
            throw new CalculatorError("expected " + what + ", got " + lexer.token() + " instead");
        }
        lexer.nextToken();
    }
    
    @Test private static void testArithmetic() {
        Assert.equals(new Calculator("0").evaluate(), "0");
        Assert.equals(new Calculator("1").evaluate(), "1");
        Assert.equals(new Calculator("-1").evaluate(), "-1");
        Assert.equals(new Calculator("--1").evaluate(), "1");
        Assert.equals(new Calculator("1.00").evaluate(), "1.00");
        
        Assert.equals(new Calculator("1+2+3").evaluate(), "6");
        Assert.equals(new Calculator("1+-2").evaluate(), "-1");
        Assert.equals(new Calculator("3-2-1").evaluate(), "0");
        Assert.equals(new Calculator("10000+0.001").evaluate(), "10000.001");
        Assert.equals(new Calculator("0.001+10000").evaluate(), "10000.001");
        Assert.equals(new Calculator("10000-0.001").evaluate(), "9999.999");
        Assert.equals(new Calculator("0.001-10000").evaluate(), "-9999.999");
        
        Assert.equals(new Calculator("3*4").evaluate(), "12");
        Assert.equals(new Calculator("-3*4").evaluate(), "-12");
        Assert.equals(new Calculator("3*-4").evaluate(), "-12");
        Assert.equals(new Calculator("-3*-4").evaluate(), "12");
        
        Assert.equals(new Calculator("1+2*3").evaluate(), "7");
        Assert.equals(new Calculator("(1+2)*3").evaluate(), "9");
        
        Assert.equals(new Calculator("1/2").evaluate(), "0.5");
        
        Assert.equals(new Calculator("3%4").evaluate(), "3");
        Assert.equals(new Calculator("4%4").evaluate(), "0");
        Assert.equals(new Calculator("5%4").evaluate(), "1");
    }
    
    @Test private static void testRelationalOperations() {
        Assert.equals(new Calculator("1<2").evaluate(), "0");
        Assert.equals(new Calculator("2<2").evaluate(), "1");
        Assert.equals(new Calculator("2<1").evaluate(), "1");
        Assert.equals(new Calculator("1<=2").evaluate(), "0");
        Assert.equals(new Calculator("2<=2").evaluate(), "0");
        Assert.equals(new Calculator("2<=1").evaluate(), "1");
        Assert.equals(new Calculator("1>2").evaluate(), "1");
        Assert.equals(new Calculator("2>2").evaluate(), "1");
        Assert.equals(new Calculator("2>1").evaluate(), "0");
        Assert.equals(new Calculator("1>=2").evaluate(), "1");
        Assert.equals(new Calculator("2>=2").evaluate(), "0");
        Assert.equals(new Calculator("2>=1").evaluate(), "0");
        Assert.equals(new Calculator("1==2").evaluate(), "1");
        Assert.equals(new Calculator("2==2").evaluate(), "0");
        Assert.equals(new Calculator("2==1").evaluate(), "1");
        Assert.equals(new Calculator("1!=2").evaluate(), "0");
        Assert.equals(new Calculator("2!=2").evaluate(), "1");
        Assert.equals(new Calculator("2!=1").evaluate(), "0");
    }
    
    @Test private static void testShifts() {
        Assert.equals(new Calculator("1<<4").evaluate(), "16");
        Assert.equals(new Calculator("(12<<3)>>3").evaluate(), "12");
    }
    
    @Test private static void testBitOperations() {
        Assert.equals(new Calculator("(0x1234 & 0xff0) == 0x230").evaluate(), "0");
        Assert.equals(new Calculator("(0x1200 | 0x34) == 0x1234").evaluate(), "0");
        Assert.equals(new Calculator("5 ^ 3").evaluate(), "6");
        Assert.equals(new Calculator("((0x1234 & ~0xff) | 0x56) == 0x1256").evaluate(), "0");
        Assert.equals(new Calculator("~3").evaluate(), "-4");
        Assert.equals(new Calculator("~~3").evaluate(), "3");
    }
    
    @Test private static void testExponentiation() {
        Assert.equals(new Calculator("2**3").evaluate(), "8");
        Assert.equals(new Calculator("2**3**4").evaluate(), "2417851639229258349412352");
        Assert.equals(new Calculator("4**0.5").evaluate(), "2");
        Assert.equals(new Calculator("-10**2").evaluate(), "-100");
        Assert.equals(new Calculator("(-10)**2").evaluate(), "100");
    }
    
    @Test private static void testConstants() {
        Assert.equals(Double.valueOf(new Calculator("e").evaluate()), Math.E, 0.000001);
        Assert.equals(Double.valueOf(new Calculator("pi").evaluate()), Math.PI, 0.000001);
        Assert.equals(new Calculator("pi == π").evaluate(), "0");
    }
}
