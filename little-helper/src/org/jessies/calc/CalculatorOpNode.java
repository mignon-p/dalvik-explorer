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
import static org.jessies.calc.BigDecimals.*;

public class CalculatorOpNode implements CalculatorAstNode {
    // Unary operators may have rhs null.
    private final CalculatorToken op;
    private final CalculatorAstNode lhs;
    private final CalculatorAstNode rhs;
    
    public CalculatorOpNode(CalculatorToken op, CalculatorAstNode lhs, CalculatorAstNode rhs) {
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
    }
    
    public BigDecimal value(Calculator environment) {
        switch (op) {
        case PLUS:
            return lhs.value(environment).add(rhs.value(environment));
        case MINUS:
            return lhs.value(environment).subtract(rhs.value(environment));
            
        case DIV:
            return lhs.value(environment).divide(rhs.value(environment), MATH_CONTEXT);
        case MUL:
            return lhs.value(environment).multiply(rhs.value(environment));
        case MOD:
            return lhs.value(environment).remainder(rhs.value(environment));
            
        case LT: return fromBoolean(cmp(environment) < 0);
        case LE: return fromBoolean(cmp(environment) <= 0);
        case GT: return fromBoolean(cmp(environment) > 0);
        case GE: return fromBoolean(cmp(environment) >= 0);
        case EQ: return fromBoolean(cmp(environment) == 0);
        case NE: return fromBoolean(cmp(environment) != 0);
            
        case SHL: return fromBigInteger(lhs.value(environment).toBigInteger().shiftLeft(rhs.value(environment).intValue()));
        case SHR: return fromBigInteger(lhs.value(environment).toBigInteger().shiftRight(rhs.value(environment).intValue()));
            
        case B_AND:
            return fromBigInteger(lhs.value(environment).toBigInteger().and(rhs.value(environment).toBigInteger()));
        case B_OR:
            return fromBigInteger(lhs.value(environment).toBigInteger().or(rhs.value(environment).toBigInteger()));
        case B_XOR:
            return fromBigInteger(lhs.value(environment).toBigInteger().xor(rhs.value(environment).toBigInteger()));
        case B_NOT:
            return fromBigInteger(lhs.value(environment).toBigInteger().not());
            
        case POW:
            {
                try {
                    int n = rhs.value(environment).intValueExact();
                    return lhs.value(environment).pow(n);
                } catch (ArithmeticException ex) {
                    return fromDouble(Math.pow(lhs.value(environment).doubleValue(), rhs.value(environment).doubleValue()));
                }
            }
            
        default:
            throw new CalculatorError("operator " + op + " not yet implemented");
        }
    }
    
    private int cmp(Calculator environment) {
        return lhs.value(environment).compareTo(rhs.value(environment));
    }
    
    @Override public String toString() {
        if (rhs == null) {
            return "(unary " + op + " (" + lhs + "))";
        } else {
            return "((" + lhs + ") " + op + " (" + rhs + "))";
        }
    }
}
