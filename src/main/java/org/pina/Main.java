package org.pina;

import org.pina.service.NetworkService;
import java.sql.SQLException;
public class Main {
    public static void main(String[] args) throws SQLException {

        NetworkService networkService = new NetworkService();

        networkService.runPINA();

    }
}