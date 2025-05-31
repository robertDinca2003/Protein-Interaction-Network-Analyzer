package org.pina.dao;

import org.pina.model.HumanProtein;
import org.pina.model.Protein;
import org.pina.model.ViralProtein;
import org.pina.service.AuditService;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProteinDAO {

    private final Connection connection;

    public ProteinDAO(Connection connection) {
        this.connection = connection;
    }

    public boolean exists(String uniprotId, String name) throws SQLException {
        String sql = "SELECT 1 FROM Protein WHERE uniprotId = ? AND name = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uniprotId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Protein retrieveProteinByNameOrNull(String name) throws SQLException {
        String sql = "SELECT * FROM Protein WHERE name = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String uniprotId = rs.getString("uniprotId");
                    String sequence = rs.getString("sequence");
                    String functionsCsv = rs.getString("functions");
                    String type = rs.getString("type");
                    String tissueExpression = rs.getString("tissueExpression");
                    String hostSpecies = rs.getString("hostSpecies");

                    Set<String> functions = new HashSet<>();
                    if (functionsCsv != null && !functionsCsv.isEmpty()) {
                        String[] parts = functionsCsv.split(",");
                        for (String part : parts) {
                            functions.add(part.trim());
                        }
                    }

                    if ("Human".equalsIgnoreCase(type)) {
                        return new HumanProtein(uniprotId, name, sequence, functions, tissueExpression);
                    } else if ("Viral".equalsIgnoreCase(type)) {
                        return new ViralProtein(uniprotId, name, sequence, functions, hostSpecies);
                    } else {
                        return new Protein(uniprotId, name, sequence, functions);
                    }
                } else {
                    return null;
                }
            }
        }
    }

    public Protein retrieveProteinByIDOrNull(String uniprotId) throws SQLException {
        String sql = "SELECT * FROM Protein WHERE unitprotId = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uniprotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String sequence = rs.getString("sequence");
                    String functionsCsv = rs.getString("functions");
                    String type = rs.getString("type");
                    String tissueExpression = rs.getString("tissueExpression");
                    String hostSpecies = rs.getString("hostSpecies");

                    Set<String> functions = new HashSet<>();
                    if (functionsCsv != null && !functionsCsv.isEmpty()) {
                        String[] parts = functionsCsv.split(",");
                        for (String part : parts) {
                            functions.add(part.trim());
                        }
                    }

                    if ("Human".equalsIgnoreCase(type)) {
                        return new HumanProtein(uniprotId, name, sequence, functions, tissueExpression);
                    } else if ("Viral".equalsIgnoreCase(type)) {
                        return new ViralProtein(uniprotId, name, sequence, functions, hostSpecies);
                    } else {
                        return new Protein(uniprotId, name, sequence, functions);
                    }
                } else {
                    return null;
                }
            }
        }
    }



    public boolean addProteinIfNotExists(Protein protein) throws SQLException {
        if (exists(protein.getUniprotId(), protein.getName())) {
            return false; // Protein already exists
        }

        String sql = """
            INSERT INTO Protein (uniprotId, name, sequence, functions, type, tissueExpression, hostSpecies)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, protein.getUniprotId());
            ps.setString(2, protein.getName());
            ps.setString(3, protein.getSequence());

            // Serialize functions set as CSV string
            Set<String> functions = protein.getFunctions();
            String functionsCsv = String.join(",", functions);
            ps.setString(4, functionsCsv);

            // Determine type and subclass fields
            String type = "Protein";
            String tissueExpression = null;
            String hostSpecies = null;

            if (protein instanceof org.pina.model.HumanProtein humanProtein) {
                type = "Human";
                tissueExpression = humanProtein.getTissueExpression();
            } else if (protein instanceof org.pina.model.ViralProtein viralProtein) {
                type = "Viral";
                hostSpecies = viralProtein.getHostSpecies();
            }

            ps.setString(5, type);
            ps.setString(6, tissueExpression);
            ps.setString(7, hostSpecies);

            int rows = ps.executeUpdate();
            AuditService.INSTANCE.log("saved_new_protein_in_database|"+ protein.getName());
            return rows > 0;
        }
    }

    public List<String> getAllProteinNames() throws SQLException {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM Protein";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }



    public boolean updateProteinByName(Protein protein) throws SQLException {
        String sql = """
        UPDATE Protein SET
            uniprotId = ?,
            sequence = ?,
            functions = ?,
            type = ?,
            tissueExpression = ?,
            hostSpecies = ?
        WHERE name = ?
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, protein.getUniprotId());
            ps.setString(2, protein.getSequence());

            Set<String> functions = protein.getFunctions();
            String functionsCsv = String.join(",", functions);
            ps.setString(3, functionsCsv);

            String type = "Protein";
            String tissueExpression = null;
            String hostSpecies = null;

            if (protein instanceof HumanProtein humanProtein) {
                type = "Human";
                tissueExpression = humanProtein.getTissueExpression();
            } else if (protein instanceof ViralProtein viralProtein) {
                type = "Viral";
                hostSpecies = viralProtein.getHostSpecies();
            }

            ps.setString(4, type);
            ps.setString(5, tissueExpression);
            ps.setString(6, hostSpecies);

            ps.setString(7, protein.getName());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                AuditService.INSTANCE.log("updated_protein_in_database|" + protein.getName());
                return true;
            } else {
                return false;
            }
        }
    }


    public boolean deleteProteinByName(String name) throws SQLException {
        String sql = "DELETE FROM Protein WHERE name = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                AuditService.INSTANCE.log("deleted_protein_from_database|" + name);
                return true;
            } else {
                return false;
            }
        }catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }

    public Protein mapResultSetToProtein(ResultSet rs) throws SQLException {
        String uniprotId = rs.getString("uniprotId");
        String name = rs.getString("name");
        String sequence = rs.getString("sequence");
        String functionsCsv = rs.getString("functions");
        String type = rs.getString("type");
        String tissueExpression = rs.getString("tissueExpression");
        String hostSpecies = rs.getString("hostSpecies");

        Set<String> functions = new HashSet<>();
        if (functionsCsv != null && !functionsCsv.isEmpty()) {
            String[] parts = functionsCsv.split(",");
            for (String part : parts) {
                functions.add(part.trim());
            }
        }

        if ("Human".equalsIgnoreCase(type)) {
            return new HumanProtein(uniprotId, name, sequence, functions, tissueExpression);
        } else if ("Viral".equalsIgnoreCase(type)) {
            return new ViralProtein(uniprotId, name, sequence, functions, hostSpecies);
        } else {
            return new Protein(uniprotId, name, sequence, functions);
        }
    }

}
