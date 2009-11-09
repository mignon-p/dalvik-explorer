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

/**
 * Represents a built-in function.
 */
public abstract class CalculatorFunction implements Cloneable, Node {
    private final String name;
    private final int arity;
    
    // Only valid in a bound function.
    protected List<Node> args;
    
    public CalculatorFunction(String name, int arity) {
        this.name = name;
        this.arity = arity;
    }
    
    public CalculatorFunction bind(List<Node> args) {
        if (arity != args.size()) {
            String message = "wrong number of arguments to function \"" + name + "\"";
            message += "; need " + arity + " but got " + args.size();
            if (args.size() > 0) {
                message += ": ";
                for (Node arg : args) {
                    message += arg.toInputString();
                }
            }
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
        if (arity > 0) {
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
}
