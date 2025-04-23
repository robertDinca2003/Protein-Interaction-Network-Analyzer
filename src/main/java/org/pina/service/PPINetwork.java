package org.pina.service;
import org.graalvm.collections.Pair;
import org.pina.model.*;

import java.util.*;


public class PPINetwork {

    private String networkName;
    private List<Protein> networkProteins;

    private List<Interaction> interactions;
    private Map<Protein, List<Protein>> adjacencyList;
    private Set<Community> communities;

    public PPINetwork(String networkName) {
        this.networkName = networkName;
        networkProteins = new ArrayList<>();
        adjacencyList = new HashMap<>();
        communities = new HashSet<>();
        interactions = new ArrayList<>();
    }

    public PPINetwork(String networkName, List<Protein> networkProteins, List<Interaction> interactions, Set<Community> communities, Map<Protein, List<Protein>> adjacencyList) {
        this.networkName = networkName;
        this.networkProteins = networkProteins;
        this.interactions = interactions;
        this.communities = communities;
        this.adjacencyList = adjacencyList;
    }

    public boolean addProtein(Protein protein) {
        if (networkProteins.contains(protein)) {
//            System.out.println("Protein " + protein.getName() + " already exists!");
            return false;
        }
        networkProteins.add(protein);
        adjacencyList.put(protein, new ArrayList<>());
        return true;
    }


    public List<Protein> getProteins() {
        return new ArrayList<>(networkProteins);
    }

    public boolean containsProtein(String proteinName) {
        return networkProteins.stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(proteinName));
    }

    public int getInteractionCount() {
        return interactions.size();
    }

    public void removeProtein(Protein protein) {
        networkProteins.remove(protein);

        Iterator<Interaction> iterator = interactions.iterator();
        while (iterator.hasNext()) {
            Interaction interaction = iterator.next();
            if (interaction.getProtein1().equals(protein) || interaction.getProtein2().equals(protein)) {
                iterator.remove();
            }
        }

        adjacencyList.remove(protein);

        for (List<Protein> neighbors : adjacencyList.values()) {
            neighbors.remove(protein);
        }
    }


    public Protein findProteinById(String uniprotId) {
        return networkProteins.stream()
                .filter(p -> p.getUniprotId().equalsIgnoreCase(uniprotId))
                .findFirst()
                .orElse(null);
    }

    public void addInteraction(Protein p1, Protein p2, double score) {
        Interaction interaction = new Interaction(p1, p2, score);
        if (!interactions.contains(interaction)) {
            interactions.add(interaction);
            adjacencyList.get(p1).add(p2);
            adjacencyList.get(p2).add(p1);
        }
        AuditService.INSTANCE.log("interaction_added|"+p1.getName()+";"+p2.getName()+";"+score);
    }

    public List<Map.Entry<Protein, Integer>> findHubProteins() {
        List<Map.Entry<Protein, Integer>> hubProteins = new ArrayList<>();

        for (Protein protein : networkProteins) {
            List<Protein> neighbors = adjacencyList.getOrDefault(protein, Collections.emptyList());
            hubProteins.add(new AbstractMap.SimpleEntry<>(protein, neighbors.size()));
        }

        hubProteins.sort(
                Comparator.<Map.Entry<Protein, Integer>, Integer>comparing(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(e -> e.getKey().getName())
        );

        return hubProteins;
    }

    public Protein findProtein(String proteinName) {
        for (Protein protein : networkProteins) {
            if(protein.getName().equals(proteinName))
                return protein;
        }
        return null;
    }

    public List<Protein> findProteinCommunity(Protein protein) {
        List<Protein> proteins = new ArrayList<>();
        Queue<Protein> queue = new LinkedList<>();
        queue.add(protein);
        Set<Protein> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            Protein current = queue.poll();
            proteins.add(current);
            visited.add(current);
            List<Protein> neighbors = adjacencyList.get(current);
            for (Protein neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return proteins;
    }

    public List<Community> findCommunities() {
        Set<Protein> verified = new HashSet<>();
        List<List<Protein>> hubCommunities = new ArrayList<>();
        for (Protein protein : networkProteins) {
            if(!verified.contains(protein)) {
               List<Protein> proteins = findProteinCommunity(protein);
               hubCommunities.add(proteins);
               for (Protein neighbourProtein : proteins) {
                   verified.add(neighbourProtein);
               }
            }
        }
        Scanner scanner = new Scanner(System.in);
        List<Community> communities = new ArrayList<>();
        for (List<Protein> hubCommunity : hubCommunities) {
            Set<Protein> proteins = new HashSet<Protein>(hubCommunity);
            System.out.println("Enter community name: ");
            String communityName = scanner.nextLine();
            Community community = new Community(communityName,proteins,"");
            communities.add(community);
        }

        return communities;
    }


    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Network: ").append(networkName).append(" ===\n");

        sb.append("\n» Proteins (").append(networkProteins.size()).append("):\n");
        for (int i = 0; i <  networkProteins.size(); i++) {
            Protein p = networkProteins.get(i);
            sb.append("- ").append(p.getName())
                    .append(" (").append(p.getUniprotId()).append(")\n");
        }


        sb.append("\n» Interactions (").append(interactions.size()).append("):\n");
        for (int i = 0; i < Math.min(3, interactions.size()); i++) {
            Interaction interaction = interactions.get(i);
            sb.append("- ").append(interaction.getProtein1().getName())
                    .append(" ↔ ").append(interaction.getProtein2().getName())
                    .append(" (Score: ").append(String.format("%.2f", interaction.getConfidenceScore())).append(")\n");
        }
        if (interactions.size() > 3) {
            sb.append("- ...and ").append(interactions.size() - 3).append(" more\n");
        }

        return sb.toString();
    }
}
