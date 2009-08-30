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
 * Represents a function application.
 */
public class CalculatorFunctionApplicationNode implements Node {
    private final CalculatorFunction function;
    private final List<Node> args;
    
    public CalculatorFunctionApplicationNode(CalculatorFunction function, List<Node> args) {
        this.function = function;
        this.args = args;
        if (function.arity() != args.size()) {
            throw new CalculatorError("wrong number of arguments to function \"" + function + "\"; need " + function.arity() + " but got " + args.size());
        }
    }
    
    public BigDecimal value(Calculator environment) {
        return function.apply(environment, args);
    }
}
