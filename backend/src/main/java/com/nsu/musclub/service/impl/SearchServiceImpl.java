package com.nsu.musclub.service.impl;

import com.nsu.musclub.dto.search.SearchEntityType;
import com.nsu.musclub.dto.search.SearchResultDto;
import com.nsu.musclub.exception.BadRequestException;
import com.nsu.musclub.repository.SearchRepository;
import com.nsu.musclub.service.EmbeddingService;
import com.nsu.musclub.service.SearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {
    private final SearchRepository searchRepository;
    private final EmbeddingService embeddingService;
    private final double lexicalWeight;
    private final double vectorWeight;
    private final double minVectorScore;

    public SearchServiceImpl(SearchRepository searchRepository,
                             EmbeddingService embeddingService,
                             @Value("${search.hybrid.lexical-weight:0.65}") double lexicalWeight,
                             @Value("${search.hybrid.vector-weight:0.35}") double vectorWeight,
                             @Value("${search.hybrid.min-vector-score:0.15}") double minVectorScore) {
        this.searchRepository = searchRepository;
        this.embeddingService = embeddingService;
        this.lexicalWeight = lexicalWeight;
        this.vectorWeight = vectorWeight;
        this.minVectorScore = minVectorScore;
    }

    @Override
    public Page<SearchResultDto> hybridSearch(String query, Set<SearchEntityType> types, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Поисковый запрос не может быть пустым", "EMPTY_SEARCH_QUERY");
        }
        String normalizedQuery = query.trim();
        String embeddingLiteral = toVectorLiteral(embeddingService.embed(normalizedQuery));
        return searchRepository.search(
                normalizedQuery,
                embeddingLiteral,
                types,
                pageable,
                lexicalWeight,
                vectorWeight,
                minVectorScore
        );
    }

    private static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}

