package com.klausapp.services;

import com.klausapp.grpc.*;
import com.klausapp.grpc.DateScoreObject;
import com.klausapp.repositories.ScoringRepository;
import com.klausapp.utils.DateUtils;
import com.klausapp.models.Rating;

import java.time.LocalDate;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

public class ScoringService {
    private final ScoringRepository scoringRepository;

    public ScoringService(ScoringRepository scoringRepository) {
        this.scoringRepository = scoringRepository;
    }

    public CategoryScoreResponse getAggregatedCategoryScore(String start, String end) {
        LocalDate startDate = DateUtils.mapToLocalDate(start);
        LocalDate endDate = DateUtils.mapToLocalDate(end);
        List<Rating> ratings = scoringRepository.getRatings(startDate, endDate);
        Map<String, List<Rating>> groupedRatingsByCategory = ratings.stream().collect(Collectors.groupingBy(Rating::getCategory));
        List<CategoryScore> categoryScoreList = buildCategoryScoreList(groupedRatingsByCategory, startDate, endDate);

        return CategoryScoreResponse.newBuilder()
                .addAllCategoryScoreList(categoryScoreList)
                .build();
    }

    public ScoresByTicketResponse getScoresByTicket(String start, String end) {
        LocalDate startDate = DateUtils.mapToLocalDate(start);
        LocalDate endDate = DateUtils.mapToLocalDate(end);
        List<Rating> ratings = scoringRepository.getRatings(startDate, endDate);
        Map<Integer, List<Rating>> groupedRatingsByTicketId = ratings.stream().collect(Collectors.groupingBy(Rating::getTicketId));
        List<ScoresByTicket> scoresByTicketList = buildScoresByTicketList(groupedRatingsByTicketId);

        return ScoresByTicketResponse.newBuilder()
                .addAllScoresByTicketList(scoresByTicketList)
                .build();
    }

    public OverallScoreResponse getOverallScore(String start, String end) {
        LocalDate startDate = DateUtils.mapToLocalDate(start);
        LocalDate endDate = DateUtils.mapToLocalDate(end);
        int score = calculateScoreFromRatings(startDate, endDate);

        return OverallScoreResponse.newBuilder()
                .setScore(score)
                .build();
    }

    public OverallScoreChangeResponse getOverallScoreChange(String start, String end, String secondStart, String secondEnd) {
        LocalDate startDate = DateUtils.mapToLocalDate(start);
        LocalDate endDate = DateUtils.mapToLocalDate(end);
        LocalDate secondStartDate = DateUtils.mapToLocalDate(secondStart);
        LocalDate secondEndDate = DateUtils.mapToLocalDate(secondEnd);
        int firstPeriodScore = calculateScoreFromRatings(startDate, endDate);
        int secondPeriodScore = calculateScoreFromRatings(secondStartDate, secondEndDate);

        return OverallScoreChangeResponse.newBuilder()
                .setScoreChange(calculateScoreChange(firstPeriodScore, secondPeriodScore))
                .build();
    }

    private int calculateScoreFromRatings(LocalDate startDate, LocalDate endDate) {
        List<Rating> ratings = scoringRepository.getRatings(startDate, endDate);
        List<DateScoreObject> dateScoreList = ratings.stream()
                .map(rating -> DateScoreObject.newBuilder()
                        .setDate(rating.getCreatedAt().toString())
                        .setScore(calculateScore(rating.getWeight(), rating.getRating()))
                        .build())
                .collect(Collectors.toList());

        return calculateAggregatedScore(dateScoreList);
    }

