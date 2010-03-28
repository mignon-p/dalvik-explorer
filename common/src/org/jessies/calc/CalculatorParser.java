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

import java.util.*;

public class CalculatorParser {
    private final Calculator calculator;
    private final CalculatorLexer lexer;
    
    public CalculatorParser(Calculator calculator, String expression) {
        this.calculator = calculator;
        this.lexer = new CalculatorLexer(expression);
    }
    
    public Node parse() {
        final Node result = parseExpr();
        expect(CalculatorToken.END_OF_INPUT);
        return result;
    }
    
    private Node parseExpr() {
        return parseAssignmentExpression();
    }
    
    // Mathematica operator precedence: http://reference.wolfram.com/mathematica/tutorial/OperatorInputForms.html
    
    // = (assignment)
    private Node parseAssignmentExpression() {
        Node result = parseOrExpression();
        if (lexer.token() == CalculatorToken.ASSIGN) {
            lexer.nextToken();
            result = getFunction("define").bind(Arrays.asList(result, parseOrExpression()));
        }
        return result;
        
    }
    
    // ||
    private Node parseOrExpression() {
        Node result = parseAndExpression();
        while (lexer.token() == CalculatorToken.L_OR) {
            lexer.nextToken();
            // FIXME: make Or varargs.
            result = getFunction(CalculatorToken.L_OR).bind(Arrays.asList(result, parseAndExpression()));
        }
        return result;
    }
    
    // &&
    private Node parseAndExpression() {
        Node result = parseBitOrExpression();
        while (lexer.token() == CalculatorToken.L_AND) {
            lexer.nextToken();
            // FIXME: make And varargs.
            result = getFunction(CalculatorToken.L_AND).bind(Arrays.asList(result, parseBitOrExpression()));
        }
        return result;
    }
    
    // |
    private Node parseBitOrExpression() {
        Node result = parseBitAndExpression();
        while (lexer.token() == CalculatorToken.B_OR) {
            lexer.nextToken();
            // FIXME: make BitOr varargs.
            result = getFunction(CalculatorToken.B_OR).bind(Arrays.asList(result, parseBitAndExpression()));
        }
        return result;
    }
    
    // &
    private Node parseBitAndExpression() {
        Node result = parseRelationalExpression();
        while (lexer.token() == CalculatorToken.B_AND) {
            lexer.nextToken();
            // FIXME: make BitAnd varargs.
            result = getFunction(CalculatorToken.B_AND).bind(Arrays.asList(result, parseRelationalExpression()));
        }
        return result;
    }
    
    // == >= > <= < !=
    private Node parseRelationalExpression() {
        Node result = parseShiftExpression();
        while (lexer.token() == CalculatorToken.EQ || lexer.token() == CalculatorToken.GE || lexer.token() == CalculatorToken.GT || lexer.token() == CalculatorToken.LE || lexer.token() == CalculatorToken.LT || lexer.token() == CalculatorToken.NE) {
            final CalculatorFunction function = getFunction(lexer.token());
            lexer.nextToken();
            result = function.bind(Arrays.asList(result, parseShiftExpression()));
        }
        return result;
    }
    
    // << >>
    private Node parseShiftExpression() {
        Node result = parseAdditiveExpression();
        while (lexer.token() == CalculatorToken.SHL || lexer.token() == CalculatorToken.SHR) {
            final CalculatorFunction function = getFunction(lexer.token());
            lexer.nextToken();
            result = function.bind(Arrays.asList(result, parseAdditiveExpression()));
        }
        return result;
    }
    
    // + -
    private Node parseAdditiveExpression() {
        Node result = parseMultiplicativeExpression();
        while (lexer.token() == CalculatorToken.PLUS || lexer.token() == CalculatorToken.MINUS) {
            final CalculatorFunction function = getFunction(lexer.token());
            lexer.nextToken();
            result = function.bind(Arrays.asList(result, parseMultiplicativeExpression()));
        }
        return result;
    }
    
    // * / %
    private Node parseMultiplicativeExpression() {
        Node result = parseSqrtExpression();
        while (lexer.token() == CalculatorToken.MUL || lexer.token() == CalculatorToken.DIV || lexer.token() == CalculatorToken.MOD) {
            final CalculatorFunction function = getFunction(lexer.token());
            lexer.nextToken();
            result = function.bind(Arrays.asList(result, parseSqrtExpression()));
        }
        return result;
    }
    
    // sqrt
    private Node parseSqrtExpression() {
        if (lexer.token() == CalculatorToken.SQRT) {
            lexer.nextToken();
            return getFunction("sqrt").bind(Collections.singletonList(parseSqrtExpression()));
        } else {
            return parseExponentiationExpression();
        }
    }
    
