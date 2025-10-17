package org.example.rostering;


import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.constraints.SoftCardinalityDC;
import org.maxicp.cp.engine.core.CPBoolVar;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.search.DFSearch;
import org.maxicp.search.Objective;
import org.maxicp.search.Searches;
import static org.maxicp.cp.CPFactory.*;

import java.io.*;
import java.util.*;

public class Rostering {


    public static CPIntVar[] getColumn(CPIntVar[][] matrix, int colIndex) {;
        return Arrays.stream(matrix).map(row -> row[colIndex]).toArray(CPIntVar[]::new);
    }

    RosteringData data;
    CPSolver cp;
    // x[e][s] = true if employee e works at slot s
    CPBoolVar[][] x;
    // skill[e][s] = k if employee skilled used at slot s is k
    CPIntVar[][] skill;

    int[][] skillBest; // current best solution


    CPIntVar[] missedSkills; // number of skills missing at each slot
    CPIntVar totalMissedSkills; // total number of missing skills


    public Rostering(RosteringData data) {
        this.data = data;
        this.cp = CPFactory.makeSolver();

        x = new CPBoolVar[data.employees][data.slots];

        for (int e = 0; e < data.employees; e++) {
            x[e] = CPFactory.makeBoolVarArray(cp, data.slots);
        }

        skill = new CPIntVar[data.employees][data.slots];
        for (int e = 0; e < data.employees; e++) {
            for (int s = 0; s < data.slots; s++) {
                skill[e][s] = CPFactory.makeIntVar(cp, data.getEmployeeSkills(e));
            }
        }
        skillBest = new int[data.employees][data.slots];

        missedSkills = CPFactory.makeIntVarArray(cp, data.slots, 0, data.employees);
        totalMissedSkills = sum(missedSkills);

        channelSkillAndX();
        //skillRequirementsHard();
        skillRequirementsSoft();
    }

    private void channelSkillAndX() {
        for (int s = 0; s < data.slots; s++) {
            for (int e = 0; e < data.employees; e++) {
                // link skill and x
                cp.post(CPFactory.or(x[e][s],CPFactory.isEq(skill[e][s], data.dummySkill))); // dummy skill 0 if not working
            }
        }
    }

    private void skillRequirementsSoft() {
        for (int s = 0; s < data.slots; s++) {
            int [] minCard = data.getMinCard(s);
            int [] maxCard = new int[data.skills + 1];
            Arrays.fill(maxCard, data.employees);
            System.out.println("Slot " + s + " minCard: " + Arrays.toString(minCard) + " maxCard: " + Arrays.toString(maxCard));
            System.out.println(Arrays.toString(getColumn(skill, s))+ " missedSkills: "+ missedSkills[s]);

            cp.post(new SoftCardinalityDC(getColumn(skill, s),0, minCard, maxCard, missedSkills[s]));
        }
    }

    private void skillRequirementsHard() {
        for (int s = 0; s < data.slots; s++) {
            // all skill variables at slot s
            CPIntVar[] skillAtS = getColumn(skill, s);

            for (int k = 0; k < data.skills; k++) {
                int k_ = k;
                int s_ = s;
                CPIntVar[] skillKUsed = CPFactory.makeIntVarArray(data.employees, e -> CPFactory.isEq(skill[e][s_],k_));
                cp.post(CPFactory.ge(CPFactory.sum(skillKUsed), data.getMinCard(s)[k]));
            }
            // cp.post(new CardinalityMinFWC(skillAtS, minCard));
        }
    }

    public void optimize() {
        CPIntVar [] xFlat = Arrays.stream(x).flatMap(Arrays::stream).toArray(CPIntVar[]::new);
        CPIntVar [] skillFlat = Arrays.stream(skill).flatMap(Arrays::stream).toArray(CPIntVar[]::new);

        DFSearch dfSearch = CPFactory.makeDfs(cp, Searches.and(Searches.firstFail(xFlat), Searches.firstFail(skillFlat)));
        System.out.println("Dummy skill: " + data.dummySkill);
        dfSearch.onSolution(() -> {
            System.out.println("===========> Total missed skills: " + totalMissedSkills);

            // update skill best
            for (int e = 0; e < data.employees; e++) {
                for (int t = 0; t < data.slots; t++) {
                    skillBest[e][t] = skill[e][t].min();
                }
            }

            /*
            System.out.println("Employee");
            for (int e = 0; e < data.employees; e++) {
                System.out.println("employee "+ e + " :"+Arrays.toString(x[e]));
            }
            System.out.println("Skill used");
            for (int e = 0; e < data.employees; e++) {
                System.out.println("employee "+ e + " : " +Arrays.toString(skill[e]));
            }*/
        });

        Objective obj = cp.minimize(totalMissedSkills);
        dfSearch.optimize(obj, s -> s.numberOfSolutions() > 0);
        //dfSearch.optimize(obj);
        Random rand = new Random(0);

        // LNS
        for (int iter = 0; iter < 1000; iter++) {
            int it = iter;
            dfSearch.optimizeSubjectTo(obj, s -> s.numberOfFailures() > 1000, () -> {
                System.out.println("................ restart .............." + it);
                for (int e = 0; e < data.employees; e++) {
                    for (int t = 0; t < data.slots; t++) {
                        if (rand.nextDouble() > 0.20) { // 75% of the employe-slot remain fixed as the currebnt best solution
                            cp.post(eq(skill[e][t],skillBest[e][t]), false); // false is to delay fix-point
                        }
                    }
                }
                cp.fixPoint();

            });
        }
    }


    // --- Example usage ---
    public static void main(String[] args) throws IOException {
        try {
            RosteringData data_ = RosteringData.parseFile("data/ROSTERING/input_hard.txt");
            RosteringData data = RosteringData.randomInstance(90,30,10,0.3,5);

            Rostering rostering = new Rostering(data);
            rostering.optimize();

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }




}