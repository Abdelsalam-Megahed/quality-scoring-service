package org.example;

import io.grpc.stub.StreamObserver;
import org.baeldung.grpc.*;

public class ScoringServiceImpl extends ScoringServiceGrpc.ScoringServiceImplBase {
    private final ScoringService scoringService;

    public ScoringServiceImpl(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @Override
    public void hello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        String greeting = "hello, " + request.getFirstName() + " " + request.getLastName();

        HelloResponse response = HelloResponse.newBuilder()
                .setGreeting(greeting)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getCategoryScore(Period request, StreamObserver<CategoryScoreResponse> responseObserver) {
        CategoryScoreResponse categoryScoreResponse = scoringService
                .getAggregatedCategoryScore(request.getStartDate(), request.getEndDate());

        responseObserver.onNext(categoryScoreResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getScoresByTicket(Period request, StreamObserver<ScoresByTicketResponse> responseObserver) {
        ScoresByTicketResponse scoresByTicketResponse = scoringService.getScoresByTicket(request.getStartDate(), request.getEndDate());

        responseObserver.onNext(scoresByTicketResponse);
        responseObserver.onCompleted();
    }
}
