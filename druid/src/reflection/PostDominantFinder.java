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

import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.Type;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DominatorsFinder;
import soot.toolkits.graph.MHGPostDominatorsFinder;
import soot.toolkits.graph.UnitGraph;

public class PostDominantFinder {
	public static Type findPostDominator(Body body, AssignStmt s) {
		UnitGraph cfg = new BriefUnitGraph(body);
		DominatorsFinder<Unit> dominators = new MHGPostDominatorsFinder(cfg);
		return getPostDominatorType(cfg, dominators, s);
	}

	private static Type getPostDominatorType(UnitGraph cfg, DominatorsFinder<Unit> dominators, AssignStmt stmt) {
		if(! (stmt.getLeftOp() instanceof Local)) {
			return null;
		}
		Local left = (Local) stmt.getLeftOp();
		for(Unit dom : dominators.getDominators(stmt)) {
			if(dom instanceof AssignStmt
					&& ((AssignStmt) dom).getRightOp() instanceof CastExpr) {
				CastExpr cast = (CastExpr)((AssignStmt) dom).getRightOp();
				if(cast.getOp().equals(left)) {
					Type t = cast.getCastType();
					if(t instanceof ArrayType) {
						return null;
					} else {
						return t;
					}
				}
			}
		}
		return null;		
	}
}
