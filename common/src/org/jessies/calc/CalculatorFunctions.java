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

public class CalculatorFunctions {
    private CalculatorFunctions() {}
    
    private static boolean isNumber(Node n) {
        return (n instanceof NumberNode);
    }
    
    private static boolean isZero(Node n) {
        return (n instanceof IntegerNode) && (((IntegerNode) n).compareTo(IntegerNode.ZERO) == 0);
    }
    
    private static boolean isOne(Node n) {
        return (n instanceof IntegerNode) && (((IntegerNode) n).compareTo(IntegerNode.ONE) == 0);
    }
    
    private static int cmp(Calculator environment, List<Node> args) {
        final NumberNode lhs = toNumber("cmp", environment, args.get(0));
        final NumberNode rhs = toNumber("cmp", environment, args.get(1));
        return cmp(lhs, rhs);
    }
    
    private static int cmp(NumberNode lhs, NumberNode rhs) {
        if (lhs instanceof IntegerNode && rhs instanceof IntegerNode) {
            return ((IntegerNode) lhs).compareTo((IntegerNode) rhs);
        } else {
            // FIXME: what about an integer too large to be represented as a real, compared to a real?
            return lhs.toReal().compareTo(rhs.toReal());
        }
    }
    
    private static BooleanNode equal(Node lhs, Node rhs) {
        if (lhs instanceof BooleanNode && rhs instanceof BooleanNode) {
            return BooleanNode.valueOf(lhs == rhs);
        } else if (lhs instanceof NumberNode && rhs instanceof NumberNode) {
            return BooleanNode.valueOf(cmp((NumberNode) lhs, (NumberNode) rhs) == 0);
        } else if (lhs instanceof ListNode && rhs instanceof ListNode) {
            return equalLists((ListNode) lhs, (ListNode) rhs);
        } else {
            return BooleanNode.FALSE;
        }
    }
    
    private static BooleanNode equalLists(ListNode lhs, ListNode rhs) {
        if (lhs.size() != rhs.size()) {
            return BooleanNode.FALSE;
        }
        for (int i = 0, size = lhs.size(); i < size; ++i) {
            if (equal(lhs.get(i), rhs.get(i)) == BooleanNode.FALSE) {
                return BooleanNode.FALSE;
            }
        }
        return BooleanNode.TRUE;
    }
    
    private static BooleanNode unequal(Node lhs, Node rhs) {
        return equal(lhs, rhs).not();
    }
    
    private static BooleanNode toBoolean(String function, Calculator environment, Node node) {
        node = node.evaluate(environment);
        if (node instanceof BooleanNode) {
            return (BooleanNode) node;
        }
        throw expected(function, "boolean");
    }
    
    private static ListNode toList(String function, Calculator environment, Node node) {
        node = node.evaluate(environment);
        if (node instanceof ListNode) {
            return (ListNode) node;
        }
        throw expected(function, "list");
    }
    
    private static NumberNode toNumber(String function, Calculator environment, Node node) {
        node = node.evaluate(environment);
        if (node instanceof NumberNode) {
            return (NumberNode) node;
        }
        throw expected(function, "numeric");
    }
    
    private static IntegerNode toInteger(String function, Calculator environment, Node node) {
        node = node.evaluate(environment);
        if (node instanceof IntegerNode) {
            return (IntegerNode) node;
        }
        throw expected(function, "integer");
    }
    
    private static RealNode toReal(String function, Calculator environment, Node node) {
        node = node.evaluate(environment);
        if (node instanceof RealNode) {
            return (RealNode) node;
        } else if (node instanceof IntegerNode) {
            return ((IntegerNode) node).toReal();
        }
        throw expected(function, "real");
    }
    
    private static CalculatorError expected(String function, String type) {
        throw new CalculatorError("'" + function + "' expected " + type + " argument");
    }
    
    // Returns [rowCount, columnCount], or null if 'node' is not a matrix.
    private static ListNode matrixDimensions(Node node) {
        if (!(node instanceof ListNode)) {
            return null;
        }
        // 2. each element is a list...
        final ListNode list = (ListNode) node;
        int columnCount = 0;
        for (Node element : list) {
            if (!(element instanceof ListNode)) {
                return null;
            }
            // 3. of the same length...
            ListNode row = (ListNode) element;
            int rowLength = row.size();
            if (columnCount != 0 && columnCount != rowLength) {
                return null;
            }
            columnCount = rowLength;
            // 4. where none of the elements itself contains a list.
            for (Node rowElement : row) {
                if (rowElement instanceof ListNode) {
                    return null;
                }
            }
        }
        int rowCount = list.size();
        return new ListNode().add(IntegerNode.valueOf(rowCount)).add(IntegerNode.valueOf(columnCount));
    }
    
