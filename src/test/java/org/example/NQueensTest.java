package org.example;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NQueensTest {

    @Test
    public void test() {
        assertEquals(92, NQueens.nSolutions(8));
    }
}