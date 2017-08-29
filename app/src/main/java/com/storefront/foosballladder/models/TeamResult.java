package com.storefront.foosballladder.models;

import android.support.annotation.NonNull;

public class TeamResult implements Comparable<TeamResult> {
    private String name;
    private int id;
    private int wins = 0;
    private int losses = 0;
    private int goalsFor = 0;
    private int goalsAgainst = 0;

    public TeamResult(String teamName, int id) {
        name = teamName;
        this.id = id;
    }

    public TeamResult() {

    }

    public double getWinPercentage() {
        return wins + losses == 0 ? 0.5 : (double) wins / (wins + losses);
    }

    @Override
    public int compareTo(@NonNull TeamResult results) {
        if (Double.compare(results.getWinPercentage(), this.getWinPercentage()) == 0) {
            if (Integer.compare(results.getGoalsFor(), this.getGoalsFor()) == 0) {
                return Integer.compare(this.getGoalsAgainst(), results.getGoalsAgainst());
            }

            return Integer.compare(results.getGoalsFor(), this.getGoalsFor());
        }

        return Double.compare(results.getWinPercentage(), this.getWinPercentage());
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getWins() {
        return wins;
    }

    public void addWin() {
        this.wins += 1;
    }

    public int getLosses() {
        return losses;
    }

    public void addLoss() {
        this.losses += 1;
    }

    public int getGoalsFor() {
        return goalsFor;
    }

    public void addGoalsFor(int addGoals) {
        this.goalsFor += addGoals;
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    public void addGoalsAgainst(int addGoalsAgainst) {
        this.goalsAgainst += addGoalsAgainst;
    }
}
