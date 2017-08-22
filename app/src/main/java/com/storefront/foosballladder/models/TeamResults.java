package com.storefront.foosballladder.models;

import android.support.annotation.NonNull;

public class TeamResults implements Comparable<TeamResults> {
    private String name;
    private int wins = 0;
    private int losses = 0;
    private int goalsFor = 0;
    private int goalsAgainst = 0;

    public TeamResults(String teamName) {
        name = teamName;
    }

    public double getWinPercentage() {
        return wins + losses == 0 ? 0.5 : (double) wins / (wins + losses);
    }

    @Override
    public int compareTo(@NonNull TeamResults results) {
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

    public int getWins() {
        return wins;
    }

    public void addWin() {
        this.wins = +1;
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
