package org.jabref.gui.menus;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.jabref.gui.importer.fetcher.WebSearchPaneViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.SearchHistory;

public class SearchHistoryMenu extends Menu {

    private final SearchHistory history;
    private WebSearchPaneViewModel viewModel;
    private StringProperty query;

    public SearchHistoryMenu() {
        setText(Localization.lang("Recent web search"));
        viewModel = null;
        query = null;

        history = new SearchHistory();
        setDisable(true);
    }

    /**
     * Adds the filename to the top of the menu. If it already is in
     * the menu, it is merely moved to the top.
     */
    public void insertSearch(String query) {
        //Verify if the query exists in the search history
        if (query.equals("")) return;

        if (!history.contains(query)){
            history.newSearch(query);
        } else {
            history.removeSearch(query);
            history.newSearch(query);
        }

        setItems();
        setDisable(false);
    }

    /**
     *
     */
    private void setItems() {
        getItems().clear();
        for (int index = 0; index < history.size(); index++) {
            addItem(history.getSearchAt(index), index + 1);
        }
    }

    /**
     *
     * @param query
     * @param num
     */
    private void addItem(String query, int num) {
        String number = Integer.toString(num);
        MenuItem item = new MenuItem("[" + number + "] " + query);

        item.setMnemonicParsing(false);
        item.setOnAction(event -> this.openSearch(query));
        getItems().add(item);
    }

    /**
     *
     * @param query
     */
    private void openSearch(String query) {
        this.query.setValue(query);
        this.viewModel.search();
        this.insertSearch(query);
    }

    /**
     *
     * @param query
     * @param viewModel
     */
    public void setSearchBar(StringProperty query, WebSearchPaneViewModel viewModel) {
        this.query = query;
        this.viewModel = viewModel;
    }

}
