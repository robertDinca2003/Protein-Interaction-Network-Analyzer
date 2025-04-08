package org.pina.service;

import org.pina.model.Community;
import org.pina.model.Protein;

import java.util.*;

public class NetworkService {

    private PPINetwork currentNetwork;

    // Methods:
    public void createNetwork() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter network name: ");
        String name = scanner.nextLine();
        currentNetwork = new PPINetwork(name);
    }
    public List<Map.Entry<Protein,Integer>> getHubProteins() {
        return currentNetwork.findHubProteins();
    }

    public void addProtein(String proteinName) {
        currentNetwork.addProtein(new Protein("",proteinName,"", new HashSet<>()));
    }

    public void removeProtein(String proteinName) {
        currentNetwork.removeProtein(currentNetwork.findProtein(proteinName));
    }

    public List<Community> getCommunities() {
        return currentNetwork.findCommunities();
    }

    public void predictInteractions() {  }

    public List<String> getSavedNetworks(){
        return new ArrayList<>();
    }

    public void saveNetwork() {}

    public void loadNetwork() {}

    public void loadSavedProtein(){

    }

    public List<Protein> fetchNewProteins() {
        List<Protein> proteins = new ArrayList<>();
        // api call
        return proteins;
    }
    public void generateNetworkImage(){

    }

    public void displayNetwork() {

    }

    public void runPINA(){
        int option = -1;
        Scanner scanner = new Scanner(System.in);
        while (option != 0) {
            displayNetwork();
            System.out.print("Enter option: ");
            option = scanner.nextInt();
            switch (option) {
                case 1:
                    createNetwork();
                    break;
                case 2:
                    loadNetwork();
                    break;
                case 3:
                    saveNetwork();
                    break;
                case 4:
                    fetchNewProteins();
                    break;
                case 5:
                    addProtein("Protein");
                    break;
                case 6:
                    removeProtein("Protein");
                    break;
                case 7:
                    predictInteractions();
                    break;
                case 8:
                    generateNetworkImage();
                    break;
                case 9:
                    getHubProteins();
                    break;
                case 10:
                    getCommunities();
                    break;
                default:
                    System.out.println("Invalid option");
                    break;
            }
        }
    }

}
