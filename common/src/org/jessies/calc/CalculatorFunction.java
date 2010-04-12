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

/**
 * Represents a built-in function.
 */
public abstract class CalculatorFunction implements Cloneable, Node {
    private final String name;
    private final int minArity;
    private final int maxArity;
    
    // Only valid in a bound function.
    protected List<Node> args;
    
    // A fixed-arity function.
    public CalculatorFunction(String name, int arity) {
        this(name, arity, arity);
    }
    
    // A variable-arity function.
    public CalculatorFunction(String name, int minArity, int maxArity) {
        this.name = name;
        this.minArity = minArity;
        this.maxArity = maxArity;
    }
    
    // For use in subclass implementations of evaluate.
    protected Node arg(Calculator environment, int i) {
        return args.get(i).evaluate(environment);
    }
    
    public CalculatorFunction bind(Node... args) {
        return bind(Arrays.asList(args));
    }
    
    public CalculatorFunction bind(List<Node> args) {
        if (args.size() < minArity || args.size() > maxArity) {
            String message = "wrong number of arguments to function \"" + name + "\"; need ";
            if (minArity == maxArity) {
                message += "exactly " + minArity;
            } else if (args.size() < minArity) {
                message += "at least " + minArity;
            } else if (args.size() > maxArity) {
                message += "at most " + maxArity;
            }
            message += " but got " + args.size();
            throw new CalculatorError(message);
        }
        
        try {
            CalculatorFunction result = (CalculatorFunction) this.clone();
            result.args = args;
            return result;
        } catch (CloneNotSupportedException ex) {
            throw new CalculatorError("internal error: couldn't clone '" + name + "'");
        }
    }
    
    public Node evaluate(Calculator environment) {
        return apply(environment);
    }
    
    public Node simplify(Calculator environment) {
        return bind(simplifyArgs(environment));
    }
    
    protected ArrayList<Node> simplifyArgs(Calculator environment) {
        ArrayList<Node> simplifiedArgs = new ArrayList<Node>();
        for (int i = 0; i < args.size(); ++i) {
            simplifiedArgs.add(args.get(i).simplify(environment));
        }
        return simplifiedArgs;
    }
    
    public abstract Node apply(Calculator environment);
    
    public String name() {
        return name;
    }
    
    @Override public String toInputString() {
        StringBuilder result = new StringBuilder();
        result.append(name);
        if (args != null && args.size() > 0) {
            result.append("(");
            for (int i = 0; i < args.size(); ++i) {
                Node arg = args.get(i);
                if (i > 0) {
                    result.append(", ");
                }
                result.append(arg.toInputString());
            }
            result.append(")");
        }
        return result.toString();
    }
    
    @Override public String toString() {
        return toInputString();
    }
    
    @Override public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CalculatorFunction)) {
            return false;
        }
        CalculatorFunction rhs = (CalculatorFunction) other;
        if (!name.equals(rhs.name)) {
            return false;
        }
        return (args != null) ? args.equals(rhs.args) : rhs.args == null;
    }
    
    @Override public int hashCode() {
        throw new UnsupportedOperationException("hashing functions/function applications not yet supported");
    }
}
