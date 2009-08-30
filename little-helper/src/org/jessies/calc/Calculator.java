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
import java.util.*;
import org.jessies.test.*;

// FIXME: Mac OS' calculator offers -d variants of all the trig functions for degrees. that, or offer constants to multiply by to convert to degrees/radians?
// FIXME: higher-order built-in functions like http://www.vitanuova.com/inferno/man/1/calc.html (sum, product, integral, differential, solve).
// FIXME: integer division (//).
// FIXME: logical not (prefix !).
public class Calculator {
    private final Map<String, Node> constants;
    private final Map<String, CalculatorFunction> functions;
    private final Map<CalculatorToken, CalculatorFunction> operators;
    private final Map<String, Node> variables;
    
    private CalculatorLexer lexer;
    
    public Calculator() {
        this.constants = new HashMap<String, Node>();
        this.functions = new HashMap<String, CalculatorFunction>();
        this.operators = new EnumMap<CalculatorToken, CalculatorFunction>(CalculatorToken.class);
        this.variables = new HashMap<String, Node>();
        
        initBuiltInConstants();
        initBuiltInFunctions();
    }
    
    private void initBuiltInConstants() {
        // FIXME: use higher-precision string forms?
        constants.put("e", new CalculatorNumberNode(new BigDecimal(Math.E)));
        
        final Node pi = new CalculatorNumberNode(new BigDecimal(Math.PI));
        constants.put("pi", pi);
        constants.put("\u03c0", pi);
    }
    
    private void initBuiltInFunctions() {
        // FIXME: acosh, asinh, atanh, chop, clip, sign(um), int(eger_part), frac(tional_part)
        functions.put("abs",             new CalculatorFunctions.Abs());
        functions.put("acos",            new CalculatorFunctions.Acos());
        functions.put("asin",            new CalculatorFunctions.Asin());
        functions.put("atan",            new CalculatorFunctions.Atan());
        functions.put("atan2",           new CalculatorFunctions.Atan2());
        functions.put("BitAnd",          new CalculatorFunctions.BitAnd());
        functions.put("BitNot",          new CalculatorFunctions.BitNot());
        functions.put("BitOr",           new CalculatorFunctions.BitOr());
        functions.put("BitShiftLeft",    new CalculatorFunctions.BitShiftLeft());
        functions.put("BitShiftRight",   new CalculatorFunctions.BitShiftRight());
        functions.put("BitXor",          new CalculatorFunctions.BitXor());
        functions.put("cbrt",            new CalculatorFunctions.Cbrt());
        final CalculatorFunction ceiling = new CalculatorFunctions.Ceiling();
        functions.put("ceil",            ceiling);
        functions.put("ceiling",         ceiling);
        functions.put("cos",             new CalculatorFunctions.Cos());
        functions.put("cosh",            new CalculatorFunctions.Cosh());
        functions.put("define",          new CalculatorFunctions.Define());
        functions.put("Divide",          new CalculatorFunctions.Divide());
        functions.put("Equal",           new CalculatorFunctions.Equal());
        functions.put("exp",             new CalculatorFunctions.Exp());
        functions.put("factorial",       new CalculatorFunctions.Factorial());
        functions.put("floor",           new CalculatorFunctions.Floor());
        functions.put("Greater",         new CalculatorFunctions.Greater());
        functions.put("GreaterEqual",    new CalculatorFunctions.GreaterEqual());
        functions.put("hypot",           new CalculatorFunctions.Hypot());
        functions.put("is_prime",        new CalculatorFunctions.IsPrime());
        functions.put("Less",            new CalculatorFunctions.Less());
        functions.put("LessEqual",       new CalculatorFunctions.LessEqual());
        functions.put("log",             new CalculatorFunctions.Log());
        functions.put("log10",           new CalculatorFunctions.Log10());
        functions.put("log2",            new CalculatorFunctions.Log2());
        functions.put("logE",            new CalculatorFunctions.LogE());
        functions.put("Mod",             new CalculatorFunctions.Mod());
        functions.put("not",             new CalculatorFunctions.Not());
        functions.put("Plus",            new CalculatorFunctions.Plus());
        functions.put("Power",           new CalculatorFunctions.Power());
        final CalculatorFunction random = new CalculatorFunctions.Random();
        functions.put("rand",            random);
        functions.put("random",          random);
        functions.put("round",           new CalculatorFunctions.Round());
        functions.put("sin",             new CalculatorFunctions.Sin());
        functions.put("sinh",            new CalculatorFunctions.Sinh());
        functions.put("sqrt",            new CalculatorFunctions.Sqrt());
        functions.put("Subtract",        new CalculatorFunctions.Subtract());
        functions.put("tan",             new CalculatorFunctions.Tan());
        functions.put("tanh",            new CalculatorFunctions.Tanh());
        functions.put("Times",           new CalculatorFunctions.Times());
        functions.put("Unequal",         new CalculatorFunctions.Unequal());
        
        final CalculatorFunction sum = new CalculatorFunctions.Sum();
        functions.put("sum",             sum);
        functions.put("\u03a3",          sum); // Unicode Greek capital letter sigma.
        functions.put("\u2211",          sum); // Unicode summation sign.
        
        final CalculatorFunction product = new CalculatorFunctions.Product();
        functions.put("product",         product);
        functions.put("\u03a0",          product); // Unicode Greek capital letter pi.
        functions.put("\u220f",          product); // Unicode product sign.
        
        operators.put(CalculatorToken.B_AND, functions.get("BitAnd"));
        operators.put(CalculatorToken.B_NOT, functions.get("BitNot"));
        operators.put(CalculatorToken.B_OR,  functions.get("BitOr"));
        operators.put(CalculatorToken.DIV,   functions.get("Divide"));
        operators.put(CalculatorToken.EQ,    functions.get("Equal"));
        operators.put(CalculatorToken.GE,    functions.get("GreaterEqual"));
        operators.put(CalculatorToken.GT,    functions.get("Greater"));
        operators.put(CalculatorToken.LE,    functions.get("LessEqual"));
        operators.put(CalculatorToken.LT,    functions.get("Less"));
        operators.put(CalculatorToken.MINUS, functions.get("Subtract"));
        operators.put(CalculatorToken.MOD,   functions.get("Mod"));
        operators.put(CalculatorToken.MUL,   functions.get("Times"));
        operators.put(CalculatorToken.NE,    functions.get("Unequal"));
        operators.put(CalculatorToken.PLUS,  functions.get("Plus"));
        operators.put(CalculatorToken.POW,   functions.get("Power"));
        operators.put(CalculatorToken.SHL,   functions.get("BitShiftLeft"));
        operators.put(CalculatorToken.SHR,   functions.get("BitShiftRight"));
    }
    
