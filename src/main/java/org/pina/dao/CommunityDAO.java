package org.pina.dao;

import org.pina.model.Community;
import org.pina.model.Protein;

import java.sql.*;
import java.util.*;

public class CommunityDAO {

    private final Connection connection;

    public CommunityDAO(Connection connection) {
        this.connection = connection;
    }


    public void addOrUpdateCommunity(Community community) throws SQLException {
        String selectSql = "SELECT 1 FROM Community WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, community.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Update
                    String updateSql = "UPDATE Community SET functionalAnnotation = ? WHERE id = ?";
                    try (PreparedStatement ups = connection.prepareStatement(updateSql)) {
                        ups.setString(1, community.getFunctionalAnnotation());
                        ups.setString(2, community.getId());
                        ups.executeUpdate();
                    }
                } else {
                    // Insert
                    String insertSql = "INSERT INTO Community (id, functionalAnnotation) VALUES (?, ?)";
                    try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                        ips.setString(1, community.getId());
                        ips.setString(2, community.getFunctionalAnnotation());
                        ips.executeUpdate();
                    }
                }
            }
        }
    }


    public void linkProteinToCommunity(String communityId, String proteinId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO Community_Protein (community_id, protein_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, communityId);
            ps.setString(2, proteinId);
            ps.executeUpdate();
        }
    }


    public void deleteCommunity(String communityId) throws SQLException {
        String sql = "DELETE FROM Community_Protein WHERE community_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, communityId);
            ps.executeUpdate();
        }
        sql = "DELETE FROM Community WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, communityId);
            ps.executeUpdate();
        }
    }

}
