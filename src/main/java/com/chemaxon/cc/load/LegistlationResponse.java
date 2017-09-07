package com.chemaxon.cc.load;

import java.util.List;

public class LegistlationResponse {

    private List<List<LegistlationData>> simpleResponses;
    
    public List<List<LegistlationData>> getSimpleResponses() {
        return simpleResponses;
    }
    
    public void setSimpleResponses(List<List<LegistlationData>> simpleResponses) {
        this.simpleResponses = simpleResponses;
    }
    
}
