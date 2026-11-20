
package com.inventory;

import com.inventory.db.Database;
import com.inventory.service.SeedData;
import com.inventory.web.WebServer;

public class Main {
    public static void main(String[] args) throws Exception {
        Database.init();
        SeedData.loadIfEmpty();

        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "8080")
        );

        new WebServer().start(port);
    }
}
