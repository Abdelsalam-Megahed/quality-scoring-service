package com.klausapp.services;

import com.klausapp.grpc.*;
import com.klausapp.grpc.Date;
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
        List<Rating> ratings = scoringRepository.getRatingsFromDB(startDate, endDate);
        Map<String, List<Rating>> groupedRatingsByCategory = ratings.stream().collect(Collectors.groupingBy(Rating::getCategory));
        List<CategoryScore> categoryScoreList = buildCategoryScoreList(groupedRatingsByCategory, startDate, endDate);

        return CategoryScoreResponse.newBuilder()
                .addAllCategoryScoreList(categoryScoreList)
                .build();
    }

    public ScoresByTicketResponse getScoresByTicket(String start, String end) {
        LocalDate startDate = DateUtils.mapToLocalDate(start);
        LocalDate endDate = DateUtils.mapToLocalDate(end);
        List<Rating> ratings = scoringRepository.getRatingsFromDB(startDate, endDate);
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
        List<Rating> ratings = scoringRepository.getRatingsFromDB(startDate, endDate);
        List<Date> datesAndScoresList = ratings.stream()
                .map(rating -> Date.newBuilder()
                        .setDate(rating.getCreatedAt().toString())
                        .setScore(calculateScore(rating.getWeight(), rating.getRating()))
                        .build())
                .collect(Collectors.toList());

        return getAggregatedDailyScore(datesAndScoresList);
    }

    private List<ScoresByTicket> buildScoresByTicketList(Map<Integer, List<Rating>> groupedRatingsByTicketId) {
        return groupedRatingsByTicketId.entrySet().stream().map((group) -> {
            List<Category> categoriesAndScoresList = group.getValue().stream().map(rating -> Category.newBuilder()
                    .setCategory(rating.getCategory())
                    .setScore(calculateScore(rating.getWeight(), rating.getRating()))
                    .build()).collect(Collectors.toList());

            return ScoresByTicket.newBuilder()
                    .setTicketId(group.getKey())
                    .addAllCategories(categoriesAndScoresList)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<CategoryScore> buildCategoryScoreList(Map<String, List<Rating>> groupedRatingsByCategory, LocalDate startDate, LocalDate endDate) {
        return groupedRatingsByCategory.entrySet().stream().map((ratingGroup) -> {
            List<Week> weeklyList = new ArrayList<>();
            List<Date> dailyList = new ArrayList<>();
            boolean isPeriodLongerThanOneMonth = DateUtils.isPeriodLongerThanOneMonth(startDate, endDate);
            List<Date> datesAndScoresList = ratingGroup.getValue().stream()
                    .map(rating -> Date.newBuilder()
                            .setDate(rating.getCreatedAt().toString())
                            .setScore(calculateScore(rating.getWeight(), rating.getRating()))
                            .build())
                    .collect(Collectors.toList());

            if (isPeriodLongerThanOneMonth) {
                weeklyList = getAggregatedWeeklyList(datesAndScoresList);
            } else {
                dailyList = getAggregatedDailyList(datesAndScoresList);
            }

            int aggregatedScore = isPeriodLongerThanOneMonth
                    ? getAggregatedWeeklyScore(weeklyList)
                    : getAggregatedDailyScore(dailyList);
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

    private List<Date> getAggregatedDailyList(List<Date> dates) {
        Map<String, List<Date>> groupedDatesByWeek = dates.stream().collect(Collectors.groupingBy(Date::getDate));

        return groupedDatesByWeek.entrySet().stream().map(dateGroup -> Date.newBuilder()
                        .setDate(dateGroup.getKey())
                        .setScore(getAggregatedDailyScore(dateGroup.getValue()))
                        .build()).collect(Collectors.toList()).stream()
                .sorted(Comparator.comparing(Date::getDate))
                .collect(Collectors.toList());
    }

    private List<Week> getAggregatedWeeklyList(List<Date> dates) {
        TemporalField weekOfYear = WeekFields.of(Locale.getDefault()).weekOfYear();

        Map<Integer, Map<Integer, List<Date>>> groupedDatesByYearAndWeek = dates.stream().collect(
                Collectors.groupingBy(
                        date -> LocalDate.parse(date.getDate()).getYear(),
                        Collectors.groupingBy(date -> LocalDate.parse(date.getDate()).get(weekOfYear))
                )
        );

        return groupedDatesByYearAndWeek.entrySet().stream().flatMap(yearAndWeeks -> yearAndWeeks.getValue()
                .entrySet().stream().map(weekAndDates -> Week.newBuilder()
                        .setWeek(weekAndDates.getKey().toString())
                        .setYear(yearAndWeeks.getKey().toString())
                        .setScore(getAggregatedDailyScore(weekAndDates.getValue()))
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

    private int getAggregatedDailyScore(List<Date> datesAndScoresList) {
        int totalScore = datesAndScoresList.stream().reduce(0,
                (total, dateAndScore) -> total + dateAndScore.getScore(), Integer::sum);

        return totalScore / datesAndScoresList.size();
    }

    private int getAggregatedWeeklyScore(List<Week> weeksAndScoresList) {
        int totalScore = weeksAndScoresList.stream().reduce(0,
                (total, dateAndScore) -> total + dateAndScore.getScore(), Integer::sum);

        return totalScore / weeksAndScoresList.size();
    }
}
