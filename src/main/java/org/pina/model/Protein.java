package org.pina.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Protein {
    private String uniprotId;
    private String name;

    private String sequence;

    private Set<String> functions;

    public Protein(String uniprotId, String name, String sequence, Set<String> functions) {
        this.uniprotId = uniprotId;
        this.name = name;
        this.sequence = sequence;
        this.functions = new HashSet<>(functions);
    }

    public Protein(Protein protein) {
        this.uniprotId = protein.uniprotId;
        this.name = protein.name;
        this.sequence = protein.sequence;
        this.functions = new HashSet<>(protein.functions);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Protein protein = (Protein) o;
        return Objects.equals(uniprotId, protein.uniprotId) && Objects.equals(name, protein.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniprotId, name);
    }

    public String getUniprotId() {
        return uniprotId;
    }

    public void setUniprotId(String uniprotId) {
        this.uniprotId = uniprotId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public Set<String> getFunctions() {
        return Set.copyOf(functions);
    }

    public void setFunctions(Set<String> functions) {
        this.functions = functions;
    }

    public void addFunction(String function) {
        functions.add(function);
    }
}
