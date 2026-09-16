package com.thelifestyle.integration.adapter.out.persistence;

import com.thelifestyle.integration.application.port.out.AiSuggestionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAiSuggestionRepository implements AiSuggestionRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAiSuggestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveDraft(UUID id, String query, List<String> suggestions, String provider,
                          String model, String promptVersion, Instant generatedAt) {
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO integration.ai_suggestion
                    (id, query_text, suggestions, provider, model, prompt_version, generated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """);
            statement.setObject(1, id);
            statement.setString(2, query);
            statement.setArray(3, connection.createArrayOf("text", suggestions.toArray(String[]::new)));
            statement.setString(4, provider);
            statement.setString(5, model);
            statement.setString(6, promptVersion);
            statement.setTimestamp(7, Timestamp.from(generatedAt));
            return statement;
        });
    }
}
