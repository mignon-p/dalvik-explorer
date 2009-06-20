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

public class CalculatorFunctions {
    private CalculatorFunctions() {}
    
    private static BigDecimal fromDouble(double d) {
        return new BigDecimal(d, Calculator.MATH_CONTEXT);
    }
    
    public static class Abs extends CalculatorFunction {
        public Abs() {
            super("abs", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return args.get(0).value(environment).abs(Calculator.MATH_CONTEXT);
        }
    }
    
    public static class Acos extends CalculatorFunction {
        public Acos() {
            super("acos", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.acos(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Asin extends CalculatorFunction {
        public Asin() {
            super("asin", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.asin(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Atan extends CalculatorFunction {
        public Atan() {
            super("atan", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.atan(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Atan2 extends CalculatorFunction {
        public Atan2() {
            super("atan2", 2);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.atan2(args.get(0).value(environment).doubleValue(), args.get(1).value(environment).doubleValue()));
        }
    }
    
    public static class Cbrt extends CalculatorFunction {
        public Cbrt() {
            super("cbrt", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.cbrt(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Ceiling extends CalculatorFunction {
        public Ceiling() {
            super("ceiling", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.ceil(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Cos extends CalculatorFunction {
        public Cos() {
            super("cos", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.cos(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Cosh extends CalculatorFunction {
        public Cosh() {
            super("cosh", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.cosh(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Exp extends CalculatorFunction {
        public Exp() {
            super("exp", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.exp(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Floor extends CalculatorFunction {
        public Floor() {
            super("floor", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.floor(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Hypot extends CalculatorFunction {
        public Hypot() {
            super("hypot", 2);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.hypot(args.get(0).value(environment).doubleValue(), args.get(1).value(environment).doubleValue()));
        }
    }
    
    // log(base, n).
    public static class Log extends CalculatorFunction {
        public Log() {
            super("log", 2);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.log(args.get(1).value(environment).doubleValue()) / Math.log(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Log2 extends CalculatorFunction {
        public Log2() {
            super("log2", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.log(args.get(0).value(environment).doubleValue()) / Math.log(2.0));
        }
    }
    
    public static class LogE extends CalculatorFunction {
        public LogE() {
            super("logE", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.log(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Log10 extends CalculatorFunction {
        public Log10() {
            super("log10", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.log10(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Random extends CalculatorFunction {
        public Random() {
            super("random", 0);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.random());
        }
    }
    
    public static class Round extends CalculatorFunction {
        public Round() {
            super("round", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.round(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Sum extends CalculatorFunction {
        public Sum() {
            super("sum", 3);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            final BigDecimal iMin = args.get(0).value(environment);
            final BigDecimal iMax = args.get(1).value(environment);
            final CalculatorAstNode expr = args.get(2);
            
            if (iMin.compareTo(iMax) > 0) {
                throw new CalculatorError("minimum (" + iMin + ") greater than maximum (" + iMax + ")");
            }
            
            final BigDecimal originalI = environment.getVariable("i");
            try {
                BigDecimal result = BigDecimal.ZERO;
                for (BigDecimal i = iMin; i.compareTo(iMax) <= 0; i = i.add(BigDecimal.ONE)) {
                    environment.setVariable("i", i);
                    result = result.add(expr.value(environment));
                }
                return result;
            } finally {
                environment.setVariable("i", originalI);
            }
        }
    }
    
    public static class Sin extends CalculatorFunction {
        public Sin() {
            super("sin", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.sin(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Sinh extends CalculatorFunction {
        public Sinh() {
            super("sinh", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.sinh(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Sqrt extends CalculatorFunction {
        public Sqrt() {
            super("sqrt", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.sqrt(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Tan extends CalculatorFunction {
        public Tan() {
            super("tan", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.tan(args.get(0).value(environment).doubleValue()));
        }
    }
    
    public static class Tanh extends CalculatorFunction {
        public Tanh() {
            super("tanh", 1);
        }
        
        public BigDecimal apply(Calculator environment, List<CalculatorAstNode> args) {
            return fromDouble(Math.tanh(args.get(0).value(environment).doubleValue()));
        }
    }
}
