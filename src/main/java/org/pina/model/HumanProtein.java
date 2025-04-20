package org.pina.model;

import java.util.Set;

public class HumanProtein extends Protein {
    private String tissueExpression;

    public HumanProtein(String uniprotId, String name, String sequence,
                        Set<String> functions, String tissueExpression) {
        super(uniprotId, name, sequence, functions);
        this.tissueExpression = tissueExpression;
    }

    @Override
    public String getBasicInfo() {
        return "[Human] " + super.getBasicInfo() + " | Expressed in: " + tissueExpression;
    }
}