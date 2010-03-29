package org.jessies.calc;

/*
 * This file is part of LittleHelper.
 * Copyright (C) 2010 Elliott Hughes <enh@jessies.org>.
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

public class ListNode implements Iterable<Node>, Node {
    private final List<Node> values = new ArrayList<Node>();
    
    public ListNode add(Node value) {
        values.add(value);
        return this;
    }
    
    public Iterator<Node> iterator() {
        return values.iterator();
    }
    
    @Override public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ListNode)) {
            return false;
        }
        return values.equals(((ListNode) other).values);
    }
    
    @Override public int hashCode() {
        return values.hashCode();
    }
    
    public int size() {
        return values.size();
    }
    
    public ListNode reverse() {
        final ListNode result = new ListNode();
        for (int i = values.size() - 1; i >= 0; --i) {
            result.add(values.get(i));
        }
        return result;
    }
    
    public Node evaluate(Calculator environment) {
        ListNode result = new ListNode();
        for (Node value : values) {
            result.values.add(value.evaluate(environment));
        }
        return result;
    }
    
    public Node simplify(Calculator environment) {
        ListNode result = new ListNode();
        for (Node value : values) {
            result.values.add(value.simplify(environment));
        }
        return result;
    }
    
    public String toInputString() {
        StringBuilder result = new StringBuilder();
        result.append("[");
        for (int i = 0; i < values.size(); ++i) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(values.get(i).toInputString());
        }
        result.append("]");
        return result.toString();
    }
    
    public String toString() {
        return toInputString();
    }
}
