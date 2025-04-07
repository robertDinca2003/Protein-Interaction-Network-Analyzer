package org.pina.model;

public class Interaction {
    private Protein protein1;
    private Protein protein2;
    private double confidenceScore;
    private String experimentType;

    public Interaction(Protein protein1, Protein protein2, double confidenceScore, String experimentType) {
        this.protein1 = protein1;
        this.protein2 = protein2;
        this.confidenceScore = confidenceScore;
        this.experimentType = experimentType;
    }

    public Protein getProtein1() {
        return protein1;
    }

    public void setProtein1(Protein protein1) {
        this.protein1 = protein1;
    }

    public Protein getProtein2() {
        return protein2;
    }

    public void setProtein2(Protein protein2) {
        this.protein2 = protein2;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getExperimentType() {
        return experimentType;
    }

    public void setExperimentType(String experimentType) {
        this.experimentType = experimentType;
    }
}