    // Returns the 'j'th element of row 'i' of 'm'.
    // Uses zero-based indexes, for internal use only.
    private static Node matrixElementAt(ListNode m, IntegerNode i, IntegerNode j) {
        ListNode row = (ListNode) m.get(i);
        Node element = row.get(j);
        return element;
    }
    
    private enum MatrixOp {
        ADD("addition"),
        MUL("multiplication"),
        SUB("subtraction"),
        RSB("subtraction");
        private final String name;
        private MatrixOp(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
    
    private static Node matrixMatrixOp(ListNode lhs, ListNode lhsDimensions, ListNode rhs, ListNode rhsDimensions, MatrixOp op) {
        if (equal(lhsDimensions, rhsDimensions) == BooleanNode.FALSE) {
            throw new CalculatorError("matrix " + op + " requires compatible matrices");
        }
        final IntegerNode rowCount = (IntegerNode) lhsDimensions.get(0);
        final IntegerNode columnCount = (IntegerNode) rhsDimensions.get(1);
        final ListNode result = new ListNode();
        for (IntegerNode i = IntegerNode.ZERO; cmp(i, rowCount) < 0; i = i.increment()) {
            final ListNode newRow = new ListNode();
            for (IntegerNode j = IntegerNode.ZERO; cmp(j, columnCount) < 0; j = j.increment()) {
                Node lhsNode = matrixElementAt(lhs, i, j);
                Node rhsNode = matrixElementAt(rhs, i, j);
                if (!isNumber(lhsNode) || !isNumber(rhsNode)) {
                    throw new CalculatorError(op + " of a matrix and a scalar requires numeric matrices");
                }
                NumberNode lhsNumber = (NumberNode) lhsNode;
                NumberNode rhsNumber = (NumberNode) rhsNode;
                switch (op) {
                case ADD: newRow.add(lhsNumber.plus(rhsNumber)); break;
                case SUB: newRow.add(lhsNumber.subtract(rhsNumber)); break;
                default: throw new CalculatorError("elementwise matrix multiplication/reverse-subtract not implemented");
                }
            }
            result.add(newRow);
        }
        return result;
    }
    
    private static Node matrixScalarOp(ListNode matrix, ListNode dimensions, NumberNode scalar, MatrixOp op) {
        final ListNode result = new ListNode();
        for (Node row : matrix) {
            final ListNode newRow = new ListNode();
            for (Node value : (ListNode) row) {
                if (!isNumber(value)) {
                    throw new CalculatorError(op + " of a matrix and a scalar requires numeric matrices");
                }
                NumberNode element = (NumberNode) value;
                switch (op) {
                case ADD: newRow.add(element.plus(scalar)); break;
                case MUL: newRow.add(element.times(scalar)); break;
                case RSB: newRow.add(scalar.subtract(element)); break;
                case SUB: newRow.add(element.subtract(scalar)); break;
                }
            }
            result.add(newRow);
        }
        return result;
    }
    
    public static class Abs extends CalculatorFunction {
        public Abs() {
            super("Abs", 1);
        }
        
        public Node apply(Calculator environment) {
            return toNumber("Abs", environment, args.get(0)).abs();
        }
    }
    
    public static class Acos extends CalculatorFunction {
        public Acos() {
            super("acos", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("acos", environment, args.get(0)).acos();
        }
    }
    
    public static class And extends CalculatorFunction {
        public And() {
            super("And", 2);
        }
        
        public Node apply(Calculator environment) {
            return toBoolean("And", environment, args.get(0)).and(toBoolean("And", environment, args.get(1)));
        }
    }
    
    public static class Asin extends CalculatorFunction {
        public Asin() {
            super("asin", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("asin", environment, args.get(0)).asin();
        }
    }
    
    public static class Atan extends CalculatorFunction {
        public Atan() {
            super("atan", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("atan", environment, args.get(0)).atan();
        }
    }
    
    public static class Atan2 extends CalculatorFunction {
        public Atan2() {
            super("atan2", 2);
        }
        
        public Node apply(Calculator environment) {
            return toReal("atan2", environment, args.get(0)).atan2(toReal("atan2", environment, args.get(1)));
        }
    }
    
    public static class BitAnd extends CalculatorFunction {
        public BitAnd() {
            super("BitAnd", 2);
        }
        
        public Node apply(Calculator environment) {
            final IntegerNode lhs = toInteger("BitAnd", environment, args.get(0));
            final IntegerNode rhs = toInteger("BitAnd", environment, args.get(1));
            return lhs.bitAnd(rhs);
        }
    }
    
    public static class BitNot extends CalculatorFunction {
        public BitNot() {
            super("BitNot", 1);
        }
        
        public Node apply(Calculator environment) {
            final IntegerNode lhs = toInteger("BitNot", environment, args.get(0));
            return lhs.bitNot();
        }
    }
    
    public static class BitOr extends CalculatorFunction {
        public BitOr() {
            super("BitOr", 2);
        }
        
        public Node apply(Calculator environment) {
            final IntegerNode lhs = toInteger("BitOr", environment, args.get(0));
            final IntegerNode rhs = toInteger("BitOr", environment, args.get(1));
            return lhs.bitOr(rhs);
        }
    }
    
    public static class BitShiftLeft extends CalculatorFunction {
        public BitShiftLeft() {
            super("BitShiftLeft", 2);
        }
        
        public Node apply(Calculator environment) {
            final IntegerNode lhs = toInteger("BitShiftLeft", environment, args.get(0));
            final IntegerNode rhs = toInteger("BitShiftLeft", environment, args.get(1));
            return lhs.bitShiftLeft(rhs);
        }
    }
    
    public static class BitShiftRight extends CalculatorFunction {
        public BitShiftRight() {
            super("BitShiftRight", 2);
        }
        
        public Node apply(Calculator environment) {
            final IntegerNode lhs = toInteger("BitShiftRight", environment, args.get(0));
            final IntegerNode rhs = toInteger("BitShiftRight", environment, args.get(1));
            return lhs.bitShiftRight(rhs);
        }
    }
    
    public static class BitXor extends CalculatorFunction {
        public BitXor() {
            super("BitXor", 2);
        }
        
        public Node apply(Calculator environment) {
            final IntegerNode lhs = toInteger("BitXor", environment, args.get(0));
            final IntegerNode rhs = toInteger("BitXot", environment, args.get(1));
            return lhs.bitXor(rhs);
        }
    }
    
    public static class Cbrt extends CalculatorFunction {
        public Cbrt() {
            super("cbrt", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("cbrt", environment, args.get(0)).cbrt();
        }
    }
    
    public static class Ceiling extends CalculatorFunction {
        public Ceiling() {
            super("ceiling", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("ceiling", environment, args.get(0)).ceiling();
        }
    }
    
    public static class Cos extends CalculatorFunction {
        public Cos() {
            super("cos", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("cos", environment, args.get(0)).cos();
        }
    }
    
    public static class Cosh extends CalculatorFunction {
        public Cosh() {
            super("cosh", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("cosh", environment, args.get(0)).cosh();
        }
    }
    
    public static class Define extends CalculatorFunction {
        public Define() {
            super("define", 2);
        }
        
        public Node apply(Calculator environment) {
            final Node lhs = args.get(0);
            final Node rhs = args.get(1);
            if (!(lhs instanceof CalculatorVariableNode)) {
                throw new CalculatorError("lhs of an assignment must be a variable name (user-defined functions not yet implemented)");
            }
            final CalculatorVariableNode variable = (CalculatorVariableNode) lhs;
            final Node value = rhs.evaluate(environment);
            environment.setVariable(variable.name(), value);
            return value;
        }
    }
    
    public static class Dimensions extends CalculatorFunction {
        public Dimensions() {
            super("Dimensions", 1);
        }
        
        public Node apply(Calculator environment) {
            final ListNode dimensions = matrixDimensions(args.get(0).evaluate(environment));
            if (dimensions == null) {
                throw expected("Dimensions", "matrix");
            }
            return dimensions;
        }
    }
    
    public static class Divide extends CalculatorFunction {
        public Divide() {
            super("Divide", 2);
        }
        
        public Node apply(Calculator environment) {
            final NumberNode lhs = toNumber("Divide", environment, args.get(0));
            final NumberNode rhs = toNumber("Divide", environment, args.get(1));
            return lhs.divide(rhs);
        }
    }
    
    public static class Equal extends CalculatorFunction {
        public Equal() {
            super("Equal", 2);
        }
        
        public Node apply(Calculator environment) {
            return equal(args.get(0).evaluate(environment), args.get(1).evaluate(environment));
        }
    }
    
    public static class Exp extends CalculatorFunction {
        public Exp() {
            super("exp", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("exp", environment, args.get(0)).exp();
        }
    }
    
    public static class Factorial extends CalculatorFunction {
        public Factorial() {
            super("Factorial", 1);
        }
        
        public Node apply(Calculator environment) {
            return toInteger("Factorial", environment, args.get(0)).factorial();
        }
    }
    
    public static class Floor extends CalculatorFunction {
        public Floor() {
            super("floor", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("floor", environment, args.get(0)).floor();
        }
    }
    
    public static class FractionalPart extends CalculatorFunction {
        public FractionalPart() {
            super("FractionalPart", 1);
        }
        
        public Node apply(Calculator environment) {
            return toNumber("FractionalPart", environment, args.get(0)).fractionalPart();
        }
    }
    
    public static class Greater extends CalculatorFunction {
        public Greater() {
            super("Greater", 2);
        }
        
        public Node apply(Calculator environment) {
            return BooleanNode.valueOf(cmp(environment, args) > 0);
        }
    }
    
    public static class GreaterEqual extends CalculatorFunction {
        public GreaterEqual() {
            super("GreaterEqual", 2);
        }
        
        public Node apply(Calculator environment) {
            return BooleanNode.valueOf(cmp(environment, args) >= 0);
        }
    }
    
    public static class Hypot extends CalculatorFunction {
        public Hypot() {
            super("hypot", 2);
        }
        
        public Node apply(Calculator environment) {
            return toReal("hypot", environment, args.get(0)).hypot(toReal("hypot", environment, args.get(1)));
        }
    }
    
    public static class IdentityMatrix extends CalculatorFunction {
        public IdentityMatrix() {
            super("IdentityMatrix", 1);
        }
        
        public Node apply(Calculator environment) {
            final IntegerNode dimension = toInteger("IdentityMatrix", environment, args.get(0));
            final ListNode result = new ListNode();
            for (NumberNode i = IntegerNode.ONE; cmp(i, dimension) <= 0; i = i.increment()) {
                final ListNode row = new ListNode();
                for (NumberNode j = IntegerNode.ONE; cmp(j, dimension) <= 0; j = j.increment()) {
                    row.add(cmp(j, i) == 0 ? IntegerNode.ONE : IntegerNode.ZERO);
                }
                result.add(row);
            }
            return result;
        }
    }
    
    // Returns the number of decimal digits in the given integer.
    public static class IntegerLength extends CalculatorFunction {
        public IntegerLength() {
            super("IntegerLength", 1, 2);
        }
        
        public Node apply(Calculator environment) {
            final IntegerNode n = toInteger("IntegerLength", environment, args.get(0));
            int radix = 10;
            if (args.size() == 2) {
                final IntegerNode b = toInteger("IntegerLength", environment, args.get(1));
                if (b.compareTo(IntegerNode.valueOf(2)) < 0 || b.compareTo(IntegerNode.valueOf(36)) > 0) {
                    throw new CalculatorError("radix must be between 2 and 36");
                }
                radix = b.intValue();
            }
            return IntegerNode.valueOf(n.abs().toString(radix).length());
        }
    }
    
    public static class IntegerPart extends CalculatorFunction {
        public IntegerPart() {
            super("IntegerPart", 1);
        }
        
        public Node apply(Calculator environment) {
            return toNumber("IntegerPart", environment, args.get(0)).integerPart();
        }
    }
    
    public static class IsPrime extends CalculatorFunction {
        public IsPrime() {
            super("IsPrime", 1);
        }
        
        public Node apply(Calculator environment) {
            return toInteger("IsPrime", environment, args.get(0)).isPrime();
        }
    }
    
    public static class IsMatrix extends CalculatorFunction {
        public IsMatrix() {
            super("IsMatrix", 1);
        }
        
        public Node apply(Calculator environment) {
            // A node is a matrix if:
            // 1. it's a list..
            final Node node = args.get(0).evaluate(environment);
            return BooleanNode.valueOf(matrixDimensions(node) != null);
        }
    }
    
    public static class Length extends CalculatorFunction {
        public Length() {
            super("Length", 1);
        }
        
        public Node apply(Calculator environment) {
            Node node = args.get(0).evaluate(environment);
            if (!(node instanceof ListNode)) {
                return IntegerNode.ZERO;
            }
            ListNode list = (ListNode) node;
            return IntegerNode.valueOf(list.size());
        }
    }
    
    public static class Less extends CalculatorFunction {
        public Less() {
            super("Less", 2);
        }
        
        public Node apply(Calculator environment) {
            return BooleanNode.valueOf(cmp(environment, args) < 0);
        }
    }
    
    public static class LessEqual extends CalculatorFunction {
        public LessEqual() {
            super("LessEqual", 2);
        }
        
        public Node apply(Calculator environment) {
            return BooleanNode.valueOf(cmp(environment, args) <= 0);
        }
    }
    
    // We call this ListBuilder because reusing the name List in Java is just too confusing.
    public static class ListBuilder extends CalculatorFunction {
        public ListBuilder() {
            super("List", 0, Integer.MAX_VALUE);
        }
        
        public Node apply(Calculator environment) {
            final ListNode result = new ListNode();
            for (Node arg : args) {
                result.add(arg.evaluate(environment));
            }
            return result;
        }
    }
    
    // log(base, n).
    public static class Log extends CalculatorFunction {
        public Log() {
            super("log", 2);
        }
        
        public Node apply(Calculator environment) {
            final RealNode base = toReal("log2", environment, args.get(0));
            final RealNode n = toReal("log2", environment, args.get(1));
            return n.log(base);
        }
    }
    
    public static class Log2 extends CalculatorFunction {
        public Log2() {
            super("log2", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("log2", environment, args.get(0)).log2();
        }
    }
    
    public static class LogE extends CalculatorFunction {
        public LogE() {
            super("logE", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("logE", environment, args.get(0)).logE();
        }
    }
    
    public static class Log10 extends CalculatorFunction {
        public Log10() {
            super("log10", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("log10", environment, args.get(0)).log10();
        }
    }
    
    public static class Max extends CalculatorFunction {
        public Max() {
            super("Max", 2);
        }
        
        public Node apply(Calculator environment) {
            final NumberNode lhs = toNumber("Max", environment, args.get(0));
            final NumberNode rhs = toNumber("Max", environment, args.get(1));
            return cmp(lhs, rhs) >= 0 ? lhs : rhs;
        }
    }
    
    public static class Min extends CalculatorFunction {
        public Min() {
            super("Min", 2);
        }
        
        public Node apply(Calculator environment) {
            final NumberNode lhs = toNumber("Min", environment, args.get(0));
            final NumberNode rhs = toNumber("Min", environment, args.get(1));
            return cmp(lhs, rhs) < 0 ? lhs : rhs;
        }
    }
    
    public static class Mod extends CalculatorFunction {
        public Mod() {
            super("Mod", 2);
        }
        
        public Node apply(Calculator environment) {
            final IntegerNode lhs = toInteger("Mod", environment, args.get(0));
            final IntegerNode rhs = toInteger("Mod", environment, args.get(1));
            return lhs.mod(rhs);
        }
    }
    
    public static class Not extends CalculatorFunction {
        public Not() {
            super("Not", 1);
        }
        
        public Node apply(Calculator environment) {
            return toBoolean("Not", environment, args.get(0)).not();
        }
    }
    
    public static class Or extends CalculatorFunction {
        public Or() {
            super("Or", 2);
        }
        
        public Node apply(Calculator environment) {
            return toBoolean("Or", environment, args.get(0)).or(toBoolean("Or", environment, args.get(1)));
        }
    }
    
    public static class Plot extends CalculatorFunction {
        public Plot() {
            super("Plot", 4);
        }
        
        public Node apply(Calculator environment) {
            CalculatorPlotter plotter = environment.getPlotter();
            if (plotter == null) {
                throw new CalculatorError("this system is not capable of plotting");
            }
            
            // 0: expression
            // 1: variable
            // 2: xMin
            // 3: xMax
            // Example: plot(sin(x), x, 0, 2*pi)
            // FIXME: automatic guesses? (trig => pi? differentiate to find minima? solve to find axis crossings?).
            
            final Node expression = args.get(0);
            if (args.get(1) instanceof CalculatorVariableNode == false) {
                throw new CalculatorError("expected a variable as the second argument to Plot");
            }
            final CalculatorVariableNode variable = (CalculatorVariableNode) args.get(1);
            final RealNode xMin = toReal("Plot", environment, args.get(2));
            final RealNode xMax = toReal("Plot", environment, args.get(3));
            
            final int pixelWidth = 300;
            CalculatorPlotData plotData = new CalculatorPlotData(pixelWidth, xMin, xMax);
            
            try {
                Node free = variable.evaluate(environment);
                throw new CalculatorError("variable '" + variable.name() + "' is not free");
            } catch (CalculatorError ex) {
                // That's what we hoped...
            }
            final String variableName = variable.name();
            try {
                double x = plotData.xMin;
                double xStep = plotData.xRange / pixelWidth;
                for (int i = 0; i < pixelWidth; ++i) {
                    try {
                        environment.setVariable(variableName, new RealNode(x));
                        double y = Double.parseDouble(expression.evaluate(environment).toString());
                        if (Double.isInfinite(y)) {
                            y = Double.NaN; // Infinity confuses the range calculations.
                        }
                        plotData.data[i] = y;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        plotData.data[i] = Double.NaN;
                    }
                    x += xStep;
                }
            } finally {
                environment.setVariable(variableName, null);
            }
            
            plotData.calculateRange();
            plotter.showPlot(plotData);
            
            return BooleanNode.TRUE; // FIXME: "void"?
        }
    }
    
    public static class Plus extends CalculatorFunction {
        public Plus() {
            super("Plus", 2);
        }
        
        public Node apply(Calculator environment) {
            final Node lhs = args.get(0).evaluate(environment);
            final Node rhs = args.get(1).evaluate(environment);
            if (isNumber(lhs) && isNumber(rhs)) {
                // scalar + scalar
                return ((NumberNode) lhs).plus((NumberNode) rhs);
            }
            final ListNode lhsDimensions = matrixDimensions(lhs);
            final ListNode rhsDimensions = matrixDimensions(rhs);
            if (lhsDimensions != null && rhsDimensions != null) {
                // matrix + matrix
                return matrixMatrixOp((ListNode) lhs, lhsDimensions, (ListNode) rhs, rhsDimensions, MatrixOp.ADD);
            } else if (isNumber(lhs) && rhsDimensions != null) {
                // scalar + matrix
                return matrixScalarOp((ListNode) rhs, rhsDimensions, (NumberNode) lhs, MatrixOp.ADD);
            } else if (lhsDimensions != null && isNumber(rhs)) {
                // matrix + scalar
                return matrixScalarOp((ListNode) lhs, lhsDimensions, (NumberNode) rhs, MatrixOp.ADD);
            } else {
                throw expected("Plus", "numeric or matrix");
            }
        }
        
        @Override public Node simplify(Calculator environment) {
            final ArrayList<Node> args = simplifyArgs(environment);
            NumberNode total = IntegerNode.ZERO;
            for (int i = args.size() - 1; i >= 0; --i) {
                final Node arg = args.get(i);
                if (isNumber(arg)) {
                    if (!isZero(arg)) {
                        total = total.plus((NumberNode) arg);
                    }
                    args.remove(i);
                }
            }
            if (args.size() == 0 || !isZero(total)) {
                // Sort numeric terms last in sums (preferring "x + 1" rather than "1 + x").
                args.add(total);
            }
            if (args.size() == 1) {
                return args.get(0);
            }
            return bind(args);
        }
    }
    
    public static class Power extends CalculatorFunction {
        public Power() {
            super("power", 2);
        }
        
        public Node apply(Calculator environment) {
            final NumberNode lhs = toNumber("power", environment, args.get(0));
            final NumberNode rhs = toNumber("power", environment, args.get(1));
            return lhs.power(rhs);
        }
    }
    
    public static class Product extends CalculatorFunction {
        public Product() {
            super("product", 3);
        }
        
        public Node apply(Calculator environment) {
            return series(environment, args, IntegerNode.ONE, false);
        }
    }
    
    public static class Random extends CalculatorFunction {
        public Random() {
            super("random", 0);
        }
        
        public Node apply(Calculator environment) {
            return new RealNode(Math.random());
        }
    }
    
    public static class Range extends CalculatorFunction {
        public Range() {
            super("Range", 1, 3);
        }
        
        public Node apply(Calculator environment) {
            final NumberNode start;
            final NumberNode end;
            final NumberNode step;
            if (args.size() == 1) {
                // given n: 1, 2, ..., n
                start = IntegerNode.ONE;
                end = toNumber("range", environment, args.get(0));
                step = IntegerNode.ONE;
            } else if (args.size() == 2) {
                // given n, m: n, n+1, ..., m
                start = toNumber("range", environment, args.get(0));
                end = toNumber("range", environment, args.get(1));
                step = IntegerNode.ONE;
            } else {
                // given n, m, k: n, n+k, ..., m
                start = toNumber("range", environment, args.get(0));
                end = toNumber("range", environment, args.get(1));
                step = toNumber("range", environment, args.get(2));
            }
            return makeRange(start, end, step);
        }
        
        private static ListNode makeRange(NumberNode start, NumberNode end, NumberNode step) {
            // TODO: factor out iteration (and collection of iteration parameters) so we can have consistent behavior between functions.
            IntegerNode stepSign = step.sign();
            if (stepSign.equals(IntegerNode.ZERO)) {
                throw new CalculatorError("need a non-zero step size");
            }
            
            final ListNode result = new ListNode();
            if (cmp(stepSign, IntegerNode.ZERO) > 0) {
                for (NumberNode i = start; cmp(i, end) <= 0; i = i.plus(step)) {
                    result.add(i);
                }
            } else {
                for (NumberNode i = start; cmp(i, end) >= 0; i = i.plus(step)) {
                    result.add(i);
                }
            }
            return result;
        }
    }
    
    public static class Reverse extends CalculatorFunction {
        public Reverse() {
            super("Reverse", 1);
        }
        
        public Node apply(Calculator environment) {
            return toList("Reverse", environment, args.get(0)).reverse();
        }
    }
    
    public static class Round extends CalculatorFunction {
        public Round() {
            super("round", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("round", environment, args.get(0)).round();
        }
    }
    
    private static NumberNode series(Calculator environment, List<Node> args, IntegerNode initial, boolean isSum) {
        NumberNode iMin = toNumber("series", environment, args.get(0));
        NumberNode iMax = toNumber("series", environment, args.get(1));
        final Node expr = args.get(2);
        
        // Ensure we have two integers or two reals.
        if (iMin instanceof RealNode || iMax instanceof RealNode) {
            iMin = iMin.toReal();
            iMax = iMax.toReal();
        }
        
        if (cmp(iMin, iMax) > 0) {
            throw new CalculatorError("minimum (" + iMin + ") greater than maximum (" + iMax + ")");
        }
        
        // FIXME: support infinite sums/products, adding convergence testing.
        
        // FIXME: let the user specify the sum variable.
        final Node originalI = environment.getVariable("i");
        try {
            NumberNode result = initial;
            for (NumberNode i = iMin; cmp(i, iMax) <= 0; i = i.increment()) {
                environment.setVariable("i", i);
                // FIXME: handle undefined and non-numeric terms.
                final NumberNode term = (NumberNode) expr.evaluate(environment);
                if (isSum) {
                    result = result.plus(term);
                } else {
                    result = result.times(term);
                }
            }
            return result;
        } finally {
            environment.setVariable("i", originalI);
        }
    }
    
    public static class Sign extends CalculatorFunction {
        public Sign() {
            super("Sign", 1);
        }
        
        public Node apply(Calculator environment) {
            return toNumber("Sign", environment, args.get(0)).sign();
        }
    }
    
    public static class Sin extends CalculatorFunction {
        public Sin() {
            super("sin", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("sin", environment, args.get(0)).sin();
        }
    }
    
    public static class Sinh extends CalculatorFunction {
        public Sinh() {
            super("sinh", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("sinh", environment, args.get(0)).sinh();
        }
    }
    
    public static class Sqrt extends CalculatorFunction {
        public Sqrt() {
            super("sqrt", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("sqrt", environment, args.get(0)).sqrt();
        }
    }
    
    public static class Subtract extends CalculatorFunction {
        public Subtract() {
            super("Subtract", 2);
        }
        
        public Node apply(Calculator environment) {
            final Node lhs = args.get(0).evaluate(environment);
            final Node rhs = args.get(1).evaluate(environment);
            if (isNumber(lhs) && isNumber(rhs)) {
                // scalar - scalar
                return ((NumberNode) lhs).subtract((NumberNode) rhs);
            }
            final ListNode lhsDimensions = matrixDimensions(lhs);
            final ListNode rhsDimensions = matrixDimensions(rhs);
            if (lhsDimensions != null && rhsDimensions != null) {
                // matrix - matrix
                return matrixMatrixOp((ListNode) lhs, lhsDimensions, (ListNode) rhs, rhsDimensions, MatrixOp.SUB);
            } else if (isNumber(lhs) && rhsDimensions != null) {
                // scalar - matrix
                return matrixScalarOp((ListNode) rhs, rhsDimensions, (NumberNode) lhs, MatrixOp.RSB);
            } else if (lhsDimensions != null && isNumber(rhs)) {
                // matrix - scalar
                return matrixScalarOp((ListNode) lhs, lhsDimensions, (NumberNode) rhs, MatrixOp.SUB);
            } else {
                throw expected("Subtract", "numeric or matrix");
            }
        }
    }
    
    public static class Sum extends CalculatorFunction {
        public Sum() {
            super("sum", 3);
        }
        
        public Node apply(Calculator environment) {
            return series(environment, args, IntegerNode.ZERO, true);
        }
    }
    
    public static class Tan extends CalculatorFunction {
        public Tan() {
            super("tan", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("tan", environment, args.get(0)).tan();
        }
    }
    
    public static class Tanh extends CalculatorFunction {
        public Tanh() {
            super("tanh", 1);
        }
        
        public Node apply(Calculator environment) {
            return toReal("tanh", environment, args.get(0)).tanh();
        }
    }
    
    public static class Times extends CalculatorFunction {
        public Times() {
            super("Times", 2);
        }
        
        public Node apply(Calculator environment) {
            final Node lhs = args.get(0).evaluate(environment);
            final Node rhs = args.get(1).evaluate(environment);
            if (isNumber(lhs) && isNumber(rhs)) {
                // scalar * scalar
                return ((NumberNode) lhs).times((NumberNode) rhs);
            }
            final ListNode lhsDimensions = matrixDimensions(lhs);
            final ListNode rhsDimensions = matrixDimensions(rhs);
            if (lhsDimensions != null && rhsDimensions != null) {
                // matrix * matrix
                return matrixTimes((ListNode) lhs, lhsDimensions, (ListNode) rhs, rhsDimensions);
            } else if (isNumber(lhs) && rhsDimensions != null) {
                // scalar * matrix
                return matrixScalarOp((ListNode) rhs, rhsDimensions, (NumberNode) lhs, MatrixOp.MUL);
            } else if (lhsDimensions != null && isNumber(rhs)) {
                // matrix * scalar
                return matrixScalarOp((ListNode) lhs, lhsDimensions, (NumberNode) rhs, MatrixOp.MUL);
            } else {
                throw expected("Times", "numeric or matrix");
            }
        }
        
        // Multiply an m*n matrix by an n*p matrix.
        // http://en.wikipedia.org/wiki/Matrix_multiplication
        private static Node matrixTimes(ListNode lhs, ListNode lhsDimensions, ListNode rhs, ListNode rhsDimensions) {
            final IntegerNode n = (IntegerNode) lhsDimensions.get(1);
            if (!n.equals((IntegerNode) rhsDimensions.get(0))) {
                throw new CalculatorError("matrix multiplication requires compatible matrices");
            }
            final IntegerNode rowCount = (IntegerNode) lhsDimensions.get(0);
            final IntegerNode columnCount = (IntegerNode) rhsDimensions.get(1);
            final ListNode result = new ListNode();
            for (IntegerNode i = IntegerNode.ZERO; cmp(i, rowCount) < 0; i = i.increment()) {
                final ListNode newRow = new ListNode();
                for (IntegerNode j = IntegerNode.ZERO; cmp(j, columnCount) < 0; j = j.increment()) {
                    newRow.add(dotProduct(lhs, rhs, i, j, n));
                }
                result.add(newRow);
            }
            return result;
        }
        
        // Returns the sum from r=0..n-1 of A(i,r)*B(r,j).
        // (It's 0..n-1 instead of 1..n because we use zero-based indexes internally.)
        // http://en.wikipedia.org/wiki/Dot_product
        private static Node dotProduct(ListNode A, ListNode B, IntegerNode i, IntegerNode j, IntegerNode n) {
            NumberNode result = IntegerNode.ZERO;
            for (IntegerNode r = IntegerNode.ZERO; cmp(r, n) < 0; r = r.increment()) {
                final NumberNode Air = (NumberNode) matrixElementAt(A, i, r);
                final NumberNode Brj = (NumberNode) matrixElementAt(B, r, j);
                result = result.plus(Air.times(Brj));
            }
            return result;
        }
        
        @Override public Node simplify(Calculator environment) {
            final ArrayList<Node> args = simplifyArgs(environment);
            NumberNode total = IntegerNode.ONE;
            for (int i = args.size() - 1; i >= 0; --i) {
                final Node arg = args.get(i);
                if (isNumber(arg)) {
                    if (!isOne(arg)) {
                        total = total.times((NumberNode) arg);
                    }
                    args.remove(i);
                }
            }
            if (isZero(total)) {
                return total;
            }
            if (args.size() == 0 || !isOne(total)) {
                // Sort numeric factors first in products (preferring "2*x" rather than "x*2")..
                args.add(0, total);
            }
            if (args.size() == 1) {
                return args.get(0);
            }
            return bind(args);
        }
    }
    
    public static class Transpose extends CalculatorFunction {
        public Transpose() {
            super("Transpose", 1);
        }
        
        public Node apply(Calculator environment) {
            final Node node = args.get(0).evaluate(environment);
            final ListNode dimensions = matrixDimensions(node);
            if (dimensions == null) {
                throw expected("Dimensions", "matrix");
            }
            final ListNode m = (ListNode) node;
            final IntegerNode rowCount = (IntegerNode) dimensions.get(1); // new row count == old column count.
            final IntegerNode columnCount = (IntegerNode) dimensions.get(0); // new column count == old row count.
            final ListNode result = new ListNode();
            for (IntegerNode i = IntegerNode.ZERO; cmp(i, rowCount) < 0; i = i.increment()) {
                final ListNode newRow = new ListNode();
                for (IntegerNode j = IntegerNode.ZERO; cmp(j, columnCount) < 0; j = j.increment()) {
                    newRow.add(matrixElementAt(m, j, i));
                }
                result.add(newRow);
            }
            return result;
        }
    }
    
    public static class Unequal extends CalculatorFunction {
        public Unequal() {
            super("Unequal", 2);
        }
        
        public Node apply(Calculator environment) {
            return unequal(args.get(0).evaluate(environment), args.get(1).evaluate(environment));
        }
    }
}
