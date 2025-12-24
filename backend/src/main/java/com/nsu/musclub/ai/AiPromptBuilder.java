package com.nsu.musclub.ai;

import com.nsu.musclub.domain.Event;

import java.time.format.DateTimeFormatter;

/**
 * Utility class for building AI prompts from event data.
 */
public class AiPromptBuilder {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Builds a user prompt from event data for AI text generation.
     * 
     * @param event the event to build prompt from
     * @return formatted prompt string
     */
    public static String buildEventPrompt(Event event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Event Information:\n\n");
        sb.append("Title: ").append(event.getTitle()).append("\n");

        if (event.getStartTime() != null) {
            sb.append("Start Time: ")
                    .append(DATE_TIME_FORMATTER.format(event.getStartTime()))
                    .append("\n");
        }

        if (event.getEndTime() != null) {
            sb.append("End Time: ")
                    .append(DATE_TIME_FORMATTER.format(event.getEndTime()))
                    .append("\n");
        }

        if (event.getVenue() != null && !event.getVenue().isBlank()) {
            sb.append("Venue: ").append(event.getVenue()).append("\n");
        }

        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            sb.append("Description: ").append(event.getDescription()).append("\n");
        }

        if (event.getAiDescription() != null && !event.getAiDescription().isBlank()) {
            sb.append("AI-Generated Description: ").append(event.getAiDescription()).append("\n");
        }

        return sb.toString();
    }
}

