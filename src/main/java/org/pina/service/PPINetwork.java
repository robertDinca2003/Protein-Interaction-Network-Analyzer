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
    }

    public PPINetwork(String networkName, List<Protein> networkProteins, List<Interaction> interactions, Set<Community> communities, Map<Protein, List<Protein>> adjacencyList) {
        this.networkName = networkName;
        this.networkProteins = networkProteins;
        this.interactions = interactions;
        this.communities = communities;
        this.adjacencyList = adjacencyList;
    }

    public void addProtein(Protein protein) {
        if (!networkProteins.contains(protein)) {
            networkProteins.add(protein);
        }
        else{
            System.out.println("Already added protein");
        }
    }
    public void removeProtein(Protein protein) {
        networkProteins.remove(protein);
        for (Interaction interaction : interactions) {
            if (interaction.getProtein1().equals(protein) || interaction.getProtein2().equals(protein))
            {
                interactions.remove(protein);
            }
        }

        adjacencyList.remove(protein);
    }

    public void createInteractions() {
    }

    public List<Map.Entry<Protein, Integer>> findHubProteins() {
        List<Map.Entry<Protein, Integer>> hubProteins = new ArrayList<>();
        for (Protein protein : networkProteins) {
            List<Protein> neighbors = adjacencyList.get(protein);
            int degree = (neighbors != null) ? neighbors.size() : 0;
            hubProteins.add(new AbstractMap.SimpleEntry<>(protein, degree));
        }
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
}
