package com.nsu.musclub.web;

import com.nsu.musclub.dto.search.SearchEntityType;
import com.nsu.musclub.dto.search.SearchResultDto;
import com.nsu.musclub.service.SearchService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/hybrid")
    public Page<SearchResultDto> hybridSearch(@RequestParam("q") String query,
                                               @RequestParam(value = "types", required = false) Set<SearchEntityType> types,
                                               @ParameterObject Pageable pageable) {
        return searchService.hybridSearch(query, types, pageable);
    }
}

