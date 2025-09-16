/*
 * MaxiCP is under MIT License
 * Copyright (c)  2024 UCLouvain
 *
 */

package org.example;

import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.search.DFSearch;
import org.maxicp.search.SearchStatistics;

import java.util.Arrays;

import static org.maxicp.cp.CPFactory.*;
import static org.maxicp.search.Searches.*;


/**
 * The N-Queens problem.
 * <a href="http://csplib.org/Problems/prob054/">CSPLib</a>.
 */
public class NQueens {
    public static void main(String[] args) {
        int n = 8;

        CPSolver cp = CPFactory.makeSolver();
        CPIntVar[] q = CPFactory.makeIntVarArray(cp, n, n);
        CPIntVar[] qL = CPFactory.makeIntVarArray(n, i -> minus(q[i],i));
        CPIntVar[] qR = CPFactory.makeIntVarArray(n, i -> plus(q[i],i));

        cp.post(allDifferent(q));
        cp.post(allDifferent(qL));
        cp.post(allDifferent(qR));


        DFSearch search = makeDfs(cp, firstFail(q));


        /*
        DFSearch search = CPFactory.makeDfs(cp, () -> {
            int idx = -1; // index of the first variable that is not fixed
            for (int k = 0; k < q.length; k++)
                if (q[k].size() > 1) {
                    idx = k;
                    break;
                }
            if (idx == -1)
                return EMPTY;
            else {
                CPIntVar qi = q[idx];
                int v = qi.min();
                Runnable left = () -> cp.post(CPFactory.eq(qi, v));
                Runnable right = () -> cp.post(CPFactory.neq(qi, v));
                return new Runnable[]{left, right};
            }
        });*/

        // a more compact first fail search using selectors is given next
        /*
        DFSearch search = makeDfs(cp, () -> {
            CPIntVar qs = selectMin(q,
                    qi -> qi.size() > 1,
                    qi -> qi.size());
            if (qs == null) return EMPTY;
            else {
                int v = qs.min();
                return branch(() -> cp.post(CPFactory.eq(qs, v)),
                        () -> cp.post(CPFactory.neq(qs, v)));
            }
        });*/




    }
}
