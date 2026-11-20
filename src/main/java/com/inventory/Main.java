package com.inventory;

import com.inventory.db.Database;
import com.inventory.service.SeedData;
import com.inventory.web.WebServer;

public class Main {
    public static void main(String[] args) throws Exception {
        Database.init();
        SeedData.loadIfEmpty();
        new WebServer().start(8080);
    }
}
