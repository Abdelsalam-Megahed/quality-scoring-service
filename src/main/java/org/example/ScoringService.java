package org.example;

import org.baeldung.grpc.CategoryScore;
import org.baeldung.grpc.CategoryScoreResponse;
import org.baeldung.grpc.Date;
import org.baeldung.grpc.Week;
import org.example.models.Rating;
import org.example.utils.DateUtils;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

public class ScoringService {
    private final Connection connection;

    public ScoringService(Connection connection) {
        this.connection = connection;
    }

    public CategoryScoreResponse getAggregatedCategoryScore(String requestStartDate, String requestEndDate) {
        LocalDate startDate = LocalDate.parse(requestStartDate, DateUtils.getDateFormat());
        LocalDate endDate = LocalDate.parse(requestEndDate, DateUtils.getDateFormat());
        List<Rating> ratings = getRatingsFromDB(startDate, endDate);
        Map<String, List<Rating>> groupedRatings = ratings.stream().collect(Collectors.groupingBy(Rating::getCategory));
        List<CategoryScore> categoryScoreList = groupedRatings.entrySet().stream().map((ratingGroup) -> {
            List<Date> dates = ratingGroup.getValue().stream()
                    .map(rating -> Date.newBuilder()
                            .setDate(String.valueOf(rating.getCreatedAt()))
                            .setScore(calculateScore(rating.getWeight(), rating.getRating()))
                            .build())
                    .collect(Collectors.toList());
            List<Week> weeklyList = new ArrayList<>();
            List<Date> dailyList = new ArrayList<>();
            boolean isPeriodLongerThanOneMonth = DateUtils.isPeriodLongerThanOneMonth(startDate, endDate);

            if (isPeriodLongerThanOneMonth) {
                weeklyList = aggregateWeeklyScores(dates);
            } else {
                dailyList = aggregateDailyScores(dates);
            }

            int overallAverageScore = getAverageScore(dailyList);
            int overallRating = ratingGroup.getValue().stream()
                    .reduce(0, (total, rating) -> total + rating.getRating(), Integer::sum);

            return CategoryScore.newBuilder()
                    .setCategory(ratingGroup.getKey())
                    .setScore(overallAverageScore)
                    .setRatings(overallRating)
                    .addAllDates(isPeriodLongerThanOneMonth ? new ArrayList<>() : dailyList)
                    .addAllWeeks(weeklyList)
                    .build();
        }).collect(Collectors.toList());

        return CategoryScoreResponse.newBuilder()
                .addAllCategoryScoreList(categoryScoreList)
                .build();
    }

    private List<Date> aggregateDailyScores(List<Date> dates) {
        Map<String, List<Date>> groupedDatesByWeek = dates.stream().collect(
                Collectors.groupingBy(Date::getDate));

        return groupedDatesByWeek.entrySet().stream().map(dateGroup -> Date.newBuilder()
                .setDate(dateGroup.getKey())
                .setScore(getAverageScore(dateGroup.getValue()))
                .build()).collect(Collectors.toList());
    }

    private List<Week> aggregateWeeklyScores(List<Date> dates) {
        TemporalField weekOfYear = WeekFields.of(Locale.getDefault()).weekOfYear();

        Map<Integer, List<Date>> groupedDatesByWeek = dates.stream().collect(Collectors.groupingBy(
                date -> LocalDate.parse(date.getDate()).get(weekOfYear),
                LinkedHashMap::new,
                Collectors.toList()
        ));

        return groupedDatesByWeek.entrySet().stream().map(weekAndDates -> Week.newBuilder()
                        .setWeek(String.valueOf(weekAndDates.getKey()))
                        .setScore(getAverageScore(weekAndDates.getValue()))
                        .build())
//                .sorted(Comparator.comparing(Week::getWeek))
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
                .setCreatedAt(DateUtils.parseDate(resultSet.getString("created_at")));
    }
}
