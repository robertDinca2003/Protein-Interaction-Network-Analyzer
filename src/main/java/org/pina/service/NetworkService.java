package org.pina.service;

import org.pina.dao.*;
import org.pina.model.Community;
import org.pina.model.HumanProtein;
import org.pina.model.Protein;

import java.sql.Connection;
import java.sql.SQLException;
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
import org.pina.model.ViralProtein;
//FILE formats
import java.nio.file.*;

public class NetworkService {

    private PPINetwork currentNetwork;
    private final Random random = new Random();
    private final Set<String> availableProteins = new HashSet<>();
    private final Set<String> fetchedProteins = new HashSet<>();
    Connection conn;
    ProteinDAO proteinDAO;

    InteractionDAO interactionDAO;

    CommunityDAO communityDAO;

    PPINetworkDAO ppnDAO;

    public NetworkService() throws SQLException {
        this.conn = DatabaseConnection.getInstance().getConnection();
        this.proteinDAO = new ProteinDAO(conn);
        this.interactionDAO = new InteractionDAO(conn);
        this.communityDAO = new CommunityDAO(conn);
        this.ppnDAO = new PPINetworkDAO(conn);

        // Load protein names from DB and add to availableProteins
        List<String> proteinNamesFromDb = proteinDAO.getAllProteinNames();
        this.availableProteins.addAll(proteinNamesFromDb);
    }


