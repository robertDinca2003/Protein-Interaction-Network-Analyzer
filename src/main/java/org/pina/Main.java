package org.pina;

import org.pina.service.AuditService;
import org.pina.service.NetworkService;

public class Main {
    public static void main(String[] args) {

        NetworkService networkService = new NetworkService();

        networkService.runPINA();

    }
}