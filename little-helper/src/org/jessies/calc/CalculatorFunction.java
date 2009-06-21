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

/**
 * Represents a built-in function.
 */
public abstract class CalculatorFunction {
    private final String name;
    private final int arity;
    
    public CalculatorFunction(String name, int arity) {
        this.name = name;
        this.arity = arity;
    }
    
    public int arity() {
        return arity;
    }
    
    public abstract BigDecimal apply(Calculator environment, List<CalculatorAstNode> args);
    
    @Override public String toString() {
        return name;
    }
}
