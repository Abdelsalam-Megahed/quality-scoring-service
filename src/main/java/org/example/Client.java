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

        OverallScoreResponse overallScore = stub.getOverallScore(
                Period.newBuilder()
                        .setStartDate("2019-02-25")
                        .setEndDate("2019-03-25")
                        .build()
        );

        OverallScoreResponse overallScoreChange = stub.getOverallScoreChange(
                PeriodRange.newBuilder()
                        .setStartDate("2019-02-25")
                        .setEndDate("2019-03-25")
                        .setSecondStartDate("2019-05-25")
                        .setSecondEndDate("2019-09-25")
                        .build()
        );

        System.out.println("Overall score is: " + overallScore.getScore() + "%");
        System.out.println("Overall score change is: " + overallScoreChange.getScore() + "%");

        channel.shutdown();
    }
}