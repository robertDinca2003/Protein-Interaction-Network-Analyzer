package org.pina.dao;

import org.pina.model.*;
import org.pina.service.*;

import java.sql.*;
import java.util.*;
public class PPINetworkDAO {

    private final Connection connection;
    private final ProteinDAO proteinDAO;
    private final InteractionDAO interactionDAO;
    private final CommunityDAO communityDAO;

    public PPINetworkDAO(Connection connection) {
        this.connection = connection;
        this.proteinDAO = new ProteinDAO(connection);
        this.interactionDAO = new InteractionDAO(connection);
        this.communityDAO = new CommunityDAO(connection);
    }

    public void saveNetwork(PPINetwork network) throws SQLException {
        connection.setAutoCommit(false);
        try {
            int networkId = insertOrGetNetworkId(network.getNetworkName());

            for (Protein p : network.getProteins()) {
                proteinDAO.addProteinIfNotExists(p);
                linkProteinToNetwork(networkId, p.getUniprotId());
            }

            for (Interaction interaction : network.getInteractions()) {
                int interactionId = interactionDAO.createOrGetInteractionId(interaction);
                linkInteractionToNetwork(networkId, interactionId);
            }

            for (Community community : network.getCommunities()) {
                communityDAO.addOrUpdateCommunity(community);
                linkCommunityToNetwork(networkId, community.getId());

                for (Protein p : community.getProteins()) {
                    communityDAO.linkProteinToCommunity(community.getId(), p.getUniprotId());
                }
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<String> listNetworkNames() throws SQLException {
        List<String> networkNames = new ArrayList<>();
        String selectSql = "SELECT name FROM Network";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    networkNames.add(rs.getString(1));
                }
            }
        }
        return networkNames;
    }

    public PPINetwork loadNetwork(String networkName) throws SQLException {
        int networkId = getNetworkIdByName(networkName);
        if (networkId == -1) return null;

        List<Protein> proteins = loadProteinsByNetworkId(networkId);

        List<Interaction> interactions = loadInteractionsByNetworkId(networkId, proteins);

        Map<Protein, List<Protein>> adjacencyList = buildAdjacencyList(proteins, interactions);

        Set<Community> communities = loadCommunitiesByNetworkId(networkId, proteins);

        return new PPINetwork(networkName, proteins, interactions, communities, adjacencyList);
    }


    private int insertOrGetNetworkId(String networkName) throws SQLException {
        String selectSql = "SELECT id FROM Network WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, networkName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        String insertSql = "INSERT INTO Network (name) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, networkName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                else throw new SQLException("Failed to insert network");
            }
        }
    }

    private void linkProteinToNetwork(int networkId, String proteinId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO Network_Protein (network_id, protein_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, networkId);
            ps.setString(2, proteinId);
            ps.executeUpdate();
        }
    }

    private void linkInteractionToNetwork(int networkId, int interactionId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO Network_Interaction (network_id, interaction_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, networkId);
            ps.setInt(2, interactionId);
            ps.executeUpdate();
        }
    }

    private void linkCommunityToNetwork(int networkId, String communityId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO Network_Community (network_id, community_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, networkId);
            ps.setString(2, communityId);
            ps.executeUpdate();
        }
    }

    private int getNetworkIdByName(String networkName) throws SQLException {
        String sql = "SELECT id FROM Network WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, networkName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
                else return -1;
            }
        }
    }

    private List<Protein> loadProteinsByNetworkId(int networkId) throws SQLException {
        String sql = """
            SELECT p.* FROM Protein p
            JOIN Network_Protein np ON p.uniprotId = np.protein_id
            WHERE np.network_id = ?
        """;
        List<Protein> proteins = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, networkId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    proteins.add(proteinDAO.mapResultSetToProtein(rs));
                }
            }
        }
        return proteins;
    }

    private List<Interaction> loadInteractionsByNetworkId(int networkId, List<Protein> proteins) throws SQLException {
        String sql = """
            SELECT i.* FROM Interaction i
            JOIN Network_Interaction ni ON i.id = ni.interaction_id
            WHERE ni.network_id = ?
        """;
        List<Interaction> interactions = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, networkId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String p1Id = rs.getString("protein1_id");
                    String p2Id = rs.getString("protein2_id");
                    double score = rs.getDouble("confidenceScore");
                    Protein p1 = findProteinById(proteins, p1Id);
                    Protein p2 = findProteinById(proteins, p2Id);
                    if (p1 != null && p2 != null) {
                        interactions.add(new Interaction(p1, p2, score));
                    }
                }
            }
        }
        return interactions;
    }

    private Protein findProteinById(List<Protein> proteins, String uniprotId) {
        for (Protein p : proteins) {
            if (p.getUniprotId().equals(uniprotId)) return p;
        }
        return null;
    }

    private Map<Protein, List<Protein>> buildAdjacencyList(List<Protein> proteins, List<Interaction> interactions) {
        Map<Protein, List<Protein>> adjacencyList = new HashMap<>();
        for (Protein p : proteins) {
            adjacencyList.put(p, new ArrayList<>());
        }
        for (Interaction i : interactions) {
            adjacencyList.get(i.getProtein1()).add(i.getProtein2());
            adjacencyList.get(i.getProtein2()).add(i.getProtein1());
        }
        return adjacencyList;
    }

    private Set<Community> loadCommunitiesByNetworkId(int networkId, List<Protein> proteins) throws SQLException {
        String sql = """
            SELECT c.* FROM Community c
            JOIN Network_Community nc ON c.id = nc.community_id
            WHERE nc.network_id = ?
        """;
        Set<Community> communities = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, networkId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String communityId = rs.getString("id");
                    String annotation = rs.getString("functionalAnnotation");
                    Set<Protein> communityProteins = loadProteinsByCommunityId(communityId, proteins);
                    communities.add(new Community(communityId, communityProteins, annotation));
                }
            }
        }
        return communities;
    }

    private Set<Protein> loadProteinsByCommunityId(String communityId, List<Protein> proteins) throws SQLException {
        String sql = """
            SELECT protein_id FROM Community_Protein WHERE community_id = ?
        """;
        Set<Protein> communityProteins = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, communityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String proteinId = rs.getString("protein_id");
                    Protein p = findProteinById(proteins, proteinId);
                    if (p != null) {
                        communityProteins.add(p);
                    }
                }
            }
        }
        return communityProteins;
    }
}
