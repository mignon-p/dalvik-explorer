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
 * Superclass of all functions taking two numeric arguments.
 */
public abstract class CalculatorFunctionNN extends CalculatorFunction {
    public CalculatorFunctionNN(String name) {
        super(name, 2);
    }
    
    public Node apply(Calculator environment) {
        NumberNode lhs = toNumber(name(), environment, arg(environment, 0));
        NumberNode rhs = toNumber(name(), environment, arg(environment, 1));
//        Node lhs = arg(environment, 0);
//        Node rhs = arg(environment, 1);
//        if (!(lhs instanceof NumberNode) || !(rhs instanceof NumberNode)) {
//            if (isUnacceptable(lhs) || isUnacceptable(rhs)) {
//                throw new CalculatorError("'" + name() + "' requires two numeric arguments");
//            }
//            return bind(lhs, rhs);
//        }
        return apply(environment, lhs, rhs);
    }
    
//    private boolean isUnacceptable(Node n) {
//        return (n instanceof BooleanNode) || (n instanceof ListNode);
//    }
    
    public abstract Node apply(Calculator environment, NumberNode lhs, NumberNode rhs);
}
