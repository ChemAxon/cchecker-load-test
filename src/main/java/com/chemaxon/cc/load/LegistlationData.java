package com.chemaxon.cc.load;

import java.util.List;

public class LegistlationData {
    
    private List<String> casNubmers;
    private List<String> deaNubmers;
    private String example;
    private String categoryName;
    private String legislativeLinks;
    private String molName;
    private String input;
    private String errorMessage;
    private boolean error;
    
    
    public List<String> getCasNubmers() {
        return casNubmers;
    }
    public void setCasNubmers(List<String> casNubmers) {
        this.casNubmers = casNubmers;
    }
    public List<String> getDeaNubmers() {
        return deaNubmers;
    }
    public void setDeaNubmers(List<String> deaNubmers) {
        this.deaNubmers = deaNubmers;
    }
    public String getExample() {
        return example;
    }
    public void setExample(String example) {
        this.example = example;
    }
    public String getCategoryName() {
        return categoryName;
    }
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    public String getLegislativeLinks() {
        return legislativeLinks;
    }
    public void setLegislativeLinks(String legislativeLinks) {
        this.legislativeLinks = legislativeLinks;
    }
    public String getMolName() {
        return molName;
    }
    public void setMolName(String molName) {
        this.molName = molName;
    }
    public String getInput() {
        return input;
    }
    public void setInput(String input) {
        this.input = input;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    public boolean isError() {
        return error;
    }
    public void setError(boolean error) {
        this.error = error;
    }
}
