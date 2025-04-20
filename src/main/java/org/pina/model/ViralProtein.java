package org.pina.model;

import java.util.Set;

public class ViralProtein extends Protein {
    private String hostSpecies;

    public ViralProtein(String uniprotId, String name, String sequence,
                        Set<String> functions, String hostSpecies) {
        super(uniprotId, name, sequence, functions);
        this.hostSpecies = hostSpecies;
    }

    @Override
    public String getBasicInfo() {
        return "[Viral] " + super.getBasicInfo() + " | Host: " + hostSpecies;
    }
}