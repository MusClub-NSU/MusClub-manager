package com.nsu.musclub.service.impl;

import com.nsu.musclub.ai.AiTextClient;
import com.nsu.musclub.domain.Event;
import com.nsu.musclub.repository.EventRepository;
import com.nsu.musclub.service.EventPosterAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;

@Service
@Transactional
public class EventPosterAiServiceImpl implements EventPosterAiService {

    private static final Logger log = LoggerFactory.getLogger(EventPosterAiServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final EventRepository eventRepository;
    private final AiTextClient aiTextClient;

    public EventPosterAiServiceImpl(EventRepository eventRepository, AiTextClient aiTextClient) {
        this.eventRepository = eventRepository;
        this.aiTextClient = aiTextClient;
    }

    @Override
    public String generatePosterDescription(Long eventId, boolean saveToEvent) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        String systemPrompt = """
                Ты помощник организатора музыкальных мероприятий.
                Пиши короткие, живые описания афиш на русском языке.
                Стиль: дружелюбный, человеческий, 3–6 предложений.
                Обязательно укажи дату, место и ключевые особенности события.
                Не придумывай новых фактов, используй только переданные данные.
                """;

        String userPrompt = buildUserPrompt(event);

        String description;
        try {
            description = aiTextClient.generateText(systemPrompt, userPrompt);
        } catch (HttpClientErrorException e) {
            log.error("AI provider client error while generating description for event {}: status={}, body={}",
                    eventId, e.getStatusCode(), e.getResponseBodyAsString(), e);

            if (e.getStatusCode().value() == 402) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Провайдер ИИ вернул ошибку оплаты (Insufficient Balance). Обратитесь к администратору."
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Ошибка при обращении к провайдеру ИИ: " + e.getStatusCode()
            );
        } catch (HttpServerErrorException e) {
            log.error("AI provider server error while generating description for event {}: status={}, body={}",
                    eventId, e.getStatusCode(), e.getResponseBodyAsString(), e);

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Провайдер ИИ временно недоступен, попробуйте позже"
            );
        } catch (Exception e) {
            log.error("Unexpected error while generating AI poster description for event {}", eventId, e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Не удалось сгенерировать описание афиши, попробуйте позже"
            );
        }

        if (saveToEvent) {
            event.setAiDescription(description);
            eventRepository.save(event);
        }

        return description;
    }

    private String buildUserPrompt(Event event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Нужно составить текст для афиши музыкального события.\n\n");
        sb.append("Название: ").append(event.getTitle()).append("\n");

        if (event.getStartTime() != null) {
            sb.append("Начало: ")
                    .append(DATE_TIME_FORMATTER.format(event.getStartTime()))
                    .append("\n");
        }

        if (event.getEndTime() != null) {
            sb.append("Окончание: ")
                    .append(DATE_TIME_FORMATTER.format(event.getEndTime()))
                    .append("\n");
        }

        if (event.getVenue() != null && !event.getVenue().isBlank()) {
            sb.append("Место проведения: ").append(event.getVenue()).append("\n");
        }

        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            sb.append("Черновое описание от организаторов: ")
                    .append(event.getDescription())
                    .append("\n");
        }

        sb.append("""
                
                Требования к тексту:
                - Напиши 3–6 предложений.
                - Стиль: живой, приглашающий, без канцелярита и токсичной рекламы.
                - Не используй эмодзи.
                - Текст должен быть на русском языке.
                """);

        return sb.toString();
    }
}
