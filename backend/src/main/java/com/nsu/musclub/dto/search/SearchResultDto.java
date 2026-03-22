package com.nsu.musclub.dto.search;

public class SearchResultDto {
    private SearchEntityType entityType;
    private Long entityId;
    private String title;
    private String snippet;
    private double score;
    private double lexicalScore;
    private double vectorScore;

    public SearchEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(SearchEntityType entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getLexicalScore() {
        return lexicalScore;
    }

    public void setLexicalScore(double lexicalScore) {
        this.lexicalScore = lexicalScore;
    }

    public double getVectorScore() {
        return vectorScore;
    }

    public void setVectorScore(double vectorScore) {
        this.vectorScore = vectorScore;
    }
}

