package com.klausapp.services;

import com.klausapp.grpc.*;
import io.grpc.stub.StreamObserver;

public class ScoringServiceImpl extends ScoringServiceGrpc.ScoringServiceImplBase {
    private final ScoringService scoringService;

    public ScoringServiceImpl(ScoringService scoringService) {
        this.scoringService = scoringService;
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

    @Override
    public void getOverallScore(Period request, StreamObserver<OverallScoreResponse> responseObserver) {
        OverallScoreResponse overallScoreResponse = scoringService.getOverallScore(request.getStartDate(), request.getEndDate());

        responseObserver.onNext(overallScoreResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getOverallScoreChange(PeriodRange request, StreamObserver<OverallScoreChangeResponse> responseObserver) {
        OverallScoreChangeResponse overallScoreChangeResponse = scoringService
                .getOverallScoreChange(request.getStartDate(), request.getEndDate(), request.getSecondStartDate(), request.getSecondEndDate());

        responseObserver.onNext(overallScoreChangeResponse);
        responseObserver.onCompleted();
    }
}
