package org.example;

import org.baeldung.grpc.CategoryScore;
import org.baeldung.grpc.CategoryScoreResponse;
import org.baeldung.grpc.Date;
import org.baeldung.grpc.Week;
import org.example.models.Rating;
import org.example.repositry.ScoringRepositry;
import org.example.utils.DateUtils;

import java.time.LocalDate;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

public class ScoringService {
    private final ScoringRepositry scoringRepositry;

    public ScoringService(ScoringRepositry scoringRepositry) {
        this.scoringRepositry = scoringRepositry;
    }

    public CategoryScoreResponse getAggregatedCategoryScore(String start, String end) {
        LocalDate startDate = DateUtils.mapToLocalDate(start);
        LocalDate endDate = DateUtils.mapToLocalDate(end);
        List<Rating> ratings = scoringRepositry.getRatingsFromDB(startDate, endDate);
        Map<String, List<Rating>> groupedRatingsByCategory = ratings.stream().collect(Collectors.groupingBy(Rating::getCategory));
        List<CategoryScore> categoryScoreList = buildCategoryScoreList(groupedRatingsByCategory, startDate, endDate);

        return CategoryScoreResponse.newBuilder()
                .addAllCategoryScoreList(categoryScoreList)
                .build();
    }

    private List<CategoryScore> buildCategoryScoreList(Map<String, List<Rating>> groupedRatingsByCategory, LocalDate startDate, LocalDate endDate) {
        return groupedRatingsByCategory.entrySet().stream().map((ratingGroup) -> {
            List<Week> weeklyList = new ArrayList<>();
            List<Date> dailyList = new ArrayList<>();
            boolean isPeriodLongerThanOneMonth = DateUtils.isPeriodLongerThanOneMonth(startDate, endDate);
            List<Date> dates = ratingGroup.getValue().stream()
                    .map(rating -> Date.newBuilder()
                            .setDate(String.valueOf(rating.getCreatedAt()))
                            .setScore(calculateScore(rating.getWeight(), rating.getRating()))
                            .build())
                    .collect(Collectors.toList());

            if (isPeriodLongerThanOneMonth) {
                weeklyList = aggregateWeeklyScores(dates);
            } else {
                dailyList = aggregateDailyScores(dates);
            }

            int overallAverageScore = isPeriodLongerThanOneMonth
                    ? getAverageWeeklyScore(weeklyList)
                    : getAverageScore(dailyList);
            int overallRating = ratingGroup.getValue().stream()
                    .reduce(0, (total, rating) -> total + rating.getRating(), Integer::sum);

            return CategoryScore.newBuilder()
                    .setCategory(ratingGroup.getKey())
                    .setScore(overallAverageScore)
                    .setRatings(overallRating)
                    .addAllDates(dailyList)
                    .addAllWeeks(weeklyList)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<Date> aggregateDailyScores(List<Date> dates) {
        Map<String, List<Date>> groupedDatesByWeek = dates.stream().collect(
                Collectors.groupingBy(Date::getDate));

        return groupedDatesByWeek.entrySet().stream().map(dateGroup -> Date.newBuilder()
                        .setDate(dateGroup.getKey())
                        .setScore(getAverageScore(dateGroup.getValue()))
                        .build()).collect(Collectors.toList()).stream()
                .sorted(Comparator.comparing(Date::getDate))
                .collect(Collectors.toList());
    }

    private List<Week> aggregateWeeklyScores(List<Date> dates) {
        TemporalField weekOfYear = WeekFields.of(Locale.getDefault()).weekOfYear();
        Map<Integer, List<Date>> groupedDatesByWeek = dates.stream().collect(Collectors.groupingBy(
                date -> LocalDate.parse(date.getDate()).get(weekOfYear), //TODO: make sure it's group by week and YEAR
                LinkedHashMap::new,
                Collectors.toList()
        ));

        return groupedDatesByWeek.entrySet().stream().map(weekAndDates -> Week.newBuilder()
                        .setWeek(String.valueOf(weekAndDates.getKey()))
                        .setScore(getAverageScore(weekAndDates.getValue()))
                        .build())
                .collect(Collectors.toList());
    }

    private int calculateScore(float weight, int rating) {
        return Math.round(weight * rating * 20);
    }

    private int getAverageScore(List<Date> dates) {
        return (int) dates.stream()
                .mapToInt(Date::getScore)
                .average()
                .orElse(0);
    }

    private int getAverageWeeklyScore(List<Week> dates) {
        return (int) dates.stream()
                .mapToInt(Week::getScore)
                .average()
                .orElse(0);
    }
}
