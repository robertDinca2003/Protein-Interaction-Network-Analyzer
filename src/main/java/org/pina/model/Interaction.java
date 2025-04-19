package org.pina.model;

import java.util.Objects;

public class Interaction {
    private Protein protein1;
    private Protein protein2;
    private double confidenceScore;

    public Interaction(Protein protein1, Protein protein2, double confidenceScore) {
        this.protein1 = protein1;
        this.protein2 = protein2;
        this.confidenceScore = confidenceScore;
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
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interaction that = (Interaction) o;
        return (Objects.equals(protein1, that.protein1) &&
                Objects.equals(protein2, that.protein2)) ||
                (Objects.equals(protein1, that.protein2) &&
                        Objects.equals(protein2, that.protein1));
    }

    @Override
    public int hashCode() {
        return Objects.hash(protein1, protein2) + Objects.hash(protein2, protein1);
    }

}
