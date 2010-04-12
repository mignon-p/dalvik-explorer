package org.jessies.calc;

/*
 * This file is part of org.jessies.calc.
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

public interface Node {
    /**
     * Returns a Node representing the most-evaluated form of this Node in the given environment.
     * The result may be the same as the original Node (for example, given a Node that's a free variable).
     */
    public Node evaluate(Calculator environment);
    
    /**
     * Returns a Node, the evaluation of which would produce the same result as evaluating this Node, but which is "simpler".
     * This is not intended to factor or expand expressions because that would assume a specific user intent.
     * Rearranging and combining terms is acceptable, as is propagation of constants.
     * Application of identities should only take place for "trivial" ones (involving 0 and 1, for example).
     */
    public Node simplify(Calculator environment);
    
    /**
     * Returns a string that can be parsed to produce an expression equivalent to this Node.
     */
    public String toInputString();
}
