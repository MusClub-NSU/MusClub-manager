package com.nsu.musclub.service.impl;

import com.nsu.musclub.ai.AiTextClient;
import com.nsu.musclub.domain.Event;
import com.nsu.musclub.dto.event.EventProgramItemResponseDto;
import com.nsu.musclub.repository.EventRepository;
import com.nsu.musclub.service.EventPosterAiService;
import com.nsu.musclub.service.EventProgramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
public class EventPosterAiServiceImpl implements EventPosterAiService {

    private static final Logger log = LoggerFactory.getLogger(EventPosterAiServiceImpl.class);

    private final EventRepository eventRepository;
    private final EventProgramService eventProgramService;
    private final AiTextClient aiTextClient;
    private final ZoneId displayZone;
    private final DateTimeFormatter displayDateTimeFormatter;
    private final DateTimeFormatter programTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public EventPosterAiServiceImpl(
            EventRepository eventRepository,
            EventProgramService eventProgramService,
            AiTextClient aiTextClient,
            @Value("${musclub.display-timezone:Asia/Novosibirsk}") String displayTimezoneId
    ) {
        this.eventRepository = eventRepository;
        this.eventProgramService = eventProgramService;
        this.aiTextClient = aiTextClient;
        this.displayZone = ZoneId.of(displayTimezoneId);
        this.displayDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(this.displayZone);
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
                Время начала и окончания ниже уже указано в часовом поясе для афиши — переписывай их в тексте как есть, без пересчёта в другой пояс.
                Упоминай время окончания («до …», «завершится в …») только если в данных есть строка «Окончание:». Если её нет — не указывай окончание и не придумывай длительность.
                Если дана концертная программа, можно кратко отразить состав (например, ключевые номера или исполнителей), не добавляя сведений, которых нет в программе.
                """;

        String userPrompt = buildUserPrompt(event.getId(), event);

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

    private String formatForPoster(java.time.OffsetDateTime odt) {
        return displayDateTimeFormatter.format(odt.toInstant());
    }

    private void appendProgramSection(StringBuilder sb, Long eventId) {
        var items = eventProgramService.list(eventId);
        if (items.isEmpty()) {
            return;
        }
        sb.append("\nКонцертная программа (порядок выступлений):\n");
        for (EventProgramItemResponseDto item : items) {
            sb.append("- ");
            if (item.getPlannedTime() != null) {
                sb.append(item.getPlannedTime().format(programTimeFormatter)).append(" — ");
            }
            sb.append(item.getTitle());
            if (item.getArtist() != null && !item.getArtist().isBlank()) {
                sb.append(" (").append(item.getArtist()).append(")");
            }
            if (item.getDurationText() != null && !item.getDurationText().isBlank()) {
                sb.append(", длительность: ").append(item.getDurationText());
            }
            if (item.getNotes() != null && !item.getNotes().isBlank()) {
                sb.append(". Примечание: ").append(item.getNotes());
            }
            sb.append("\n");
        }
    }

    private String buildUserPrompt(Long eventId, Event event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Нужно составить текст для афиши музыкального события.\n\n");
        sb.append("Название: ").append(event.getTitle()).append("\n");

        if (event.getStartTime() != null) {
            sb.append("Начало (")
                    .append(displayZone.getId())
                    .append("): ")
                    .append(formatForPoster(event.getStartTime()))
                    .append("\n");
        }

        if (event.getEndTime() != null) {
            sb.append("Окончание (")
                    .append(displayZone.getId())
                    .append("): ")
                    .append(formatForPoster(event.getEndTime()))
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

        appendProgramSection(sb, eventId);

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
