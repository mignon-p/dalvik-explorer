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
 * Superclass of all functions taking a single boolean argument.
 */
public abstract class CalculatorFunctionZ extends CalculatorFunction {
    public CalculatorFunctionZ(String name) {
        super(name, 1);
    }
    
    public Node apply(Calculator environment) {
        Node arg = arg(environment, 0);
        if (!(arg instanceof BooleanNode)) {
            if (isUnacceptable(arg)) {
                throw new CalculatorError("'" + name() + "' requires one boolean argument");
            }
            return bind(arg);
        }
        return apply(environment, (BooleanNode) arg);
    }
    
    private boolean isUnacceptable(Node n) {
        return (n instanceof ListNode) || (n instanceof NumberNode);
    }
    
    public abstract Node apply(Calculator environment, BooleanNode arg);
}
