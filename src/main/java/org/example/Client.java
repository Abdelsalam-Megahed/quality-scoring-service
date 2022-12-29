package org.example;


import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.grpc.*;

public class Client {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();

        ScoringServiceGrpc.ScoringServiceBlockingStub stub
                = ScoringServiceGrpc.newBlockingStub(channel);

        HelloResponse helloResponse = stub.hello(
                HelloRequest.newBuilder()
                        .setFirstName("karim")
                        .setLastName("gRPC")
                        .build()
        );

        OverallScoreResponse overallScore = stub.getOverallScore(
                Period.newBuilder()
                        .setStartDate("2019-02-25")
                        .setEndDate("2019-03-25")
                        .build()
        );

        System.out.println(helloResponse);
        System.out.println("Overall score is: " + overallScore.getScore() + "%");

        channel.shutdown();
    }
}