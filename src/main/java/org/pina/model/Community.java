package org.pina.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Community ").append(id).append(" ===\n");
        sb.append("Functional Annotation: ").append(
                functionalAnnotation.isBlank() ? "None" : functionalAnnotation
        ).append("\n");

        sb.append("\nProteins (").append(proteins.size()).append("):\n");
        if (proteins.isEmpty()) {
            sb.append("  - No proteins in this community");
        } else {
            boolean first = true;
            for (Protein p : proteins) {
                if (!first) sb.append("\n");  // New line after first entry
                sb.append("  - ").append(p.getName())
                        .append(" (").append(p.getUniprotId()).append(")");
                first = false;
            }
        }
        return sb.toString();
    }
}
