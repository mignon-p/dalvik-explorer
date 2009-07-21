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

public enum CalculatorToken {
    // Special.
    
    END_OF_INPUT("end of input"),
    IDENTIFIER("identifier"),
    NUMBER("number"),
    
    // Ambiguous.
    
    PLING("!"), // May be either L_NOT or FACTORIAL, depending on context.
    MINUS("-"), // May be either unary or binary, depending on context.
    
    // Binary.
    
    ASSIGN("="),
    
    PLUS("+"),
    MUL("*"),
    POW("**"),
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
    
    L_NOT("prefix !"),
    
    B_AND("&"),
    B_OR("|"),
    B_XOR("^"),
    
    // Prefix unary.
    
    B_NOT("unary ~"),
    
    // Postifx unary.
    
    FACTORIAL("postfix !"),
    
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
