package com.chemaxon.cc.load;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SearchResponseStat {
    
    private int passed = 0;
    private int error = 0;
    private List<Integer> hits = new ArrayList<>();

    public void registerPassed() {
        ++passed;
    }
    
    public void registerError() {
        ++error;
    }
    
    public void registerHit(int hitSize) {
        hits.add(hitSize);
    }
    
    public int getPassed() {
        return passed;
    }
    
    public int getError() {
        return error;
    }
    
    public List<Integer> getHits() {
        return Collections.unmodifiableList(hits);
    }
    
    public int getHitCount() {
        return hits.size();
    }
    
    public int getHitSize() {
        return hits.stream().collect(Collectors.summingInt(i -> i.intValue()));
    }
}
