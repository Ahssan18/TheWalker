package app.thewalker.pojo;

public class HistoryDataItem {
    String updated_on;
    int num_coins;
    int steps;
    public HistoryDataItem(String updated_on,int num_coins,int steps){
        this.num_coins=num_coins;
        this.updated_on=updated_on;
        this.steps=steps;
    }

    public String getUpdated_on() {
        return updated_on;
    }

    public void setUpdated_on(String updated_on) {
        this.updated_on = updated_on;
    }

    public int getNum_coins() {
        return num_coins;
    }

    public void setNum_coins(int num_coins) {
        this.num_coins = num_coins;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }
}
