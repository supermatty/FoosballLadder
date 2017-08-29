package com.storefront.foosballladder.models;

import android.support.annotation.Keep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class TeamResultCollection implements Iterable<TeamResult>, Iterator<TeamResult> {

    @SuppressWarnings("WeakerAccess")
    @Keep
    public ArrayList<TeamResult> results;

    private int count = 0;

    public TeamResultCollection(Collection<TeamResult> results) {
        this.results = new ArrayList<>(results);
    }


    @SuppressWarnings("unused")
    public TeamResultCollection() { }


    @Override
    public Iterator<TeamResult> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return count < results.size();
    }

    @Override
    public TeamResult next() {
        if (count == results.size()) {
            throw new NoSuchElementException();
        }
        count++;
        return results.get(count - 1);
    }
}