    // ^
    private Node parseExponentiationExpression() {
        Node result = parseUnaryExpression();
        if (lexer.token() == CalculatorToken.POW) {
            lexer.nextToken();
            result = getFunction(CalculatorToken.POW).bind(Arrays.asList(result, parseExponentiationExpression()));
        }
        return result;
    }
    
    // prefix -, prefix ~, prefix !, postfix !
    private Node parseUnaryExpression() {
        // Prefix unary operators...
        if (lexer.token() == CalculatorToken.MINUS) {
            lexer.nextToken();
            // Convert (-f) to (-1*f) for simplicity.
            return getFunction(CalculatorToken.MUL).bind(Arrays.asList(IntegerNode.valueOf(-1), parseUnaryExpression()));
        } else if (lexer.token() == CalculatorToken.B_NOT) {
            lexer.nextToken();
            return getFunction(CalculatorToken.B_NOT).bind(Collections.singletonList(parseUnaryExpression()));
        } else if (lexer.token() == CalculatorToken.PLING) {
            lexer.nextToken();
            return getFunction("Not").bind(Collections.singletonList(parseUnaryExpression()));
        }
        
        Node result = parseFactor();
        
        // Postfix unary operators...
        while (lexer.token() == CalculatorToken.PLING) {
            expect(CalculatorToken.PLING);
            result = getFunction("Factorial").bind(Collections.singletonList(result));
        }
        return result;
    }
    
    private Node parseFactor() {
        if (lexer.token() == CalculatorToken.OPEN_PARENTHESIS) {
            expect(CalculatorToken.OPEN_PARENTHESIS);
            Node result = parseExpr();
            expect(CalculatorToken.CLOSE_PARENTHESIS);
            return result;
        } else if (lexer.token() == CalculatorToken.OPEN_SQUARE) {
            return parseList();
        } else if (lexer.token() == CalculatorToken.NUMBER) {
            Node result = lexer.number();
            expect(CalculatorToken.NUMBER);
            return result;
        } else if (lexer.token() == CalculatorToken.IDENTIFIER) {
            final String identifier = lexer.identifier();
            expect(CalculatorToken.IDENTIFIER);
            if (lexer.token() == CalculatorToken.OPEN_PARENTHESIS) {
                final CalculatorFunction fn = getFunction(identifier);
                if (fn == null) {
                    throw new CalculatorError("undefined  function '" + identifier + "'");
                }
                return fn.bind(parseArgs());
            } else {
                return new CalculatorVariableNode(identifier);
            }
        } else {
            throw new CalculatorError("unexpected " + quoteTokenForErrorMessage(lexer.token()));
        }
    }
    
    // '(' expr [ ',' expr ] ')'
    private List<Node> parseArgs() {
        final List<Node> result = new LinkedList<Node>();
        expect(CalculatorToken.OPEN_PARENTHESIS);
        while (lexer.token() != CalculatorToken.CLOSE_PARENTHESIS) {
            result.add(parseExpr());
            if (lexer.token() == CalculatorToken.COMMA) {
                expect(CalculatorToken.COMMA);
                continue;
            }
        }
        expect(CalculatorToken.CLOSE_PARENTHESIS);
        return result;
    }
    
    // '[' [ expr [ ',' expr ] ] ']'
    private ListNode parseList() {
        final ListNode result = new ListNode();
        expect(CalculatorToken.OPEN_SQUARE);
        while (lexer.token() != CalculatorToken.CLOSE_SQUARE) {
            result.add(parseExpr());
            if (lexer.token() == CalculatorToken.COMMA) {
                expect(CalculatorToken.COMMA);
                continue;
            }
        }
        expect(CalculatorToken.CLOSE_SQUARE);
        return result;
    }
    
    private void expect(CalculatorToken what) {
        if (lexer.token() != what) {
            throw new CalculatorError("expected " + quoteTokenForErrorMessage(what) + ", got " + quoteTokenForErrorMessage(lexer.token()) + " instead");
        }
        lexer.nextToken();
    }
    
    private final CalculatorFunction getFunction(String name) {
        return calculator.getFunction(name);
    }
    
    private final CalculatorFunction getFunction(CalculatorToken token) {
        return calculator.getFunction(token);
    }
    
    private static String quoteTokenForErrorMessage(CalculatorToken token) {
        String result = token.toString();
        if (result.length() > 2) {
            // We probably already have something usable like "end of input".
            return result;
        }
        // Quote operators.
        return "'" + result + "'";
    }
}
