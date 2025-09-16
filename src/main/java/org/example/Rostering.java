package org.example;


import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.constraints.CardinalityMinFWC;
import org.maxicp.cp.engine.core.CPBoolVar;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.search.DFSearch;
import org.maxicp.search.Searches;

import java.io.*;
import java.util.*;

public class Rostering {

    // --- Example usage ---
    public static void main(String[] args) throws IOException {
        try {
            RosteringData data = parseFile("data/ROSTERING/input1.txt");
            int dummySkill = data.skills;

            CPSolver cp = CPFactory.makeSolver();
            // x[e][s]=true if employee e works at slot s
            CPBoolVar[][] x = new CPBoolVar[data.employees][data.slots];
            for (int e = 0; e < data.employees; e++) {
                x[e] = CPFactory.makeBoolVarArray(cp, data.slots);
            }
            // skill[e][s]=k if employee skilled used at slot s is k
            CPIntVar[][] skill = new CPIntVar[data.employees][data.slots];
            for (int e = 0; e < data.employees; e++) {
                for (int s = 0; s < data.slots; s++) {
                    // set of skills employee e has
                    ArrayList<Integer> skillsE = new ArrayList<>();
                    for (int k = 0; k < data.skills; k++) {
                        if (data.employeeSkills[e][k] == 1) {
                            skillsE.add(k);
                        }
                    }
                    Set<Integer> skillsS = new HashSet<>(skillsE);
                    skillsS.add(dummySkill); // add dummy skill
                    skill[e][s] = CPFactory.makeIntVar(cp, skillsS);
                }
            }
            // for each slot, the number of skills that are scheduled
            CPIntVar[][] numSkills = new CPIntVar[data.slots][data.skills+1];
            for (int s = 0; s < data.slots; s++) {
                numSkills[s] = CPFactory.makeIntVarArray(cp, data.skills+1, 0, data.employees);
            }

            for (int s = 0; s < data.slots; s++) {
                for (int e = 0; e < data.employees; e++) {
                    // link skill and x
                    cp.post(CPFactory.or(x[e][s],CPFactory.isEq(skill[e][s], dummySkill))); // dummy skill 0 if not working
                }
            }

            for (int s = 0; s < data.slots; s++) {
                // all skill variables at slot s
                CPIntVar[] skillAtS = new CPIntVar[data.employees];
                for (int e = 0; e < data.employees; e++) {
                    skillAtS[e] = skill[e][s];
                }
                int [] minCard = new int[data.skills+1];
                for (int k = 0; k < data.skills; k++) {
                    minCard[k] = data.slotDemands[s][k];
                }
                cp.post(new CardinalityMinFWC(skillAtS, minCard));
            }

            CPIntVar [] xFlat = Arrays.stream(x).flatMap(Arrays::stream).toArray(CPIntVar[]::new);
            CPIntVar [] skillFlat = Arrays.stream(skill).flatMap(Arrays::stream).toArray(CPIntVar[]::new);

            DFSearch dfSearch = CPFactory.makeDfs(cp, Searches.and(Searches.firstFail(xFlat), Searches.firstFail(skillFlat)));

            dfSearch.onSolution(() -> {
                System.out.println("Employee");
                for (int e = 0; e < data.employees; e++) {
                    System.out.println(Arrays.toString(x[e]));
                }
                System.out.println("Skill used");
                for (int e = 0; e < data.employees; e++) {
                    System.out.println(Arrays.toString(skill[e]));
                }
            });

            dfSearch.solve(s -> s.numberOfSolutions() > 0);



        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    static class RosteringData {
        int slots;
        int employees;
        int skills;
        int[][] employeeSkills;
        int[][] slotDemands;
    }

    public static RosteringData parseFile(String filename) throws IOException {

        RosteringData data = new RosteringData();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;

            // --- Read first line: slots, employees, skills ---
            line = br.readLine();
            line = stripComments(line);
            String[] parts = line.trim().split("\\s+");
            data.slots = Integer.parseInt(parts[0]);
            data.employees = Integer.parseInt(parts[1]);
            data.skills = Integer.parseInt(parts[2]);

            // --- Read employees × skills matrix ---
            data.employeeSkills = new int[data.employees][data.skills];
            for (int i = 0; i < data.employees; i++) {
                line = br.readLine();
                line = stripComments(line);
                parts = line.trim().split("\\s+");
                for (int j = 0; j < data.skills; j++) {
                    data.employeeSkills[i][j] = Integer.parseInt(parts[j]);
                }
            }

            // --- Read slots × skills matrix ---
            data.slotDemands = new int[data.slots][data.skills];
            for (int i = 0; i < data.slots; i++) {
                line = br.readLine();
                line = stripComments(line);
                parts = line.trim().split("\\s+");
                for (int j = 0; j < data.skills; j++) {
                    data.slotDemands[i][j] = Integer.parseInt(parts[j]);
                }
            }
        }
        return data;
    }

    private static String stripComments(String line) {
        if (line == null) return "";
        int idx = line.indexOf('#');
        if (idx >= 0) {
            return line.substring(0, idx);
        }
        return line;
    }


}