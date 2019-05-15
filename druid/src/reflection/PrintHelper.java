/* Java and Android Analysis Framework
 * Copyright (C) 2017 Yifei Zhang, Tian Tan, Yue Li and Jingling Xue
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
package reflection;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

public class PrintHelper {
	private Set<PrintStream> outputs;
	
	public PrintHelper() {
		outputs = new HashSet<>();
	}
	
	public void add(PrintStream output) {
		if(! outputs.contains(output))
			outputs.add(output);
	}
	
	public void println(String s) {
		for(PrintStream output : outputs)
			output.println(s);
	}
}