    public String evaluate(String expression) throws CalculatorError {
        this.lexer = new CalculatorLexer(expression);
        
        Node ast = parseExpr();
        expect(CalculatorToken.END_OF_INPUT);
        
        //System.err.println(ast);
        BigDecimal value = ast.value(this);
        setVariable("Ans", new CalculatorNumberNode(value));
        return value.toString();
    }
    
    public Node getVariable(String name) {
        return variables.get(name);
    }
    
    public void setVariable(String name, Node newValue) {
        variables.put(name, newValue);
    }
    
    private Node parseExpr() {
        return parseAssignmentExpression();
    }
    
    // Mathematica operator precedence: http://reference.wolfram.com/mathematica/tutorial/OperatorInputForms.html
    
    // = (assignment)
    private Node parseAssignmentExpression() {
        Node result = parseOrExpression();
        if (lexer.token() == CalculatorToken.ASSIGN) {
            lexer.nextToken();
            result = new CalculatorFunctionApplicationNode(functions.get("define"), Arrays.asList(result, parseOrExpression()));
        }
        return result;
        
    }
    
    // |
    private Node parseOrExpression() {
        Node result = parseAndExpression();
        while (lexer.token() == CalculatorToken.B_OR) {
            lexer.nextToken();
            // FIXME: make BitOr varargs.
            result = new CalculatorFunctionApplicationNode(operators.get(CalculatorToken.B_OR), Arrays.asList(result, parseAndExpression()));
        }
        return result;
    }
    
