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

public enum CalculatorToken {
    // Special.
    
    END_OF_INPUT("end of input"),
    IDENTIFIER("identifier"),
    NUMBER("number"),
    
    // Ambiguous.
    
    PLING("!"), // May be either "not" or "factorial", depending on context.
    MINUS("-"), // May be either unary or binary, depending on context.
    
    // Binary.
    
    ASSIGN("="),
    
    PLUS("+"),
    MUL("*"),
    POW("^"),
    DIV("/"),
    MOD("%"),
    
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),
    EQ("=="),
    NE("!="),
    
    SHL("<<"),
    SHR(">>"),
    
    B_AND("&"),
    B_OR("|"),
    L_AND("&&"),
    L_OR("||"),
    
    // Prefix unary.
    
    B_NOT("unary ~"),
    SQRT("\u221a"),
    
    // Brackets.
    
    OPEN_PARENTHESIS("("),
    CLOSE_PARENTHESIS(")"),
    
    COMMA(",")
    
    ;
    
    public final String name;
    
    private CalculatorToken(String name) {
        this.name = name;
    }
    
    public String toString() {
        return name;
    }
}
