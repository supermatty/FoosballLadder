package com.storefront.foosballladder;

import android.support.annotation.NonNull;

class Results implements Comparable<Results> {
    private String name;
    private int wins = 0;
    private int losses = 0;
    private int goalsFor = 0;
    private int goalsAgainst = 0;

    double getWinPercentage() {
        return wins + losses == 0 ? 0.5 : (double) wins / (wins + losses);
    }

    @Override
    public int compareTo(@NonNull Results results) {
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

    public void setName(String name) {
        this.name = name;
    }

    int getWins() {
        return wins;
    }

    void setWins(int wins) {
        this.wins = wins;
    }

    int getLosses() {
        return losses;
    }

    void setLosses(int losses) {
        this.losses = losses;
    }

    int getGoalsFor() {
        return goalsFor;
    }

    void setGoalsFor(int goalsFor) {
        this.goalsFor = goalsFor;
    }

    int getGoalsAgainst() {
        return goalsAgainst;
    }

    void setGoalsAgainst(int goalsAgainst) {
        this.goalsAgainst = goalsAgainst;
    }
}
