package org.jabref.logic.util.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;
class SearchHistoryTest {

    private SearchHistory history;

    @BeforeEach
    void setUp() {
        history = new SearchHistory();
    }

    @Test
    void newSearchesAreAddedInRightOrder() {
        history.newSearch("aa");
        history.newSearch("bb");
        assertEquals(Arrays.asList("bb", "aa"), history.getHistory());
    }

    @Test
    void removingSearchesLeavesOtherSearchesInRightOrder() {
        history.newSearch("aa");
        history.newSearch("bb");
        history.newSearch("cc");

        history.removeSearch("bb");

        assertEquals(Arrays.asList("cc", "aa"), history.getHistory());
    }

    @Test
    void sizeTest() {
        LinkedList<String> expected = new LinkedList<>();
        for (int i = 0; i < SearchHistory.HISTORY_SIZE; i++) {
            if(i > 0){
                expected.push(String.valueOf((char) i+65));
            }
            assertEquals(i, history.size());
            history.newSearch(String.valueOf((char) i+65));
        }

        assertEquals(SearchHistory.HISTORY_SIZE, history.size());
        history.newSearch("aa");
        expected.push("aa");
        assertEquals(SearchHistory.HISTORY_SIZE, history.size());
        assertEquals(expected, history.getHistory());
    }

    @Test
    void isEmptyTest() {
        assertTrue(history.isEmpty());
        history.newSearch("aa");
        assertFalse(history.isEmpty());
    }

    @Test
    void getSearchAtTest() {
        history.newSearch("aa");
        history.newSearch("bb");
        history.newSearch("cc");
        assertEquals("bb", history.getSearchAt(1));
    }
}