    // &
    private Node parseAndExpression() {
        Node result = parseNotExpression();
        while (lexer.token() == CalculatorToken.B_AND) {
            lexer.nextToken();
            // FIXME: make BitAnd varargs.
            result = new CalculatorFunctionApplicationNode(operators.get(CalculatorToken.B_AND), Arrays.asList(result, parseNotExpression()));
        }
        return result;
    }
    
    // !
    private Node parseNotExpression() {
        if (lexer.token() == CalculatorToken.PLING) {
            lexer.nextToken();
            return new CalculatorFunctionApplicationNode(functions.get("not"), Collections.singletonList(parseNotExpression()));
        } else {
            return parseRelationalExpression();
        }
    }
    
    // == >= > <= < !=
    private Node parseRelationalExpression() {
        Node result = parseShiftExpression();
        while (lexer.token() == CalculatorToken.EQ || lexer.token() == CalculatorToken.GE || lexer.token() == CalculatorToken.GT || lexer.token() == CalculatorToken.LE || lexer.token() == CalculatorToken.LT || lexer.token() == CalculatorToken.NE) {
            final CalculatorFunction function = operators.get(lexer.token());
            lexer.nextToken();
            result = new CalculatorFunctionApplicationNode(function, Arrays.asList(result, parseShiftExpression()));
        }
        return result;
    }
    
    // << >>
    private Node parseShiftExpression() {
        Node result = parseAdditiveExpression();
        while (lexer.token() == CalculatorToken.SHL || lexer.token() == CalculatorToken.SHR) {
            final CalculatorFunction function = operators.get(lexer.token());
            lexer.nextToken();
            result = new CalculatorFunctionApplicationNode(function, Arrays.asList(result, parseAdditiveExpression()));
        }
        return result;
    }
    
    // + -
    private Node parseAdditiveExpression() {
        Node result = parseMultiplicativeExpression();
        while (lexer.token() == CalculatorToken.PLUS || lexer.token() == CalculatorToken.MINUS) {
            final CalculatorFunction function = operators.get(lexer.token());
            lexer.nextToken();
            result = new CalculatorFunctionApplicationNode(function, Arrays.asList(result, parseMultiplicativeExpression()));
        }
        return result;
    }
    
    // * / %
    private Node parseMultiplicativeExpression() {
        Node result = parseUnaryExpression();
        while (lexer.token() == CalculatorToken.MUL || lexer.token() == CalculatorToken.DIV || lexer.token() == CalculatorToken.MOD) {
            final CalculatorFunction function = operators.get(lexer.token());
            lexer.nextToken();
            result = new CalculatorFunctionApplicationNode(function, Arrays.asList(result, parseUnaryExpression()));
        }
        return result;
    }
    
    // ~ -
    private Node parseUnaryExpression() {
        if (lexer.token() == CalculatorToken.MINUS) {
            lexer.nextToken();
            // Convert (-f) to (-1*f) for simplicity.
            return new CalculatorFunctionApplicationNode(operators.get(CalculatorToken.MUL), Arrays.asList(new CalculatorNumberNode(BigDecimal.ONE.negate()), parseUnaryExpression()));
        } else if (lexer.token() == CalculatorToken.B_NOT) {
            lexer.nextToken();
            return new CalculatorFunctionApplicationNode(operators.get(CalculatorToken.B_NOT), Collections.singletonList(parseUnaryExpression()));
        }
        return parseSqrtExpression();
    }
    
    // sqrt
    private Node parseSqrtExpression() {
        if (lexer.token() == CalculatorToken.SQRT) {
            lexer.nextToken();
            return new CalculatorFunctionApplicationNode(functions.get("sqrt"), Collections.singletonList(parseSqrtExpression()));
        } else {
            return parseExponentiationExpression();
        }
    }
    
    // ^
    private Node parseExponentiationExpression() {
        Node result = parseFactorialExpression();
        if (lexer.token() == CalculatorToken.POW) {
            lexer.nextToken();
            result = new CalculatorFunctionApplicationNode(operators.get(CalculatorToken.POW), Arrays.asList(result, parseExponentiationExpression()));
        }
        return result;
    }
    
    // postfix-!
    private Node parseFactorialExpression() {
        Node result = parseFactor();
        if (lexer.token() == CalculatorToken.PLING) {
            expect(CalculatorToken.PLING);
            result = new CalculatorFunctionApplicationNode(functions.get("factorial"), Collections.singletonList(result));
        }
        return result;
    }
    
