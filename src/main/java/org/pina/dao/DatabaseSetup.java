package org.pina.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {

    public static void createAllTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // Protein table with subtype info and functions as CSV string
            String createProteinTable = """
                CREATE TABLE IF NOT EXISTS Protein (
                    uniprotId TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    sequence TEXT,
                    functions TEXT, -- CSV string of functions
                    type TEXT, -- 'Human', 'Viral', or 'Protein'
                    tissueExpression TEXT, -- for HumanProtein
                    hostSpecies TEXT -- for ViralProtein
                );
            """;

            // Community table
            String createCommunityTable = """
                CREATE TABLE IF NOT EXISTS Community (
                    id TEXT PRIMARY KEY,
                    functionalAnnotation TEXT
                );
            """;

            // Join table for Community <-> Protein many-to-many
            String createCommunityProteinTable = """
                CREATE TABLE IF NOT EXISTS Community_Protein (
                    community_id TEXT NOT NULL,
                    protein_id TEXT NOT NULL,
                    PRIMARY KEY (community_id, protein_id),
                    FOREIGN KEY (community_id) REFERENCES Community(id) ON DELETE CASCADE,
                    FOREIGN KEY (protein_id) REFERENCES Protein(uniprotId) ON DELETE CASCADE
                );
            """;

            // Interaction table
            String createInteractionTable = """
                CREATE TABLE IF NOT EXISTS Interaction (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    protein1_id TEXT NOT NULL,
                    protein2_id TEXT NOT NULL,
                    confidenceScore REAL,
                    FOREIGN KEY (protein1_id) REFERENCES Protein(uniprotId) ON DELETE CASCADE,
                    FOREIGN KEY (protein2_id) REFERENCES Protein(uniprotId) ON DELETE CASCADE
                );
            """;

            // Network table
            String createNetworkTable = """
                CREATE TABLE IF NOT EXISTS Network (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL
                );
            """;

            // Network_Protein join table
            String createNetworkProteinTable = """
                CREATE TABLE IF NOT EXISTS Network_Protein (
                    network_id INTEGER NOT NULL,
                    protein_id TEXT NOT NULL,
                    PRIMARY KEY (network_id, protein_id),
                    FOREIGN KEY (network_id) REFERENCES Network(id) ON DELETE CASCADE,
                    FOREIGN KEY (protein_id) REFERENCES Protein(uniprotId) ON DELETE CASCADE
                );
            """;

            // Network_Community join table
            String createNetworkCommunityTable = """
                CREATE TABLE IF NOT EXISTS Network_Community (
                    network_id INTEGER NOT NULL,
                    community_id TEXT NOT NULL,
                    PRIMARY KEY (network_id, community_id),
                    FOREIGN KEY (network_id) REFERENCES Network(id) ON DELETE CASCADE,
                    FOREIGN KEY (community_id) REFERENCES Community(id) ON DELETE CASCADE
                );
            """;

            // Network_Interaction join table
            String createNetworkInteractionTable = """
                CREATE TABLE IF NOT EXISTS Network_Interaction (
                    network_id INTEGER NOT NULL,
                    interaction_id INTEGER NOT NULL,
                    PRIMARY KEY (network_id, interaction_id),
                    FOREIGN KEY (network_id) REFERENCES Network(id) ON DELETE CASCADE,
                    FOREIGN KEY (interaction_id) REFERENCES Interaction(id) ON DELETE CASCADE
                );
            """;

            // Execute all table creation statements
            stmt.execute(createProteinTable);
            stmt.execute(createCommunityTable);
            stmt.execute(createCommunityProteinTable);
            stmt.execute(createInteractionTable);
            stmt.execute(createNetworkTable);
            stmt.execute(createNetworkProteinTable);
            stmt.execute(createNetworkCommunityTable);
            stmt.execute(createNetworkInteractionTable);
        }
    }
}
