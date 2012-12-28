package org.jessies.calc;

/*
 * This file is part of Mathdroid.
 * Copyright (C) 2012 Elliott Hughes <enh@jessies.org>.
 * 
 * Mathdroid is free software; you can redistribute it and/or modify
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

public final class StringNode implements Node {
  private final String value;

  public StringNode(String value) {
    this.value = value;
  }

  public Node evaluate(Calculator environment) {
    return this;
  }

  public Node simplify(Calculator environment) {
    return this;
  }

  public String toInputString() {
    return '"' + value + '"'; // TODO: escaping.
  }

  @Override public String toString() {
    return value;
  }
}