    private Node parseFactor() {
        if (lexer.token() == CalculatorToken.OPEN_PARENTHESIS) {
            expect(CalculatorToken.OPEN_PARENTHESIS);
            Node result = parseExpr();
            expect(CalculatorToken.CLOSE_PARENTHESIS);
            return result;
        } else if (lexer.token() == CalculatorToken.NUMBER) {
            Node result = new CalculatorNumberNode(lexer.number());
            expect(CalculatorToken.NUMBER);
            return result;
        } else if (lexer.token() == CalculatorToken.IDENTIFIER) {
            final String identifier = lexer.identifier();
            expect(CalculatorToken.IDENTIFIER);
            Node result = constants.get(identifier);
            if (result == null) {
                final CalculatorFunction fn = functions.get(identifier);
                if (fn != null) {
                    result = new CalculatorFunctionApplicationNode(fn, parseArgs());
                } else {
                    result = new CalculatorVariableNode(identifier);
                }
            }
            return result;
        } else {
            throw new CalculatorError("unexpected " + quoteTokenForErrorMessage(lexer.token()));
        }
    }
    
    // '(' expr [ ',' expr ] ')'
    private List<Node> parseArgs() {
        final List<Node> result = new LinkedList<Node>();
        expect(CalculatorToken.OPEN_PARENTHESIS);
        while (lexer.token() != CalculatorToken.CLOSE_PARENTHESIS) {
            result.add(parseExpr());
            if (lexer.token() == CalculatorToken.COMMA) {
                expect(CalculatorToken.COMMA);
                continue;
            }
        }
        expect(CalculatorToken.CLOSE_PARENTHESIS);
        return result;
    }
    
    private void expect(CalculatorToken what) {
        if (lexer.token() != what) {
            throw new CalculatorError("expected " + quoteTokenForErrorMessage(what) + ", got " + quoteTokenForErrorMessage(lexer.token()) + " instead");
        }
        lexer.nextToken();
    }
    
    private static String quoteTokenForErrorMessage(CalculatorToken token) {
        String result = token.toString();
        if (result.length() > 2) {
            // We probably already have something usable like "end of input".
            return result;
        }
        // Quote operators.
        return "'" + result + "'";
    }
    
    @Test private static void testArithmetic() {
        Assert.equals(new Calculator().evaluate("0"), "0");
        Assert.equals(new Calculator().evaluate("1"), "1");
        Assert.equals(new Calculator().evaluate("-1"), "-1");
        Assert.equals(new Calculator().evaluate("--1"), "1");
        Assert.equals(new Calculator().evaluate("1.00"), "1.00");
        
        Assert.equals(new Calculator().evaluate(".2"), "0.2");
        
        Assert.equals(new Calculator().evaluate("1.2E3"), "1.2E+3");
        Assert.equals(new Calculator().evaluate("1E3"), "1E+3");
        Assert.equals(new Calculator().evaluate("1.E3"), "1E+3");
        Assert.equals(new Calculator().evaluate(".1E3"), "1E+2");
        
        Assert.equals(new Calculator().evaluate("1+2+3"), "6");
        Assert.equals(new Calculator().evaluate("1+-2"), "-1");
        Assert.equals(new Calculator().evaluate("3-2-1"), "0");
        Assert.equals(new Calculator().evaluate("10000+0.001"), "10000.001");
        Assert.equals(new Calculator().evaluate("0.001+10000"), "10000.001");
        Assert.equals(new Calculator().evaluate("10000-0.001"), "9999.999");
        Assert.equals(new Calculator().evaluate("0.001-10000"), "-9999.999");
        
        Assert.equals(new Calculator().evaluate("3*4"), "12");
        Assert.equals(new Calculator().evaluate("-3*4"), "-12");
        Assert.equals(new Calculator().evaluate("3*-4"), "-12");
        Assert.equals(new Calculator().evaluate("-3*-4"), "12");
        
        Assert.equals(new Calculator().evaluate("1+2*3"), "7");
        Assert.equals(new Calculator().evaluate("(1+2)*3"), "9");
        
        Assert.equals(new Calculator().evaluate("1/2"), "0.5");
        
        Assert.equals(new Calculator().evaluate("3%4"), "3");
        Assert.equals(new Calculator().evaluate("4%4"), "0");
        Assert.equals(new Calculator().evaluate("5%4"), "1");
    }
    
