package app.thewalker.listeners;

import java.util.ArrayList;

import app.thewalker.pojo.HistoryDataItem;

public interface HistoryDataItemsListener {
    void historyDataItemsList(ArrayList<HistoryDataItem> historyDataItems);
}
