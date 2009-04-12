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

public class Calculator {
    public static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);
    
    private final CalculatorLexer lexer;
    
    public Calculator(String expression) {
        this.lexer = new CalculatorLexer(expression);
    }
    
    public String evaluate() throws CalculatorError {
        CalculatorAstNode ast = expr();
        BigDecimal value = ast.value();
        return value.toString();
    }
    
    private CalculatorAstNode expr() {
        return orExpression();
    }
    
    // Mathematica operator precedence: http://reference.wolfram.com/mathematica/tutorial/OperatorInputForms.html
    
    // |
    private CalculatorAstNode orExpression() {
        CalculatorAstNode result = xorExpression();
        while (lexer.token() == CalculatorToken.B_OR) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorAstNode(op, result, xorExpression());
        }
        return result;
    }
    
    // ^
    private CalculatorAstNode xorExpression() {
        CalculatorAstNode result = andExpression();
        while (lexer.token() == CalculatorToken.B_XOR) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorAstNode(op, result, andExpression());
        }
        return result;
    }
    
    // &
    private CalculatorAstNode andExpression() {
        CalculatorAstNode result = relationalExpression();
        while (lexer.token() == CalculatorToken.B_AND) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorAstNode(op, result, relationalExpression());
        }
        return result;
    }
    
    // == >= > <= < !=
    private CalculatorAstNode relationalExpression() {
        CalculatorAstNode result = shiftExpression();
        while (lexer.token() == CalculatorToken.EQ || lexer.token() == CalculatorToken.GE || lexer.token() == CalculatorToken.GT || lexer.token() == CalculatorToken.LE || lexer.token() == CalculatorToken.LT || lexer.token() == CalculatorToken.NE) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorAstNode(op, result, shiftExpression());
        }
        return result;
    }
    
    // << >>
    private CalculatorAstNode shiftExpression() {
        CalculatorAstNode result = additiveExpression();
        while (lexer.token() == CalculatorToken.SHL || lexer.token() == CalculatorToken.SHR) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorAstNode(op, result, additiveExpression());
        }
        return result;
    }
    
    // + -
    private CalculatorAstNode additiveExpression() {
        CalculatorAstNode result = multiplicativeExpression();
        while (lexer.token() == CalculatorToken.PLUS || lexer.token() == CalculatorToken.MINUS) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorAstNode(op, result, multiplicativeExpression());
        }
        return result;
    }
    
    // * / %
    private CalculatorAstNode multiplicativeExpression() {
        CalculatorAstNode result = unaryExpression();
        while (lexer.token() == CalculatorToken.MUL || lexer.token() == CalculatorToken.DIV || lexer.token() == CalculatorToken.MOD) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorAstNode(op, result, unaryExpression());
        }
        return result;
    }
    
    // ~ -
    private CalculatorAstNode unaryExpression() {
        if (lexer.token() == CalculatorToken.MINUS) {
            lexer.nextToken();
            // Convert (-f) to (0-f) for simplicity.
            return new CalculatorAstNode(CalculatorToken.MINUS, new CalculatorAstNode(new BigDecimal("0", Calculator.MATH_CONTEXT)), factor());
        } else if (lexer.token() == CalculatorToken.B_NOT) {
            lexer.nextToken();
            return new CalculatorAstNode(CalculatorToken.B_NOT, factor(), null);
        }
        return factor();
    }
    
    // sqrt
    
    // **
    
    // !
    
    private CalculatorAstNode factor() {
        if (lexer.token() == CalculatorToken.OPEN_PARENTHESIS) {
            expect(CalculatorToken.OPEN_PARENTHESIS);
            CalculatorAstNode result = expr();
            expect(CalculatorToken.CLOSE_PARENTHESIS);
            return result;
        } else if (lexer.token() == CalculatorToken.NUMBER) {
            CalculatorAstNode result = new CalculatorAstNode(lexer.number());
            expect(CalculatorToken.NUMBER);
            return result;
        } else if (lexer.token() == CalculatorToken.IDENTIFIER) {
            CalculatorAstNode result = new CalculatorAstNode(lexer.identifier());
            expect(CalculatorToken.IDENTIFIER);
            return result;
        } else {
            throw new CalculatorError("unexpected '" + lexer.token() + "'");
        }
    }
    
    private void expect(CalculatorToken what) {
        if (lexer.token() != what) {
            throw new CalculatorError("expected " + what + ", got " + lexer.token() + " instead");
        }
        lexer.nextToken();
    }
}
