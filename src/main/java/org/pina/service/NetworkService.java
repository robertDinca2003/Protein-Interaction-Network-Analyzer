package org.pina.service;

import org.pina.model.Community;
import org.pina.model.Protein;

import java.util.*;
// HTTP Client
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
// JSON Parsing
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NetworkService {

    private PPINetwork currentNetwork;
    private final Random random = new Random();
    private final Set<String> availableProteins = new HashSet<>(Arrays.asList(
            "TP53", "BRCA1", "EGFR", "AKT1", "MYC", "VEGFA",
            "INS", "MAPK1", "JUN", "TNF", "IL6", "CDKN2A"
    ));
    private final Set<String> fetchedProteins = new HashSet<>();

    // Methods:
    public void createNetwork() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter network name: ");
        String name = scanner.nextLine();
        currentNetwork = new PPINetwork(name);
        System.out.println("Network successfully created: " + currentNetwork.getNetworkName());
    }
    public void getHubProteins() {
        List<Map.Entry<Protein,Integer>> hubProteins = currentNetwork.findHubProteins();
        for (Map.Entry<Protein,Integer> entry : hubProteins) {
            Protein protein = entry.getKey();
            int score = entry.getValue();
            System.out.println(protein.getName()+ ": score " + String.valueOf(score));
        }
    }

    public void addProtein(String proteinName) {
        currentNetwork.addProtein(new Protein("",proteinName,"", new HashSet<>()));
    }

    public void removeProtein(String proteinName) {
        currentNetwork.removeProtein(currentNetwork.findProtein(proteinName));
    }

    public void getCommunities() {
        List<Community> communities =  currentNetwork.findCommunities();
        System.out.println("Communities found: " + communities.size());
        for (Community community : communities) {
            System.out.println("\n================\n");
            System.out.println(community);
            System.out.println("\n================\n");
        }
    }

    public void predictInteractions() {  }

    public List<String> getSavedNetworks(){
        return new ArrayList<>();
    }

    public void saveNetwork() {}

    public void loadNetwork() {}

    public void loadSavedProtein(){

    }

    public void fetchNewProteins() {
        if (currentNetwork == null) {
            System.out.println("Error: No active network!");
            return;
        }


        int fetched = 0;
        while (fetched < 5 && !availableProteins.isEmpty()) {
            String protein = getRandomProtein();
            if (protein == null) break;

            int added = fetchProteinFromAPI(protein);
            if (added > 0) {
                fetchedProteins.add(protein);
                fetched++;
            }
        }
        System.out.println("Fetched " + fetched + " new proteins!");
    }

    private String getRandomProtein() {
        List<String> available = new ArrayList<>(availableProteins);
        available.removeAll(fetchedProteins);
        return available.isEmpty() ? null : available.get(random.nextInt(available.size()));
    }

    private int fetchProteinFromAPI(String proteinQuery) {
        try {
            String apiUrl = "https://string-db.org/api/json/network?identifiers=" + proteinQuery
                    + "&species=9606&required_score=700&caller_identity=your_project_name";

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder().uri(URI.create(apiUrl)).build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) return 0;

            JsonArray interactions = JsonParser.parseString(response.body()).getAsJsonArray();
            int newCount = 0;

            for (JsonElement element : interactions) {
                JsonObject interaction = element.getAsJsonObject();

                Protein p1 = createProtein(
                        interaction.get("stringId_A").getAsString(),
                        interaction.get("preferredName_A").getAsString()
                );

                Protein p2 = createProtein(
                        interaction.get("stringId_B").getAsString(),
                        interaction.get("preferredName_B").getAsString()
                );

                if (currentNetwork.addProtein(p1)) newCount++;
                if (currentNetwork.addProtein(p2)) newCount++;

                currentNetwork.addInteraction(p1, p2,
                        interaction.get("score").getAsDouble());
            }

//            AuditService.INSTANCE.log("fetch_protein|" + proteinQuery);
            return newCount;

        } catch (Exception e) {
            System.err.println("Failed to fetch " + proteinQuery);
            return 0;
        }
    }

    private Protein createProtein(String stringId, String name) {
        return new Protein(
                stringId.substring(stringId.lastIndexOf('.') + 1),
                name, "", new HashSet<>()
        );
    }

    public void generateNetworkImage(){

    }

    public void displayNetworkMenu() {
        String networkStatus = (currentNetwork != null)
                ? currentNetwork.getNetworkName()
                : "No Active Network";

        System.out.println("\n=== Network Menu: " + networkStatus + " ===");

        if (currentNetwork != null) {
            System.out.println("\n» Network Summary:");
            System.out.println(currentNetwork); // Calls toString()


            System.out.println("\n» Available Proteins:");
            List<Protein> fetched = currentNetwork.getProteins().stream()
                    .filter(p -> fetchedProteins.contains(p.getName()))
                    .toList();

            if (fetched.isEmpty()) {
                System.out.println("No protein available! (use option [4])");
            } else {
                fetched.forEach(p ->
                        System.out.println("- " + p.getName() + " (" + p.getUniprotId() + ")")
                );
            }

      }
        System.out.println("\n» Options:");
        System.out.println("1. Create new network");
        System.out.println("2. Load network");
        if (currentNetwork != null) {
            System.out.println("3. Save network");
            System.out.println("4. Fetch new proteins");
            System.out.println("5. Add custom protein");
            System.out.println("6. Remove protein");
            System.out.println("7. Predict interactions");
            System.out.println("8. Generate network image");
            System.out.println("9. Show hub proteins");
            System.out.println("10. Show communities");
        }
        System.out.println("0. Exit");
        System.out.print("\nEnter choice: ");
    }
    public void runPINA(){
        int option = -1;
        Scanner scanner = new Scanner(System.in);
        while (option != 0) {
            displayNetworkMenu();
            option = scanner.nextInt();
            switch (option) {
                case 0:
                    System.out.println("Good  bye!");
                    break;
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
