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

import e.util.*;
import java.math.*;
import java.util.*;

/**
 * Represents a function application.
 */
public class CalculatorFunctionApplicationNode implements CalculatorAstNode {
    private final CalculatorFunction function;
    private final List<CalculatorAstNode> args;
    
    public CalculatorFunctionApplicationNode(CalculatorFunction function, List<CalculatorAstNode> args) {
        this.function = function;
        this.args = args;
        if (function.arity() != args.size()) {
            throw new CalculatorError("function \"" + function + "\" requires " + StringUtilities.pluralize(function.arity(), "argument", "arguments") + "; got " + args.size());
        }
    }
    
    public BigDecimal value(Calculator environment) {
        return function.apply(environment, args);
    }
}
