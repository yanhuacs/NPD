/* Java and Android Analysis Framework
 * Copyright (C) 2017 Diyu Wu, Yulei Sui and Jingling Xue
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package string;

import java.util.List;

import dk.brics.automaton.Automaton;
import dk.brics.string.HotspotKind;
import soot.ValueBox;

public class HotSpot {
	String methodSignature;
	
	//The position of the argument added as a hotspot. 0 is the first method argument.
	//-1 is a method return value added as a hotspot.
	int argumentPosition=-1;

	/**
     * The actual string expression to analyze.
     */
    public ValueBox spot;

    /**
     * The expected analysis result for the expression.
     * This corresponds to the regular expression or automaton
     * given in the runtime method call.
     */
    public Automaton expected;


    HotSpot(ValueBox spot, Automaton expected) {
        this.spot = spot;
        this.expected = expected;
    }
	
}
