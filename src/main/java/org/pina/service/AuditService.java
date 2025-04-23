package org.pina.service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Paths;

public enum AuditService {
    INSTANCE;

    private static final String FILE_NAME = "audit_log.csv";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Synchronized to be thread-safe
    private boolean headerWritten = false;

    public synchronized void log(String action) {
        try {
            if (!headerWritten && !Files.exists(Paths.get(FILE_NAME))) {
                try (FileWriter fw = new FileWriter(FILE_NAME, true);
                     PrintWriter pw = new PrintWriter(fw)) {
                    pw.println("timestamp,action");
                }
                headerWritten = true;
            }

            try (FileWriter fw = new FileWriter(FILE_NAME, true);
                 PrintWriter pw = new PrintWriter(fw)) {

                String timestamp = LocalDateTime.now().format(FORMATTER);
                pw.printf("\"%s\",\"%s\"%n", timestamp, action);
            }
        } catch (IOException e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }
}
