package org.pina.dao;

import org.pina.model.Interaction;
import org.pina.model.Protein;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InteractionDAO {

    private final Connection connection;
    ProteinDAO proteinDAO ;

    public InteractionDAO(Connection connection) {
        this.connection = connection;
        this.proteinDAO = new ProteinDAO(connection);
    }

    public boolean createInteraction(Interaction interaction) throws SQLException {
        String sql = """
            INSERT INTO Interaction (protein1_id, protein2_id, confidenceScore)
            VALUES (?, ?, ?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, interaction.getProtein1().getUniprotId());
            ps.setString(2, interaction.getProtein2().getUniprotId());
            ps.setDouble(3, interaction.getConfidenceScore());

            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }


    public Interaction readInteractionByProteinIDs(String p1_id, String p2_id) throws SQLException {
        String sql = "SELECT * FROM Interaction WHERE protein1_id = ? AND protein2_id = ? LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p1_id);
            ps.setString(2, p2_id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Protein p1 = proteinDAO.retrieveProteinByIDOrNull(rs.getString("protein1_id"));
                    Protein p2 = proteinDAO.retrieveProteinByIDOrNull(rs.getString("protein2_id"));
                    double score = rs.getDouble("confidenceScore");
                    return new Interaction(p1, p2, score);
                } else {
                    return null;
                }
            }
        }
    }



    public boolean updateInteraction(int id, Interaction interaction) throws SQLException {
        String sql = """
            UPDATE Interaction SET
                protein1_id = ?,
                protein2_id = ?,
                confidenceScore = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, interaction.getProtein1().getUniprotId());
            ps.setString(2, interaction.getProtein2().getUniprotId());
            ps.setDouble(3, interaction.getConfidenceScore());
            ps.setInt(4, id);

            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }


    public boolean deleteInteraction(String p1_id, String p2_id) throws SQLException {
        String sql = "DELETE FROM Interaction WHERE protein1_id = ? AND protein2_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p1_id);
            ps.setString(2, p2_id);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }


    public List<Interaction> listAllInteractions() throws SQLException {
        List<Interaction> interactions = new ArrayList<>();
        String sql = "SELECT * FROM Interaction";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String protein1Id = rs.getString("protein1_id");
                String protein2Id = rs.getString("protein2_id");
                double score = rs.getDouble("confidenceScore");

                var p1 = new org.pina.model.Protein(protein1Id, "", "", new java.util.HashSet<>());
                var p2 = new org.pina.model.Protein(protein2Id, "", "", new java.util.HashSet<>());

                interactions.add(new Interaction(p1, p2, score));
            }
        }
        return interactions;
    }


    public int createOrGetInteractionId(Interaction interaction) throws SQLException {
        String selectSql = """
        SELECT id FROM Interaction
        WHERE (protein1_id = ? AND protein2_id = ? AND confidenceScore = ?)
           OR (protein1_id = ? AND protein2_id = ? AND confidenceScore = ?)
        LIMIT 1
    """;

        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, interaction.getProtein1().getUniprotId());
            ps.setString(2, interaction.getProtein2().getUniprotId());
            ps.setDouble(3, interaction.getConfidenceScore());
            // Also check reverse order (undirected)
            ps.setString(4, interaction.getProtein2().getUniprotId());
            ps.setString(5, interaction.getProtein1().getUniprotId());
            ps.setDouble(6, interaction.getConfidenceScore());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertSql = """
        INSERT INTO Interaction (protein1_id, protein2_id, confidenceScore)
        VALUES (?, ?, ?)
    """;

        try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, interaction.getProtein1().getUniprotId());
            ps.setString(2, interaction.getProtein2().getUniprotId());
            ps.setDouble(3, interaction.getConfidenceScore());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Failed to insert interaction");
                }
            }
        }
    }

}
