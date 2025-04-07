package org.pina.model;

import java.util.HashSet;
import java.util.Set;

public class Community {
    private String id;
    private Set<Protein> proteins;
    private String functionalAnnotation;

    public Community(String id, Set<Protein> proteins, String functionalAnnotation) {
        this.id = id;
        this.proteins = new HashSet<>(proteins);
        this.functionalAnnotation = functionalAnnotation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<Protein> getProteins() {
        return Set.copyOf(proteins);
    }

    public void setProteins(Set<Protein> proteins) {
        this.proteins = new HashSet<>(proteins);
    }

    public String getFunctionalAnnotation() {
        return functionalAnnotation;
    }

    public void setFunctionalAnnotation(String functionalAnnotation) {
        this.functionalAnnotation = functionalAnnotation;
    }
}
