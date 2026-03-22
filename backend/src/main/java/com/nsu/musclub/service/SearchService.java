package com.nsu.musclub.service;

import com.nsu.musclub.dto.search.SearchEntityType;
import com.nsu.musclub.dto.search.SearchResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface SearchService {
    Page<SearchResultDto> hybridSearch(String query, Set<SearchEntityType> types, Pageable pageable);
}