    // Methods:
    public void createNetwork() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        if(this.currentNetwork != null) {
            System.out.println("Do you want to save the current network? [Y/N]");
            if(scanner.nextLine().equalsIgnoreCase("Y")) {
                saveNetwork();
            }
        }
        System.out.print("Enter network name: ");
        String name = scanner.nextLine();
        HashSet<String> usedNames = new HashSet<String>(ppnDAO.listNetworkNames());
        if (usedNames.contains(name)) {
            System.out.println("Network already exists!");
            AuditService.INSTANCE.log("unsuccessfully_created_network|network_already_exists " );
            return;
        }
        currentNetwork = new PPINetwork(name);
        System.out.println("Network successfully created: " + currentNetwork.getNetworkName());
        AuditService.INSTANCE.log("created_network| " + currentNetwork.getNetworkName());
    }

    public void listNetworks() throws SQLException {
        System.out.println("Available networks:");
        List<String> networkNames =  ppnDAO.listNetworkNames();
        for(String networkName : networkNames) {
            System.out.println("- " + networkName);
        }
        if (networkNames.size() == 0) {
            System.out.println("No networks found");
        }
        AuditService.INSTANCE.log("listed_networks|"+networkNames.size());
    }
    public void getHubProteins() {
        List<Map.Entry<Protein,Integer>> hubProteins = currentNetwork.findHubProteins();
        for (Map.Entry<Protein,Integer> entry : hubProteins) {
            Protein protein = entry.getKey();
            int score = entry.getValue();
            System.out.println(protein.getName()+ ": score " + String.valueOf(score));
        }
        AuditService.INSTANCE.log("protein_hubs_retrieved|" + currentNetwork.getNetworkName());
    }

    public void addProtein() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("» Enter protein name: ");
        String proteinName = scanner.nextLine();

        Protein existingProteinDB = proteinDAO.retrieveProteinByNameOrNull(proteinName);
        if (existingProteinDB != null) {
            boolean alreadyExists = currentNetwork.addProtein(existingProteinDB);
//            System.out.println("Test, it worked");
            if(alreadyExists == false) {
                System.out.println("Protein already exists!");
            }
            AuditService.INSTANCE.log("added_to_network|"+ proteinName);
            return;
        }


        try {
            String apiUrl = "https://string-db.org/api/json/network?identifiers=" + proteinName
                    + "&species=9606&required_score=700&caller_identity=PINA";

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder().uri(URI.create(apiUrl)).build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) return;

            JsonArray interactions = JsonParser.parseString(response.body()).getAsJsonArray();
            boolean found = false;
            for (JsonElement element : interactions) {
                JsonObject interaction = element.getAsJsonObject();
                if(interaction.get("preferredName_A").getAsString().equals(proteinName)) {
                    Protein p1 = createProteinFromApiData(
                            interaction.get("stringId_A").getAsString(),
                            interaction.get("preferredName_A").getAsString(),
                            interaction.getAsJsonObject("proteinA_details")
                    );
                    found = true;
                    currentNetwork.addProtein(p1);
                    proteinDAO.addProteinIfNotExists(p1);
                    break;
                }

            }
            if (!found) {
                throw new Exception("Protein name not found");
            }


        } catch (Exception e) {
            System.err.println("Failed to fetch " + proteinName);
            return;
        }
        System.out.println("Added new protein");
        AuditService.INSTANCE.log("added_to_network|"+ proteinName);
    }

    public void removeProtein() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter protein name: ");
        String proteinName = scanner.nextLine();
        Protein targetProtein = currentNetwork.findProtein(proteinName);
        if(targetProtein == null) {
            System.out.println("Protein name not found");
            return;
        }
        currentNetwork.removeProtein(targetProtein);
        AuditService.INSTANCE.log("removed_from_network|"+proteinName);
    }

    public void getCommunities() {
        List<Community> communities =  currentNetwork.findCommunities();
        System.out.println("Communities found: " + communities.size());
        for (Community community : communities) {
            System.out.println("\n================\n");
            System.out.println(community);
            System.out.println("\n================\n");
        }
        AuditService.INSTANCE.log("communities_retrieved|" + currentNetwork.getNetworkName());
    }


    public void predictInteractions() {
        if (currentNetwork == null || currentNetwork.getProteins().size() < 2) {
            System.out.println("Need at least 2 proteins to predict interactions!");
            return;
        }

        try {
            List<String> proteinIds = currentNetwork.getProteins().stream()
                    .map(Protein::getUniprotId)
                    .toList();

            String apiUrl = "https://string-db.org/api/json/network?"
                    + "identifiers=" + String.join("%0d", proteinIds)
                    + "&species=9606"  // Human proteins
                    + "&required_score=700"  // Minimum confidence score 0.7
                    + "&caller_identity=PINA";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonArray interactions = JsonParser.parseString(response.body()).getAsJsonArray();
                int addedInteractions = processPredictedInteractions(interactions);

                System.out.println("Added " + addedInteractions + " new interactions!");
//                AuditService.INSTANCE.log("predict_interactions|added:" + addedInteractions);
            } else {
                System.out.println("API Error: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Prediction failed: " + e.getMessage());
        }
    }

    private int processPredictedInteractions(JsonArray interactions) {
        int addedCount = 0;

        for (JsonElement element : interactions) {
            JsonObject interaction = element.getAsJsonObject();

            String proteinAId = extractUniprotId(interaction.get("stringId_A").getAsString());
            String proteinBId = extractUniprotId(interaction.get("stringId_B").getAsString());
            double score = interaction.get("score").getAsDouble();

            Protein p1 = currentNetwork.findProteinById(proteinAId);
            Protein p2 = currentNetwork.findProteinById(proteinBId);

            if (p1 != null && p2 != null) {
                currentNetwork.addInteraction(p1, p2, score);
                addedCount++;
            }
        }
        return addedCount;
    }

    private String extractUniprotId(String stringId) {
        // Convert STRING-DB ID "9606.ENSP00000269305" to UniProt ID
        return stringId.substring(stringId.lastIndexOf('.') + 1);
    }

    public void saveNetwork() throws SQLException {
        ppnDAO.saveNetwork(currentNetwork);
        System.out.println("Successfully saved network");
        AuditService.INSTANCE.log("saved_network|" + currentNetwork.getNetworkName());
    }

    public void loadNetwork() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        if(this.currentNetwork != null) {
            System.out.println("Do you want to save the current network? [Y/N]");
            String answer = scanner.nextLine();
            if (answer.equalsIgnoreCase("Y")) {
                saveNetwork();
            }
        }
        System.out.println("Enter network name: ");
        String networkName = scanner.nextLine();
        this.currentNetwork = ppnDAO.loadNetwork(networkName);
        if(this.currentNetwork == null) {
            System.out.println("Network not found!");
            AuditService.INSTANCE.log("load_network|network_not_found" );
            return;
        }
        AuditService.INSTANCE.log("load_network|" + currentNetwork.getNetworkName());
    }


    @Deprecated
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
        AuditService.INSTANCE.log("fetched_new_proteins|" + fetchedProteins.size());
    }

    private String getRandomProtein() {
        List<String> available = new ArrayList<>(availableProteins);
        available.removeAll(fetchedProteins);
        return available.isEmpty() ? null : available.get(random.nextInt(available.size()));
    }

    private int fetchProteinFromAPI(String proteinQuery) {
        try {
            String apiUrl = "https://string-db.org/api/json/network?identifiers=" + proteinQuery
                    + "&species=9606&required_score=700&caller_identity=PINA";

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

                Protein p1 = createProteinFromApiData(
                        interaction.get("stringId_A").getAsString(),
                        interaction.get("preferredName_A").getAsString(),
                        interaction.getAsJsonObject("proteinA_details")
                );

                Protein p2 = createProteinFromApiData(
                        interaction.get("stringId_B").getAsString(),
                        interaction.get("preferredName_B").getAsString(),
                        interaction.getAsJsonObject("proteinA_details")
                );

                if (currentNetwork.addProtein(p1)) newCount++;
                if (currentNetwork.addProtein(p2)) newCount++;

                currentNetwork.addInteraction(p1, p2,
                        interaction.get("score").getAsDouble());
            }

            return newCount;

        } catch (Exception e) {
            System.err.println("Failed to fetch " + proteinQuery);
            return 0;
        }
    }

    private Protein createProteinFromApiData(String stringId, String name, JsonObject details) {
        try{
            String uniprotId = stringId.substring(stringId.lastIndexOf('.') + 1);

            if (details != null && details.has("taxon") && details.get("taxon").getAsString().equals("9606")) {
                String tissue = details.has("tissueExpression")
                        ? details.get("tissueExpression").getAsString()
                        : "Unknown";
                return new HumanProtein(uniprotId, name, "", new HashSet<>(), tissue);
            }
            else if (details != null && details.has("taxon") && details.get("taxon").getAsString().startsWith("virus")) {
                String host = details.has("host")
                        ? details.get("host").getAsString()
                        : "Unknown";
                return new ViralProtein(uniprotId, name, "", new HashSet<>(), host);
            }

            return new Protein(uniprotId, name, "", new HashSet<>());
        }catch (Exception e){
            e.printStackTrace();
            System.err.println("Failed to create protein from API data");
            return null;
        }

    }

    public void generateNetworkImage() {
        if (currentNetwork == null || currentNetwork.getProteins().isEmpty()) {
            System.out.println("No active network or network is empty!");
            return;
        }

        try {
            List<String> proteinIds = currentNetwork.getProteins().stream()
                    .map(Protein::getUniprotId)
                    .toList();

            String identifiersParam = String.join("%0d", proteinIds);

            String apiUrl = "https://string-db.org/api/image/network?"
                    + "identifiers=" + identifiersParam
                    + "&species=9606"
                    + "&caller_identity=PINA";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "image/png")
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                String fileName = currentNetwork.getNetworkName().replaceAll("\\s+", "_") + ".png";
                Path path = java.nio.file.Paths.get(fileName);
                Files.write(path, response.body());

                System.out.println("Network image saved as: " + fileName);
            } else {
                System.out.println("Failed to generate network image. API returned status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Error generating network image: " + e.getMessage());
            e.printStackTrace();
        }
        AuditService.INSTANCE.log("generate_network_image");
    }

    public void getScientificReference() throws SQLException {
        Scanner scanner  = new Scanner(System.in);
        System.out.println("Enter your saved protein name: ");
        String proteinName = scanner.nextLine();

        Protein protein = proteinDAO.retrieveProteinByNameOrNull(proteinName);

        if (protein == null) {
            System.out.println("Protein with name " + proteinName + " not found");
            return;
        }


        try {
            String query = "xref:" + protein.getUniprotId();

            String apiUrl = "https://rest.uniprot.org/uniprotkb/search"
                    + "?query="   + query
                    + "&fields=lit_pubmed_id";
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            HttpResponse<String> resp = client.send(
                    HttpRequest.newBuilder().uri(URI.create(apiUrl)).build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (resp.statusCode() != 200) {
                System.err.println("HTTP error: " + resp.statusCode());
                return;
            }

            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            JsonArray results = root.getAsJsonArray("results");
            if (results == null || results.size() == 0) {
                System.out.println("No entries found.");
                return;
            }

            for (JsonElement entryEl : results) {
                JsonObject entry = entryEl.getAsJsonObject();
                String accession = entry.get("primaryAccession").getAsString();
                System.out.println("=== UniProtKB:" + accession + " ===");

                JsonArray references = entry.getAsJsonArray("references");
                if (references == null) {
                    System.out.println("  (no references)");
                    continue;
                }

                for (JsonElement refEl : references) {
                    JsonObject citation = refEl.getAsJsonObject()
                            .getAsJsonObject("citation");

                    String title   = citation.has("title")   ? citation.get("title").getAsString()   : "N/A";
                    String journal = citation.has("journal") ? citation.get("journal").getAsString() : "N/A";

                    String doi = "N/A";
                    if (citation.has("citationCrossReferences")) {
                        JsonArray xrefs = citation.getAsJsonArray("citationCrossReferences");
                        for (JsonElement xrEl : xrefs) {
                            JsonObject xr = xrEl.getAsJsonObject();
                            if ("DOI".equalsIgnoreCase(xr.get("database").getAsString())) {
                                doi = xr.get("id").getAsString();
                                break;
                            }
                        }
                    }

                    System.out.printf(" • \"%s\" — %s (DOI:%s)%n", title, journal, doi);
                }
                System.out.println();
            }

            AuditService.INSTANCE.log("obtained_scientific_reference|"+proteinName);

        } catch (Exception e) {
            System.err.println("Error fetching citations: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void updateProteinData() throws SQLException {
        Scanner scanner  = new Scanner(System.in);
        System.out.println("Enter your saved protein name: ");
        String proteinName = scanner.nextLine();

        Protein protein = proteinDAO.retrieveProteinByNameOrNull(proteinName);

        if (protein == null) {
            System.out.println("Protein with name " + proteinName + " not found");
            return;
        }
        proteinDAO.deleteProteinByName(proteinName);

        System.out.println("Do you want to update protein name? [Y/N]: ");
        String doi = scanner.nextLine();
        if (doi.equalsIgnoreCase("Y")) {
            System.out.println("Enter new protein name: ");
            protein.setName(scanner.nextLine());
        }
        System.out.println("Do you want to update protein type? [Y/N]: ");
        String doi2 = scanner.nextLine();
        if (doi2.equalsIgnoreCase("Y")) {
            System.out.println("- Protein\n- Viral\n- Human\nEnter new protein type: ");
            String proteinType = scanner.nextLine();
            if (proteinType.equalsIgnoreCase("Human")) {
                System.out.println("Enter tissue expression of the human protein:");
                String humanExpression = scanner.nextLine();
                proteinDAO.addProteinIfNotExists(new HumanProtein(protein.getUniprotId(), protein.getName(), protein.getSequence(), protein.getFunctions(), humanExpression));
                return;
            }
            if (proteinType.equalsIgnoreCase("Viral")) {
                System.out.println("Enter host species of the viral protein: ");
                String species = scanner.nextLine();
                proteinDAO.addProteinIfNotExists(new ViralProtein(protein.getUniprotId(), protein.getName(), protein.getSequence(), protein.getFunctions(), species));
                return;
            }
            if (proteinType.equalsIgnoreCase("Protein")) {
                proteinDAO.addProteinIfNotExists(new Protein(protein.getUniprotId(), protein.getName(), protein.getSequence(), protein.getFunctions()));
            }
        }
        AuditService.INSTANCE.log("update_protein_data|"+proteinName);
        proteinDAO.addProteinIfNotExists(protein);
        this.currentNetwork = ppnDAO.loadNetwork(currentNetwork.getNetworkName());
    }

    public void deleteProteinFromDB() throws SQLException {
        Scanner scanner  = new Scanner(System.in);
        System.out.println("Enter your saved protein name to be deleted: ");
        String proteinName = scanner.nextLine();
        System.out.println("Do you want to delete the protein? [Y/N]: ");
        String doi = scanner.nextLine();
        if (doi.equalsIgnoreCase("Y")) {
            proteinDAO.deleteProteinByName(proteinName);
            System.out.println("Successfully deleted protein with name " + proteinName);
            AuditService.INSTANCE.log("deleted_protein_from_db|"+proteinName);

            this.currentNetwork = ppnDAO.loadNetwork(currentNetwork.getNetworkName());
        }
        if (doi.equalsIgnoreCase("N")) {
            AuditService.INSTANCE.log("deleted_protein_from_db|canceled");
            System.out.println("Successfully declined the request");
        }
    }

    public void deleteCommunity() throws SQLException {
        Scanner scanner  = new Scanner(System.in);
        System.out.println("Enter your community name: ");
        String communityName = scanner.nextLine();
        System.out.println("Do you want to delete the community? [Y/N]: ");
        String doi = scanner.nextLine();
        if (doi.equalsIgnoreCase("Y")) {
            communityDAO.deleteCommunity(communityName);
            this.currentNetwork = ppnDAO.loadNetwork(currentNetwork.getNetworkName());
            System.out.println("Successfully deleted community with name " + communityName);
            AuditService.INSTANCE.log("deleted_community_from_db|"+communityName);
        }
        if (doi.equalsIgnoreCase("N")) {
            System.out.println("Successfully declined the request");
        }
    }

    public void deleteInteraction() throws SQLException {
        Scanner scanner  = new Scanner(System.in);
        System.out.println("Enter first protein name: ");
        String proteinName1 = scanner.nextLine();
        System.out.println("Enter second protein name: ");
        String proteinName2 = scanner.nextLine();

        Protein protein1 = proteinDAO.retrieveProteinByNameOrNull(proteinName1);
        Protein protein2 = proteinDAO.retrieveProteinByNameOrNull(proteinName2);
        if (protein1 == null || protein2 == null) {
            System.out.println("Protein name not found");
        }
        if(interactionDAO.deleteInteraction(protein1.getUniprotId(), protein2.getUniprotId())){
            System.out.println("Successfully deleted interaction");
            this.currentNetwork = ppnDAO.loadNetwork(currentNetwork.getNetworkName());
            AuditService.INSTANCE.log("deleted_interaction");
        }
        else{
            System.out.println("Failed to delete interaction");
        }

    }

    public void exitApplication() throws SQLException {
        Scanner sc  = new Scanner(System.in);
        if(this.currentNetwork != null){
            System.out.println("Are you sure you want to exit without saving the current progress? [Y/N]");
            if (sc.nextLine().equalsIgnoreCase("Y")) {
                saveNetwork();
           }
        }
        System.out.println("Good  bye!");
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
            List<String> fetched = availableProteins.stream().toList();

            if (fetched.isEmpty()) {
                System.out.println("No protein available! (use option [4])");
            } else {
                fetched.forEach(p ->
                        System.out.println("- " + p)
                );
            }

      }
        System.out.println("\n» Network Options:");
        System.out.println("1. Create new network");
        System.out.println("2. List available networks");
        System.out.println("3. Load network");
        if (currentNetwork != null) {
            System.out.println("4. Save network");
            System.out.println("» Protein Options:");
            System.out.println("5. Add protein to network");
            System.out.println("6. Update protein information");
            System.out.println("7. Remove protein from network");
            System.out.println("8. Delete protein from database");
            System.out.println("9. Fetch random new proteins");
            System.out.println("10. Predict interactions");
            System.out.println("11. Delete interaction from network & database");
            System.out.println("» Research Options:");
            System.out.println("12. Generate network image");
            System.out.println("13. Show scientific references");
            System.out.println("14. Show hub proteins");
            System.out.println("15. Show communities");
            System.out.println("16. Delete community from network");

        }
        System.out.println("0. Exit");
        System.out.print("\nEnter choice: ");
    }
    public void runPINA() throws SQLException {
        int option = -1;
        Scanner scanner = new Scanner(System.in);
        AuditService.INSTANCE.log("run_pina");
        while (option != 0) {
            displayNetworkMenu();
            option = scanner.nextInt();
            switch (option) {
                case 0:
                    exitApplication();
                    break;
                case 1:
                    createNetwork();
                    break;
                case 2:
                    listNetworks();
                    break;
                case 3:
                    loadNetwork();
                    break;
                case 4:
                    saveNetwork();
                    break;
                case 5:
                    addProtein();
                    break;
                case 6:
                    updateProteinData();

                    break;
                case 7:
                    removeProtein();

                    break;
                case 8:
                    deleteProteinFromDB();

                    break;
                case 9:
                    fetchNewProteins();

                    break;
                case 10:
                    predictInteractions();

                    break;
                case 11:
                    deleteInteraction();

                    break;

                case 12:
                    generateNetworkImage();

                    break;
                case 13:
                    getScientificReference();

                    break;
                case 14:
                    getHubProteins();

                    break;
                case 15:
                    getCommunities();

                    break;
                case 16:
                    deleteCommunity();
                    break;

                default:
                    System.out.println("Invalid option");
                    break;
            }
        }
        AuditService.INSTANCE.log("exit_pina");
    }

}
