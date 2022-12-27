package org.example;

import io.grpc.ServerBuilder;
import org.example.repositry.ScoringRepositry;
import org.example.services.ScoringService;
import org.example.services.ScoringServiceImpl;

import java.sql.Connection;
import java.sql.DriverManager;

public class Server {
    public static void main(String[] args) throws Exception {
        String connectionString = "jdbc:sqlite:database.db";

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            ScoringRepositry scoringRepositry = new ScoringRepositry(connection);
            ScoringService scoringService = new ScoringService(scoringRepositry);

            io.grpc.Server server = ServerBuilder
                    .forPort(8080)
                    .addService(new ScoringServiceImpl(scoringService)).build();

            server.start();
            server.awaitTermination();
        } catch (Exception exception) {
            throw new Exception(exception.getMessage());
        }
    }
}