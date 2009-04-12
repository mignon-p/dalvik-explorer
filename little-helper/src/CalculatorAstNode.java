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

public class CalculatorAstNode {
    // For numbers.
    private BigDecimal number;
    
    // For identifiers. (FIXME: this assumes all identifiers are constants rather than functions.)
    private String identifier;
    
    // For operators. Unary operators may have rhs null.
    private CalculatorToken op;
    private CalculatorAstNode lhs;
    private CalculatorAstNode rhs;
    
    public CalculatorAstNode(BigDecimal number) {
        this.number = number;
    }
    
    public CalculatorAstNode(String identifier) {
        this.identifier = identifier;
    }
    
    public CalculatorAstNode(CalculatorToken op, CalculatorAstNode lhs, CalculatorAstNode rhs) {
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
    }
    
    public BigDecimal value() {
        if (number != null) {
            return number;
        } else if (identifier != null) {
            throw new CalculatorError("variables not yet implemented");
        } else {
            switch (op) {
            case PLUS: return lhs.value().add(rhs.value());
            case MINUS: return lhs.value().subtract(rhs.value());
                
            case DIV: return lhs.value().divide(rhs.value(), Calculator.MATH_CONTEXT);
            case MUL: return lhs.value().multiply(rhs.value());
            case MOD: return lhs.value().remainder(rhs.value());
                
            case LT: return fromBoolean(cmp() < 0);
            case LE: return fromBoolean(cmp() <= 0);
            case GT: return fromBoolean(cmp() > 0);
            case GE: return fromBoolean(cmp() >= 0);
            case EQ: return fromBoolean(cmp() == 0);
            case NE: return fromBoolean(cmp() != 0);
                
            case SHL: return fromBigInteger(lhs.value().toBigInteger().shiftLeft(rhs.value().intValue()));
            case SHR: return fromBigInteger(lhs.value().toBigInteger().shiftRight(rhs.value().intValue()));
                
            case B_AND: return fromBigInteger(lhs.value().toBigInteger().and(rhs.value().toBigInteger()));
            case B_OR: return fromBigInteger(lhs.value().toBigInteger().or(rhs.value().toBigInteger()));
            case B_XOR: return fromBigInteger(lhs.value().toBigInteger().xor(rhs.value().toBigInteger()));
            case B_NOT: return fromBigInteger(lhs.value().toBigInteger().not());
                
            case POW:
                {
                    try {
                        int n = rhs.value().intValueExact();
                        return lhs.value().pow(n);
                    } catch (ArithmeticException ex) {
                        return fromDouble(Math.pow(lhs.value().doubleValue(), rhs.value().doubleValue()));
                    }
                }
                
            default: throw new CalculatorError("operator " + op + " not yet implemented");
            }
        }
    }
    
    private BigDecimal fromBigInteger(BigInteger i) {
        return new BigDecimal(i, Calculator.MATH_CONTEXT);
    }
    
    private BigDecimal fromDouble(double d) {
        return new BigDecimal(d, Calculator.MATH_CONTEXT);
    }
    
    private BigDecimal fromBoolean(boolean b) {
        return b ? new BigDecimal("0", Calculator.MATH_CONTEXT) : new BigDecimal("1", Calculator.MATH_CONTEXT);
    }
    
    private int cmp() {
        return lhs.value().compareTo(rhs.value());
    }
    
    public String toString() {
        if (number != null) {
            return number.toString();
        } else if (identifier != null) {
            return identifier;
        } else {
            return "((" + lhs + ") " + op + " (" + rhs + "))";
        }
    }
}
