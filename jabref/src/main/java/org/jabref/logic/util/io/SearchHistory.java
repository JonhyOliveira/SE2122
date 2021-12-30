package org.jabref.logic.util.io;

import java.util.LinkedList;
import java.util.List;

public class SearchHistory {

    static final int HISTORY_SIZE = 10;

    private final LinkedList<String> history;

    public SearchHistory(){
        this.history = new LinkedList<>();
    }

    /**
     * Returns the search history data structure
     * @return
     */
    public List<String> getHistory() {
        return history;
    }

    /**
     * Returns the search history at index.
     * @param index
     * @return
     */
    public String getSearchAt(int index) {
        return history.get(index);
    }

    /**
     * Returns the current size of the search history.
     * @return
     */
    public int size() {
        return history.size();
    }

    /**
     * Checks if the search history is empty.
     * @return
     */
    public boolean isEmpty(){
        return history.isEmpty();
    }

    /**
     * Inserts new search history query.
     * @param query
     */
    public void newSearch(String query) {
        if (this.size() < HISTORY_SIZE){
            history.push(query);
        } else {
            history.removeLast();
            history.push(query);
        }
    }

    /**
     * Checks if the history contains the query
     * @param query
     * @return
     */
    public boolean contains(String query) {
        return history.contains(query);
    }

    /**
     * Removes the given query from the history
     * @param query
     * @return
     */
    public boolean removeSearch(String query) {
        return history.remove(query);
    }
}