    @Test private static void testRelationalOperations() {
        Assert.equals(new Calculator().evaluate("1<2"), "1");
        Assert.equals(new Calculator().evaluate("2<2"), "0");
        Assert.equals(new Calculator().evaluate("2<1"), "0");
        Assert.equals(new Calculator().evaluate("1<=2"), "1");
        Assert.equals(new Calculator().evaluate("2<=2"), "1");
        Assert.equals(new Calculator().evaluate("2<=1"), "0");
        Assert.equals(new Calculator().evaluate("1>2"), "0");
        Assert.equals(new Calculator().evaluate("2>2"), "0");
        Assert.equals(new Calculator().evaluate("2>1"), "1");
        Assert.equals(new Calculator().evaluate("1>=2"), "0");
        Assert.equals(new Calculator().evaluate("2>=2"), "1");
        Assert.equals(new Calculator().evaluate("2>=1"), "1");
        Assert.equals(new Calculator().evaluate("1==2"), "0");
        Assert.equals(new Calculator().evaluate("2==2"), "1");
        Assert.equals(new Calculator().evaluate("2==1"), "0");
        Assert.equals(new Calculator().evaluate("1!=2"), "1");
        Assert.equals(new Calculator().evaluate("2!=2"), "0");
        Assert.equals(new Calculator().evaluate("2!=1"), "1");
    }
    
    @Test private static void testNot() {
        Assert.equals(new Calculator().evaluate("!(1==2)"), "1");
        Assert.equals(new Calculator().evaluate("!(2==2)"), "0");
        Assert.equals(new Calculator().evaluate("!!(2==2)"), "1");
    }
    
    @Test private static void testShifts() {
        Assert.equals(new Calculator().evaluate("1<<4"), "16");
        Assert.equals(new Calculator().evaluate("(12<<3)>>3"), "12");
    }
    
    @Test private static void testBitOperations() {
        Assert.equals(new Calculator().evaluate("(0x1234 & 0xff0) == 0x230"), "1");
        Assert.equals(new Calculator().evaluate("(0x1200 | 0x34) == 0x1234"), "1");
        Assert.equals(new Calculator().evaluate("BitXor(5, 3)"), "6");
        Assert.equals(new Calculator().evaluate("((0x1234 & ~0xff) | 0x56) == 0x1256"), "1");
        Assert.equals(new Calculator().evaluate("~3"), "-4");
        Assert.equals(new Calculator().evaluate("~~3"), "3");
    }
    
    @Test private static void testExponentiation() {
        Assert.equals(new Calculator().evaluate("2^3"), "8");
        Assert.equals(new Calculator().evaluate("2^3^4"), "2417851639229258349412352");
        Assert.equals(new Calculator().evaluate("4^0.5"), "2");
        Assert.equals(new Calculator().evaluate("-10^2"), "-100");
        Assert.equals(new Calculator().evaluate("(-10)^2"), "100");
    }
    
    @Test private static void testConstants() {
        Assert.equals(Double.valueOf(new Calculator().evaluate("e")), Math.E, 0.000001);
        Assert.equals(Double.valueOf(new Calculator().evaluate("pi")), Math.PI, 0.000001);
        Assert.equals(new Calculator().evaluate("pi == \u03c0"), "1");
    }
    
