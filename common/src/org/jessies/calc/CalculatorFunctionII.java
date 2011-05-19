package org.jessies.calc;

/*
 * This file is part of org.jessies.calc.
 * Copyright (C) 2010 Elliott Hughes <enh@jessies.org>.
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

/**
 * Superclass of all functions taking two integer arguments.
 */
public abstract class CalculatorFunctionII extends CalculatorFunction {
    public CalculatorFunctionII(String name) {
        super(name, 2);
    }
    
    public Node apply(Calculator environment) {
        Node lhs = arg(environment, 0);
        Node rhs = arg(environment, 1);
        if (!(lhs instanceof IntegerNode) || !(rhs instanceof IntegerNode)) {
            if (isUnacceptable(lhs) || isUnacceptable(rhs)) {
                throw new CalculatorError("'" + name() + "' requires two integer arguments");
            }
            return bind(lhs, rhs);
        }
        return apply(environment, (IntegerNode) lhs, (IntegerNode) rhs);
    }
    
    private boolean isUnacceptable(Node n) {
        return (n instanceof BooleanNode) || (n instanceof ListNode) || (n instanceof NumberNode);
    }
    
    public abstract Node apply(Calculator environment, IntegerNode lhs, IntegerNode rhs);
}
