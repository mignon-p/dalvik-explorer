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
import java.util.*;

// FIXME: Mac OS' calculator offers -d variants of all the trig functions for degrees. that, or offer constants to multiply by to convert to degrees/radians?
// FIXME: higher-order built-in functions like http://www.vitanuova.com/inferno/man/1/calc.html (sum, product, integral, differential, solve).
// FIXME: integer division (//).
public class Calculator {
    private final Map<String, CalculatorFunction> functions;
    private final Map<CalculatorToken, CalculatorFunction> operators;
    private final Map<String, Variable> variables;
    private final Variable ans;
    private boolean degreesMode = false;
    private CalculatorPlotter plotter;
    
    // Variable names are case-insensitive but case-preserving.
    // We implement case-insensitivity by using name.toLowerCase() as the key.
    // We preserve case by using a pair of the first-encountered name and the variable's value as the map's value type.
    static class Variable {
        String name;
        Node value;
        boolean isAssignable = true;
    }
    
    public Calculator() {
        this.functions = new HashMap<String, CalculatorFunction>();
        this.operators = new EnumMap<CalculatorToken, CalculatorFunction>(CalculatorToken.class);
        this.variables = new HashMap<String, Variable>();
        this.ans = initAns();
        initBuiltInConstants();
        initBuiltInFunctions();
    }
    
    public void setPlotter(CalculatorPlotter plotter) {
        this.plotter = plotter;
    }
    
    public CalculatorPlotter getPlotter() {
        return plotter;
    }
    
    private Variable initAns() {
        final Variable result = new Variable();
        result.name = "Ans";
        result.value = null;
        result.isAssignable = false;
        variables.put(result.name.toLowerCase(), result);
        return result;
    }
    
    private void initBuiltInConstants() {
        // FIXME: use higher-precision string forms?
        initConstant("e", new RealNode(Math.E));
        
        final Node pi = new RealNode(Math.PI);
        initConstant("pi", pi);
        initConstant("\u03c0", pi);
        
        initConstant("false", BooleanNode.FALSE);
        initConstant("true", BooleanNode.TRUE);
    }
    
    private void initConstant(String name, Node value) {
        final Variable constant = new Variable();
        constant.name = name;
        constant.value = value;
        constant.isAssignable = false;
        final String key = name.toLowerCase();
        variables.put(key, constant);
    }
    
