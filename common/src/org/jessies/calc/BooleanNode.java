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

public final class BooleanNode implements Node {
    private final boolean value;
    
    public static final BooleanNode TRUE = new BooleanNode(true);
    public static final BooleanNode FALSE = new BooleanNode(false);
    
    private BooleanNode(boolean value) {
        this.value = value;
    }
    
    public BooleanNode and(BooleanNode rhs) {
        return valueOf(value && rhs.value);
    }
    
    public Node evaluate(Calculator environment) {
        return this;
    }
    
    public Node simplify(Calculator environment) {
        return this;
    }
    
    public BooleanNode or(BooleanNode rhs) {
        return valueOf(value || rhs.value);
    }
    
    public BooleanNode not() {
        return value ? FALSE : TRUE;
    }
    
    public static BooleanNode valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }
    
    public String toInputString() {
        return toString();
    }
    
    @Override public String toString() {
        return value ? "true" : "false";
    }
}
