package org.example;


import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.baeldung.grpc.HelloRequest;
import org.baeldung.grpc.HelloResponse;
import org.baeldung.grpc.ScoringServiceGrpc;

public class Client {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();

        ScoringServiceGrpc.ScoringServiceBlockingStub stub
                = ScoringServiceGrpc.newBlockingStub(channel);

        HelloResponse helloResponse = stub.hello(HelloRequest.newBuilder()
                .setFirstName("karim")
                .setLastName("gRPC")
                .build());

        System.out.println(helloResponse);

        channel.shutdown();
    }
}