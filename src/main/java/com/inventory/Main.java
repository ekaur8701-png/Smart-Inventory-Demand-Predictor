
package com.inventory;

import com.inventory.db.Database;
import com.inventory.service.SeedData;
import com.inventory.web.WebServer;

public class Main {
    public static void main(String[] args) throws Exception {
        Database.init();
        SeedData.loadIfEmpty();

        int port = getPort();
        new WebServer().start(port);
    }

    private static int getPort() {
        String value = System.getenv("PORT");

        if (value == null || value.trim().isEmpty()) {
            return 8080;
        }

        try {
            int port = Integer.parseInt(value.trim());
            if (port >= 1 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
            // Fall through to the default port.
        }

        return 8080;
    }
}
