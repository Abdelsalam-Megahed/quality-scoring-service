package com.klausapp.repositories;

import com.klausapp.utils.DateUtils;
import com.klausapp.models.Rating;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ScoringRepository {
    private final Connection connection;

    public ScoringRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Rating> getRatingsFromDB(LocalDate startDate, LocalDate endDate) {
        String query = String.format("SELECT ratings.ticket_id, rating_categories.name AS category, " +
                "rating_categories.weight AS weight, ratings.rating, substr(ratings.created_at, 1, 10) as created_at " +
                "FROM ratings LEFT JOIN rating_categories ON ratings.rating_category_id = rating_categories.id " +
                "WHERE (ratings.created_at BETWEEN DATE(\"%s\") AND DATE(\"%s\") AND rating_categories.weight > 0 AND rating NOT NULL )" +
                " GROUP BY Category, ratings.created_at ORDER BY ratings.created_at", startDate, endDate);
        List<Rating> ratings = new ArrayList<>();

        try (Statement stmt = connection.createStatement()) {
            ResultSet resultSet = stmt.executeQuery(query);

            while (resultSet.next()) {
                ratings.add(mapToRating(resultSet));
            }
        } catch (SQLException exception) {
            throw new Error(exception);
        }

        return ratings;
    }

    private Rating mapToRating(ResultSet resultSet) throws SQLException {
        return new Rating()
                .setTicketId(resultSet.getInt("ticket_id"))
                .setCategory(resultSet.getString("category"))
                .setRating(resultSet.getInt("rating"))
                .setWeight(resultSet.getFloat("weight"))
                .setCreatedAt(DateUtils.mapToLocalDate(resultSet.getString("created_at")));
    }
}
