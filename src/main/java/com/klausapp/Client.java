package com.klausapp;


import com.klausapp.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

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

        OverallScoreChangeResponse overallScoreChange = stub.getOverallScoreChange(
                PeriodRange.newBuilder()
                        .setStartDate("2019-02-25")
                        .setEndDate("2019-03-25")
                        .setSecondStartDate("2019-05-25")
                        .setSecondEndDate("2019-09-25")
                        .build()
        );

        System.out.println("Overall score is: " + overallScore.getScore() + "%");
        System.out.println("Overall score change is: " + overallScoreChange.getScoreChange() + "%");

        channel.shutdown();
    }
}