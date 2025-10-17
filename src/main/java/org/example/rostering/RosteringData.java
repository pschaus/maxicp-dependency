package org.example.rostering;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RosteringData {

    final int slots;
    final int employees;
    final int skills;
    final int dummySkill;
    public int[][] employeeSkills;
    public int[][] slotDemands;

    public RosteringData(int nSlots, int nEmployees, int skills) {
        this.slots = nSlots;
        this.employees = nEmployees;
        this.skills = skills;
        this.dummySkill = skills; // dummy skill index
        this.employeeSkills = new int[nEmployees][skills];
        this.slotDemands = new int[nSlots][skills];
    }

    /**
     * Minimum cardinality for each skill, including the dummy skill
     */
    public int[] getMinCard(int s) {
        int[] minCard = new int[skills + 1];
        for (int k = 0; k < skills; k++) {
            minCard[k] = slotDemands[s][k];
        }
        return minCard;
    }

    /**
     * Get the set of skills employee e has (including the dummy skill)
     */
    public Set<Integer> getEmployeeSkills(int e) {
        // set of skills employee e has
        ArrayList<Integer> skillsE = new ArrayList<>();
        for (int k = 0; k < this.skills; k++) {
            if (employeeSkills[e][k] == 1) {
                skillsE.add(k);
            }
        }
        skillsE.add(dummySkill); // add dummy skill
        return new HashSet<>(skillsE);
    }


    /**
     * Save the instance to a file in the specified format.
     * @param filename The name of the file to save to.
     * @throws IOException If an I/O error occurs.
     */
    /**
     * Save the instance to a file in the specified format, including comments.
     * @param filename The name of the file to save to.
     * @throws IOException If an I/O error occurs.
     */
    public void saveToFile(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            // Write header with comment
            writer.write(String.format("%d %d %d #slots #employees #skills%n", slots, employees, skills));

            // Write employee-skill matrix with comment
            for (int e = 0; e < employees; e++) {
                for (int k = 0; k < skills; k++) {
                    writer.write(employeeSkills[e][k] + " ");
                }
                if (e == 0) {
                    writer.write("# employees × skills matrix");
                }
                writer.newLine();
            }

            // Write slot demands with comment
            for (int s = 0; s < slots; s++) {
                for (int k = 0; k < skills; k++) {
                    writer.write(slotDemands[s][k] + " ");
                }
                if (s == 0) {
                    writer.write("# skill demand for each slot");
                }
                writer.newLine();
            }
        }
    }

    public static RosteringData randomInstance(int nSlots, int nEmployees, int nSkills, double skillProb, int maxDemand) {
        RosteringData data = new RosteringData(nSlots, nEmployees, nSkills);
        Random rand = new Random(0);

        // Randomly assign skills to employees
        for (int e = 0; e < nEmployees; e++) {
            for (int k = 0; k < nSkills; k++) {
                if (rand.nextDouble() < skillProb) {
                    data.employeeSkills[e][k] = 1;
                } else {
                   data.employeeSkills[e][k] = 0;
                }
            }
        }

        // Randomly generate skill demands for each slot
        for (int s = 0; s < nSlots; s++) {
            for (int k = 0; k < nSkills; k++) {
                data.slotDemands[s][k] = (int) (Math.random() * (maxDemand + 1));
            }
        }
        return data;
    }


    public static RosteringData parseFile(String filename) throws IOException {

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;

            // --- Read first line: slots, employees, skills ---
            line = br.readLine();
            line = stripComments(line);
            String[] parts = line.trim().split("\\s+");
            int nSlots = Integer.parseInt(parts[0]);
            int nEmployees = Integer.parseInt(parts[1]);
            int nSkills = Integer.parseInt(parts[2]);

            RosteringData data = new RosteringData(nSlots, nEmployees, nSkills);

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
            return data;
        }
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



