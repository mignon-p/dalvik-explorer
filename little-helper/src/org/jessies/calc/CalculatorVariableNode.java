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

public class CalculatorVariableNode implements Node {
    private final String name;
    
    public CalculatorVariableNode(String name) {
        this.name = name;
    }
    
    public String name() {
        return name;
    }
    
    public Node evaluate(Calculator environment) {
        final Node value = environment.getVariable(name);
        if (value == null) {
            throw new CalculatorError("use of undefined variable '" + name + "'");
        }
        return value;
    }
    
    public Node simplify(Calculator environment) {
        // If we have a value, use it.
        // FIXME: we should implement "e" and "pi" (and all constants?) differently first.
        final Node value = null; // environment.getVariable(name);
        return (value != null ? value : this);
    }
    
    public String toInputString() {
        return name;
    }
    
    @Override public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CalculatorVariableNode)) {
            return false;
        }
        CalculatorVariableNode rhs = (CalculatorVariableNode) other;
        return name.equals(rhs.name);
    }
    
    @Override public int hashCode() {
        return name.hashCode();
    }
    
    @Override public String toString() {
        return toInputString();
    }
}