    private List<ScoresByTicket> buildScoresByTicketList(Map<Integer, List<Rating>> groupedRatingsByTicketId) {
        return groupedRatingsByTicketId.entrySet().stream().map((group) -> {
            List<Category> categoriesScoresList = group.getValue().stream().map(rating -> Category.newBuilder()
                    .setCategory(rating.getCategory())
                    .setScore(calculateScore(rating.getWeight(), rating.getRating()))
                    .build()).collect(Collectors.toList());

            return ScoresByTicket.newBuilder()
                    .setTicketId(group.getKey())
                    .addAllCategories(categoriesScoresList)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<CategoryScore> buildCategoryScoreList(Map<String, List<Rating>> groupedRatingsByCategory, LocalDate startDate, LocalDate endDate) {
        return groupedRatingsByCategory.entrySet().stream().map((ratingGroup) -> {
            List<WeekScoreObject> weeklyList = new ArrayList<>();
            List<DateScoreObject> dailyList = new ArrayList<>();
            boolean isPeriodLongerThanOneMonth = DateUtils.isPeriodLongerThanOneMonth(startDate, endDate);
            List<DateScoreObject> dateScoreList = ratingGroup.getValue().stream()
                    .map(rating -> DateScoreObject.newBuilder()
                            .setDate(rating.getCreatedAt().toString())
                            .setScore(calculateScore(rating.getWeight(), rating.getRating()))
                            .build())
                    .collect(Collectors.toList());

            if (isPeriodLongerThanOneMonth) {
                weeklyList = getAggregatedWeeklyList(dateScoreList);
            } else {
                dailyList = getAggregatedDailyList(dateScoreList);
            }

            int aggregatedScore = isPeriodLongerThanOneMonth
                    ? calculateAggregatedWeeklyScore(weeklyList)
                    : calculateAggregatedScore(dailyList);
            int aggregatedRating = getAggregatedRating(ratingGroup.getValue());

            return CategoryScore.newBuilder()
                    .setCategory(ratingGroup.getKey())
                    .setScore(aggregatedScore)
                    .setRatings(aggregatedRating)
                    .addAllDates(dailyList)
                    .addAllWeeks(weeklyList)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<DateScoreObject> getAggregatedDailyList(List<DateScoreObject> dates) {
        Map<String, List<DateScoreObject>> groupedDatesByWeek = dates.stream().collect(Collectors.groupingBy(DateScoreObject::getDate));

        return groupedDatesByWeek.entrySet().stream().map(dateGroup -> DateScoreObject.newBuilder()
                        .setDate(dateGroup.getKey())
                        .setScore(calculateAggregatedScore(dateGroup.getValue()))
                        .build()).collect(Collectors.toList()).stream()
                .sorted(Comparator.comparing(DateScoreObject::getDate))
                .collect(Collectors.toList());
    }

    private List<WeekScoreObject> getAggregatedWeeklyList(List<DateScoreObject> dates) {
        TemporalField weekOfYear = WeekFields.of(Locale.getDefault()).weekOfYear();

        Map<Integer, Map<Integer, List<DateScoreObject>>> groupedDatesByYearAndWeek = dates.stream().collect(
                Collectors.groupingBy(
                        date -> LocalDate.parse(date.getDate()).getYear(), //group by year
                        Collectors.groupingBy(date -> LocalDate.parse(date.getDate()).get(weekOfYear)) //group by week
                )
        );

        return groupedDatesByYearAndWeek.entrySet().stream().flatMap(yearAndWeeks -> yearAndWeeks.getValue()
                .entrySet().stream().map(weekAndDates -> WeekScoreObject.newBuilder()
                        .setWeek(weekAndDates.getKey().toString())
                        .setYear(yearAndWeeks.getKey().toString())
                        .setScore(calculateAggregatedScore(weekAndDates.getValue()))
                        .build())
        ).collect(Collectors.toList());
    }

    private int calculateScore(float weight, int rating) {
        return Math.round(weight * rating * 15);
    }

    private int calculateScoreChange(int firstScore, int secondScore) {
        return 100 * (firstScore - secondScore) / secondScore;
    }

    private Integer getAggregatedRating(List<Rating> ratingGroup) {
        return ratingGroup.stream()
                .reduce(0, (total, rating) -> total + rating.getRating(), Integer::sum);
    }

    private int calculateAggregatedScore(List<DateScoreObject> dateScoreList) {
        if (dateScoreList.isEmpty()) {
            return 0;
        }

        int totalScore = dateScoreList.stream().reduce(0,
                (total, dateScoreObject) -> total + dateScoreObject.getScore(), Integer::sum);

        return totalScore / dateScoreList.size();
    }

    private int calculateAggregatedWeeklyScore(List<WeekScoreObject> weekScoreList) {
        if (weekScoreList.isEmpty()) {
            return 0;
        }

        int totalScore = weekScoreList.stream().reduce(0,
                (total, dateScoreObject) -> total + dateScoreObject.getScore(), Integer::sum);

        return totalScore / weekScoreList.size();
    }
}
