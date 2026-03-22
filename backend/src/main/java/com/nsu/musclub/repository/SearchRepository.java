package com.nsu.musclub.repository;

import com.nsu.musclub.dto.search.SearchEntityType;
import com.nsu.musclub.dto.search.SearchResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
public class SearchRepository {
    private static final String SEARCH_SQL = """
            SELECT
                entity_type,
                entity_id,
                title,
                left(content_text, 280) AS snippet,
                lexical_score,
                vector_score,
                CASE 
                    WHEN lexical_score > 0 AND vector_score >= :minVectorScore THEN
                        (:lexicalWeight * lexical_score + :vectorWeight * vector_score)
                    WHEN lexical_score > 0 THEN
                        :lexicalWeight * lexical_score
                    ELSE
                        :vectorWeight * vector_score
                END AS score
            FROM (
                SELECT
                    sd.entity_type,
                    sd.entity_id,
                    sd.title,
                    sd.content_text,
                    CASE
                        WHEN sd.content_tsv @@ plainto_tsquery('simple', :query) THEN
                            ts_rank_cd(sd.content_tsv, plainto_tsquery('simple', :query))
                        ELSE
                            CASE
                                WHEN sd.content_text ILIKE :queryLike THEN 0.8
                                ELSE 0.0
                            END
                    END AS lexical_score,
                    CASE
                        WHEN sd.embedding IS NULL THEN 0.0
                        ELSE GREATEST(0.0, 1 - (sd.embedding <=> CAST(:embedding AS vector)))
                    END AS vector_score,
                    sd.updated_at
                FROM search_documents sd
                WHERE (:typesEmpty = TRUE OR sd.entity_type IN (:types))
                  AND (
                      sd.content_tsv @@ plainto_tsquery('simple', :query)
                      OR sd.content_text ILIKE :queryLike
                      OR sd.embedding IS NOT NULL
                  )
            ) ranked
            WHERE ranked.lexical_score > 0
               OR ranked.vector_score >= :minVectorScore
            ORDER BY score DESC, updated_at DESC
            LIMIT :limit OFFSET :offset
            """;

    private static final String COUNT_SQL = """
            SELECT COUNT(*)
            FROM (
                SELECT
                    CASE
                        WHEN sd.content_tsv @@ plainto_tsquery('simple', :query) THEN
                            ts_rank_cd(sd.content_tsv, plainto_tsquery('simple', :query))
                        ELSE
                            CASE
                                WHEN sd.content_text ILIKE :queryLike THEN 0.8
                                ELSE 0.0
                            END
                    END AS lexical_score,
                    CASE
                        WHEN sd.embedding IS NULL THEN 0.0
                        ELSE GREATEST(0.0, 1 - (sd.embedding <=> CAST(:embedding AS vector)))
                    END AS vector_score
                FROM search_documents sd
                WHERE (:typesEmpty = TRUE OR sd.entity_type IN (:types))
                  AND (
                      sd.content_tsv @@ plainto_tsquery('simple', :query)
                      OR sd.content_text ILIKE :queryLike
                      OR sd.embedding IS NOT NULL
                  )
            ) ranked
            WHERE ranked.lexical_score > 0
               OR ranked.vector_score >= :minVectorScore
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO search_documents (entity_type, entity_id, title, content_text, embedding, updated_at)
            VALUES (:entityType, :entityId, :title, :contentText, CAST(:embedding AS vector), now())
            ON CONFLICT (entity_type, entity_id) DO UPDATE SET
                title = EXCLUDED.title,
                content_text = EXCLUDED.content_text,
                embedding = EXCLUDED.embedding,
                updated_at = now()
            """;

    private static final String DELETE_SQL = """
            DELETE FROM search_documents
            WHERE entity_type = :entityType AND entity_id = :entityId
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public SearchRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Page<SearchResultDto> search(String query,
                                        String embeddingLiteral,
                                        Set<SearchEntityType> types,
                                        Pageable pageable,
                                        double lexicalWeight,
                                        double vectorWeight,
                                        double minVectorScore) {
        List<String> typeValues = types == null
                ? Collections.emptyList()
                : types.stream().filter(Objects::nonNull).map(Enum::name).toList();

        // Подготавливаем query для ILIKE поиска (частичное совпадение)
        String queryLike = "%" + query.replace("'", "''") + "%";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("query", query)
                .addValue("queryLike", queryLike)
                .addValue("embedding", embeddingLiteral)
                .addValue("types", typeValues.isEmpty() ? List.of("EVENT", "USER") : typeValues)
                .addValue("typesEmpty", typeValues.isEmpty())
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset())
                .addValue("lexicalWeight", lexicalWeight)
                .addValue("vectorWeight", vectorWeight)
                .addValue("minVectorScore", minVectorScore);

        List<SearchResultDto> content = jdbc.query(SEARCH_SQL, params, (rs, rowNum) -> {
            SearchResultDto dto = new SearchResultDto();
            dto.setEntityType(SearchEntityType.valueOf(rs.getString("entity_type")));
            dto.setEntityId(rs.getLong("entity_id"));
            dto.setTitle(rs.getString("title"));
            dto.setSnippet(rs.getString("snippet"));
            dto.setLexicalScore(rs.getDouble("lexical_score"));
            dto.setVectorScore(rs.getDouble("vector_score"));
            dto.setScore(rs.getDouble("score"));
            return dto;
        });

        Long total = jdbc.queryForObject(COUNT_SQL, params, Long.class);
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    public void upsertDocument(SearchEntityType type,
                               Long entityId,
                               String title,
                               String contentText,
                               String embeddingLiteral) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("entityType", type.name())
                .addValue("entityId", entityId)
                .addValue("title", title == null ? "" : title)
                .addValue("contentText", contentText == null ? "" : contentText)
                .addValue("embedding", embeddingLiteral);
        jdbc.update(UPSERT_SQL, params);
    }

    public void deleteDocument(SearchEntityType type, Long entityId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("entityType", type.name())
                .addValue("entityId", entityId);
        jdbc.update(DELETE_SQL, params);
    }
}