    @Test private static void testFunctions() {
        // FIXME: better tests?
        Assert.equals(new Calculator().evaluate("abs(2)"), "2");
        Assert.equals(new Calculator().evaluate("abs(-2)"), "2");
        Assert.equals(new Calculator().evaluate("acos(1)"), "0");
        Assert.equals(new Calculator().evaluate("asin(0)"), "0");
        Assert.equals(new Calculator().evaluate("acos(0) == asin(1)"), "1");
        Assert.equals(new Calculator().evaluate("atan(0)"), "0");
        Assert.equals(new Calculator().evaluate("cbrt(27)"), "3");
        Assert.equals(new Calculator().evaluate("ceil(1.2)"), "2");
        Assert.equals(new Calculator().evaluate("cos(0)"), "1");
        Assert.equals(new Calculator().evaluate("cos(pi)"), "-1");
        Assert.equals(new Calculator().evaluate("cosh(0)"), "1");
        Assert.equals(Double.valueOf(new Calculator().evaluate("exp(1)/e")), 1.0, 0.000001);
        Assert.equals(new Calculator().evaluate("factorial(5)"), "120");
        Assert.equals(new Calculator().evaluate("factorial(5) == 5!"), "1");
        Assert.equals(new Calculator().evaluate("floor(1.2)"), "1");
        Assert.equals(new Calculator().evaluate("hypot(3, 4)"), "5");
        
        Assert.equals(new Calculator().evaluate("is_prime(0)"), "0");
        Assert.equals(new Calculator().evaluate("is_prime(1)"), "0");
        Assert.equals(new Calculator().evaluate("is_prime(2)"), "1");
        Assert.equals(new Calculator().evaluate("is_prime(3)"), "1");
        Assert.equals(new Calculator().evaluate("is_prime(4)"), "0");
        Assert.equals(new Calculator().evaluate("is_prime(5)"), "1");
        Assert.equals(new Calculator().evaluate("is_prime(-4)"), "0");
        Assert.equals(new Calculator().evaluate("is_prime(-5)"), "1");
        
        Assert.equals(new Calculator().evaluate("log(2, 1024)"), "10");
        Assert.equals(new Calculator().evaluate("log2(1024)"), "10");
        Assert.equals(new Calculator().evaluate("logE(exp(4))"), "4");
        Assert.equals(new Calculator().evaluate("log10(1000)"), "3");
        Assert.equals(new Calculator().evaluate("round(1.2)"), "1");
        Assert.equals(new Calculator().evaluate("round(1.8)"), "2");
        Assert.equals(new Calculator().evaluate("sin(0)"), "0");
        Assert.equals(new Calculator().evaluate("sin(pi/2)"), "1");
        Assert.equals(new Calculator().evaluate("sinh(0)"), "0");
        Assert.equals(new Calculator().evaluate("sqrt(81)"), "9");
        Assert.equals(new Calculator().evaluate("tan(0)"), "0");
        Assert.equals(new Calculator().evaluate("tanh(0)"), "0");
    }
    
    @Test private static void testSqrt() {
        Assert.equals(new Calculator().evaluate("\u221a4"), "2");
        // Check /3*2 == 2*/3 (where / is ASCII-safe \u221a).
        Assert.startsWith(new Calculator().evaluate("\u221a3*2"), "3.464");
    }
    
    @Test private static void testSum() {
        Assert.equals(new Calculator().evaluate("sum(0, 10, i)"), "55");
        Assert.equals(new Calculator().evaluate("sum(0, 10.2, i)"), "55");
        Assert.equals(new Calculator().evaluate("sum(0, 10, i^2)"), "385");
        Assert.equals(Double.valueOf(new Calculator().evaluate("sum(0,30,1/i!)-e")), 0.0, 0.000001);
        // FIXME: failure test for min > max.
    }
    
    @Test private static void testProduct() {
        Assert.equals(new Calculator().evaluate("product(1, 10, i)"), "3628800");
        Assert.equals(new Calculator().evaluate("product(1, 10.2, i)"), "3628800");
        Assert.equals(new Calculator().evaluate("product(1, 6, i^2)"), "518400");
        // FIXME: failure test for min > max.
    }
    
    @Test private static void testAns() {
        final Calculator calculator = new Calculator();
        Assert.equals(calculator.evaluate("0"), "0");
        Assert.equals(calculator.evaluate("1+Ans"), "1");
        Assert.equals(calculator.evaluate("1+Ans"), "2");
        Assert.equals(calculator.evaluate("Ans*2"), "4");
    }
    
    @Test private static void testVariables() {
        final Calculator calculator = new Calculator();
        Assert.equals(calculator.evaluate("a = 2"), "2");
        Assert.equals(calculator.evaluate("a"), "2");
        Assert.equals(calculator.evaluate("2*a"), "4");
    }
}
