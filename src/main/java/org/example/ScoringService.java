package org.example;

import org.baeldung.grpc.CategoryScore;
import org.baeldung.grpc.CategoryScoreResponse;
import org.baeldung.grpc.Date;
import org.example.models.Rating;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.DayOfWeek.MONDAY;

public class ScoringService {
    private final Connection connection;

    public ScoringService(Connection connection) {
        this.connection = connection;
    }

    public CategoryScoreResponse getAggregatedCategoryScore(String requestStartDate, String requestEndDate) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(requestStartDate, dateFormat);
        LocalDate endDate = LocalDate.parse(requestEndDate, dateFormat);

        List<Rating> ratings = getRatingsFromDB(startDate, endDate);

        Map<String, List<Rating>> groupedRatings = ratings.stream().collect(Collectors.groupingBy(Rating::getCategory));

        List<CategoryScore> categoryScoreList = groupedRatings.entrySet().stream().map((ratingGroup) -> {
            List<Date> dates = ratingGroup.getValue().stream()
                    .map(rating -> Date.newBuilder()
                            .setDate(String.valueOf(rating.getCreatedAt()))
                            .setScore(calculateScore(rating.getWeight(), rating.getRating()))
                            .build())
                    .collect(Collectors.toList());

            List<Date> finalDatesPerCategory = aggregateDailyScores(dates);

            if (isPeriodLongerThanOneMonth(startDate, endDate)) {
                aggregateWeeklyScores(finalDatesPerCategory);
            }

            int overallAverageScore = getAverageScore(finalDatesPerCategory);
            int overallRating = ratingGroup.getValue().stream()
                    .reduce(0, (total, rating) -> total + rating.getRating(), Integer::sum);

            return CategoryScore.newBuilder()
                    .setCategory(ratingGroup.getKey())
                    .setScore(overallAverageScore)
                    .setRatings(overallRating)
                    .addAllDates(finalDatesPerCategory)
                    .build();
        }).collect(Collectors.toList());

        return CategoryScoreResponse.newBuilder()
                .addAllCategoryScoreList(categoryScoreList)
                .build();
    }

    private List<Date> aggregateDailyScores(List<Date> dates) {
        Map<String, List<Date>> groupedDatesByWeek = dates.stream().collect(
                Collectors.groupingBy(date -> date.getDate()));

        return groupedDatesByWeek.entrySet().stream().map(dateGroup -> Date.newBuilder()
                .setDate(dateGroup.getKey())
                .setScore(getAverageScore(dateGroup.getValue()))
                .build()).collect(Collectors.toList());
    }

    private List<Date> aggregateWeeklyScores(List<Date> dates) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Date> weeklyDates = new ArrayList<>();
        Map<String, List<Integer>> map = new HashMap<>();

        for (Date dateObject : dates) {
            List<Integer> scores = new ArrayList<>();

            if (isFirstRatingOrStartOfWeek(dateObject.getDate())) {
//                map.put(dateObject.getDate(), scores.add(dateObject.getScore()));
            }

        }

        return weeklyDates;
    }

    private boolean isFirstRatingOrStartOfWeek(String date) {
        return date == null || isStartOfWeek(LocalDate.parse(date));
    }

    public boolean isPeriodLongerThanOneMonth(LocalDate startDate, LocalDate endDate) {
        return startDate.isBefore(endDate.minusMonths(1));
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

    private List<Rating> getRatingsFromDB(LocalDate startDate, LocalDate endDate) {
        String query = String.format("SELECT ratings.ticket_id, rating_categories.name AS category, rating_categories.weight AS weight, ratings.rating, substr(ratings.created_at, 1, 10) as created_at FROM ratings LEFT JOIN rating_categories ON ratings.rating_category_id = rating_categories.id WHERE (ratings.created_at BETWEEN DATE(\"%s\") AND DATE(\"%s\") AND rating_categories.weight > 0 AND rating NOT NULL ) GROUP BY Category, ratings.created_at ORDER BY ratings.created_at", startDate, endDate);
        List<Rating> ratings = new ArrayList<>();

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                ratings.add(convertToRatingFromRaw(rs));
            }
        } catch (SQLException exception) {
            throw new Error(exception);
        }

        return ratings;
    }

    private Rating convertToRatingFromRaw(ResultSet resultSet) throws SQLException {
        return new Rating()
                .setTicketId(resultSet.getInt("ticket_id"))
                .setCategory(resultSet.getString("category"))
                .setRating(resultSet.getInt("rating"))
                .setWeight(resultSet.getFloat("weight"))
                .setCreatedAt(parseDate(resultSet.getString("created_at")));
    }

    public static LocalDate parseDate(String dateString) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return LocalDate.parse(dateString, dateTimeFormatter);
    }

    public Boolean isStartOfWeek(LocalDate date) {
        return date.getDayOfWeek().equals(MONDAY);
    }
}