    private void initBuiltInFunctions() {
        // FIXME: acosh, asinh, atanh, chop, clip.
        addFunction(new CalculatorFunctions.Abs(),            "Abs");
        addFunction(new CalculatorFunctions.Acos(),           "Acos", "ArcCos");
        addFunction(new CalculatorFunctions.And(),            "And");
        addFunction(new CalculatorFunctions.Asin(),           "Asin", "ArcSin");
        addFunction(new CalculatorFunctions.Atan2(),          "Atan2");
        addFunction(new CalculatorFunctions.Atan(),           "Atan", "ArcTan");
        addFunction(new CalculatorFunctions.BitAnd(),         "BitAnd");
        addFunction(new CalculatorFunctions.BitClear(),       "BitClear");
        addFunction(new CalculatorFunctions.BitGet(),         "BitGet");
        addFunction(new CalculatorFunctions.BitLength(),      "BitLength");
        addFunction(new CalculatorFunctions.BitNot(),         "BitNot");
        addFunction(new CalculatorFunctions.BitOr(),          "BitOr");
        addFunction(new CalculatorFunctions.BitSet(),         "BitSet");
        addFunction(new CalculatorFunctions.BitShiftRight() , "BitShiftRight");
        addFunction(new CalculatorFunctions.BitShiftLeft(),   "BitShiftLeft");
        addFunction(new CalculatorFunctions.BitXor(),         "BitXor");
        addFunction(new CalculatorFunctions.Boole(),          "Boole");
        addFunction(new CalculatorFunctions.Cbrt(),           "Cbrt");
        addFunction(new CalculatorFunctions.Ceiling(),        "Ceiling", "Ceil");
        addFunction(new CalculatorFunctions.Cosh(),           "Cosh");
        addFunction(new CalculatorFunctions.Cos(),            "Cos");
        addFunction(new CalculatorFunctions.Define(),         "Define");
        addFunction(new CalculatorFunctions.DigitCount(),     "DigitCount");
        addFunction(new CalculatorFunctions.Dimensions(),     "Dimensions");
        addFunction(new CalculatorFunctions.Divide(),         "Divide");
        addFunction(new CalculatorFunctions.Equal(),          "Equal");
        addFunction(new CalculatorFunctions.Exp(),            "Exp");
        addFunction(new CalculatorFunctions.Factorial(),      "Factorial");
        addFunction(new CalculatorFunctions.Filter(),         "Filter");
        addFunction(new CalculatorFunctions.Floor(),          "Floor");
        addFunction(new CalculatorFunctions.FractionalPart(), "FractionalPart");
        addFunction(new CalculatorFunctions.GCD(),            "GCD");
        addFunction(new CalculatorFunctions.GreaterEqual(),   "GreaterEqual");
        addFunction(new CalculatorFunctions.Greater(),        "Greater");
        addFunction(new CalculatorFunctions.Hypot(),          "Hypot");
        addFunction(new CalculatorFunctions.IdentityMatrix(), "IdentityMatrix");
        addFunction(new CalculatorFunctions.IntegerLength(),  "IntegerLength");
        addFunction(new CalculatorFunctions.IntegerPart(),    "IntegerPart");
        addFunction(new CalculatorFunctions.IsMatrix(),       "IsMatrix");
        addFunction(new CalculatorFunctions.IsPrime(),        "IsPrime");
        addFunction(new CalculatorFunctions.LCM(),            "LCM");
        addFunction(new CalculatorFunctions.Length(),         "Length");
        addFunction(new CalculatorFunctions.LessEqual(),      "LessEqual");
        addFunction(new CalculatorFunctions.Less(),           "Less");
        addFunction(new CalculatorFunctions.ListBuilder(),    "List");
        addFunction(new CalculatorFunctions.Log10(),          "Log10");
        addFunction(new CalculatorFunctions.Log2(),           "Log2");
        addFunction(new CalculatorFunctions.LogE(),           "LogE", "Ln");
        addFunction(new CalculatorFunctions.Log(),            "Log");
        addFunction(new CalculatorFunctions.Map(),            "Map");
        addFunction(new CalculatorFunctions.Max(),            "Max");
        addFunction(new CalculatorFunctions.Min(),            "Min");
        addFunction(new CalculatorFunctions.Mod(),            "Mod");
        addFunction(new CalculatorFunctions.nCr(),            "nCr");
        addFunction(new CalculatorFunctions.Not(),            "Not");
        addFunction(new CalculatorFunctions.nPr(),            "nPr");
        addFunction(new CalculatorFunctions.Or(),             "Or");
        addFunction(new CalculatorFunctions.Plot(),           "Plot");
        addFunction(new CalculatorFunctions.Plus(),           "Plus");
        addFunction(new CalculatorFunctions.Power(),          "Power");
        addFunction(new CalculatorFunctions.Product(),        "Product", /* Unicode Greek capital letter pi */ "\u03a0", /* Unicode product sign */ "\u220f");
        addFunction(new CalculatorFunctions.Range(),          "Range");
        addFunction(new CalculatorFunctions.Random(),         "Random", "rand");
        addFunction(new CalculatorFunctions.Reverse(),        "Reverse");
        addFunction(new CalculatorFunctions.Round(),          "Round");
        addFunction(new CalculatorFunctions.Sign(),           "Sign");
        addFunction(new CalculatorFunctions.Sinh(),           "Sinh");
        addFunction(new CalculatorFunctions.Sin(),            "Sin");
        addFunction(new CalculatorFunctions.Sqrt(),           "Sqrt");
        addFunction(new CalculatorFunctions.Sum(),            "Sum", /* Unicode Greek capital letter sigma */ "\u03a3", /* Unicode summation sign */ "\u2211");
        addFunction(new CalculatorFunctions.Subtract(),       "Subtract");
        addFunction(new CalculatorFunctions.Tanh(),           "Tanh");
        addFunction(new CalculatorFunctions.Tan(),            "Tan");
        addFunction(new CalculatorFunctions.Times(),          "Times");
        addFunction(new CalculatorFunctions.Total(),          "Total");
        addFunction(new CalculatorFunctions.Transpose(),      "Transpose");
        addFunction(new CalculatorFunctions.Unequal(),        "Unequal");
        
        operators.put(CalculatorToken.B_AND, functions.get("BitAnd"));
        operators.put(CalculatorToken.B_NOT, functions.get("BitNot"));
        operators.put(CalculatorToken.B_OR,  functions.get("BitOr"));
        operators.put(CalculatorToken.DIV,   functions.get("Divide"));
        operators.put(CalculatorToken.EQ,    functions.get("Equal"));
        operators.put(CalculatorToken.GE,    functions.get("GreaterEqual"));
        operators.put(CalculatorToken.GT,    functions.get("Greater"));
        operators.put(CalculatorToken.L_AND, functions.get("And"));
        operators.put(CalculatorToken.LE,    functions.get("LessEqual"));
        operators.put(CalculatorToken.L_OR,  functions.get("Or"));
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
    
    private void addFunction(CalculatorFunction function, String... names) {
        for (String name : names) {
            addUniqueFunction(function, name);
            final String lowerCaseName = name.toLowerCase();
            if (!lowerCaseName.equals(name)) {
                addUniqueFunction(function, lowerCaseName);
            }
            final String cStyleName = name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
            if (!cStyleName.equals(lowerCaseName)) {
                addUniqueFunction(function, cStyleName);
            }
        }
    }
    
    private void addUniqueFunction(CalculatorFunction function, String name) {
        // The checks in addFunction ensure no duplication for a single function.
        // This check is to avoid accidental duplication between functions.
        if (functions.get(name) != null) {
            throw new RuntimeException("function '" + name + "' already added");
        }
        functions.put(name, function);
    }
    
    private Node parse(String stringExpression) throws CalculatorError {
        final CalculatorParser parser = new CalculatorParser(this, stringExpression);
        return parser.parse();
    }
    
    private Node simplify(Node expression) {
        return expression.simplify(this);
    }
    
    public String evaluate(String stringExpression) throws CalculatorError {
        final Node expression = parse(stringExpression);
        final Node simplifiedExpression = simplify(expression);
        if (false) {
            final String expressionString = expression.toInputString();
            final String simplifiedString = simplifiedExpression.toInputString();
            System.err.println(expressionString + (expressionString.equals(simplifiedString) ? "" : (" --- " + simplifiedString)));
        }
        final long t0 = System.nanoTime();
        final Node result = expression.evaluate(this);
        final long t1 = System.nanoTime();
        // System.err.println((t1-t0) + " ns");
        ans.value = result;
        return result.toString();
    }
    
    public CalculatorFunction getFunction(String name) {
        return functions.get(name);
    }
    
    public CalculatorFunction getFunction(CalculatorToken token) {
        return operators.get(token);
    }
    
    public Node getVariable(String name) {
        final Variable v = variables.get(name.toLowerCase());
        return (v != null) ? v.value : null;
    }
    
    public void setVariable(String name, Node newValue) {
        final String key = name.toLowerCase();
        Variable v = variables.get(key);
        if (v == null) {
            v = new Variable();
            v.name = name;
            variables.put(key, v);
        } else  if (!v.isAssignable) {
            if (v.name.equals("Ans")) {
                throw new CalculatorError("can't assign a new value to Ans");
            } else {
                throw new CalculatorError("can't assign a new value to the constant " + v.name);
            }
        }
        v.value = newValue;
    }
    
    /**
     * Tells all trigonometric functions to take/return degrees rather than radians.
     */
    public void setDegreesMode(boolean degreesMode) {
        this.degreesMode = degreesMode;
    }
    
    public RealNode angleArgument(NumberNode n) {
        RealNode value = n.toReal();
        if (degreesMode) {
            value = new RealNode(Math.toRadians(value.doubleValue()));
        }
        return value;
    }
    
    public RealNode angleResult(NumberNode n) {
        RealNode value = n.toReal();
        if (degreesMode) {
            value = new RealNode(Math.toDegrees(value.doubleValue()));
        }
        return value;
    }
}
