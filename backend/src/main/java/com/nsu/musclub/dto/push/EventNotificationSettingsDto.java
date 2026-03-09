package com.nsu.musclub.dto.push;

import java.util.List;

/**
 * DTO для настройки напоминаний мероприятия
 */
public class EventNotificationSettingsDto {

    /**
     * Включены ли напоминания за 24 часа
     */
    private boolean reminder24h = true;

    /**
     * Включены ли напоминания за 2 часа
     */
    private boolean reminder2h = true;

    /**
     * Включены ли напоминания за 15 минут
     */
    private boolean reminder15min = true;

    /**
     * Дополнительные кастомные интервалы (в минутах)
     */
    private List<Integer> customIntervals;

    public boolean isReminder24h() {
        return reminder24h;
    }

    public void setReminder24h(boolean reminder24h) {
        this.reminder24h = reminder24h;
    }

    public boolean isReminder2h() {
        return reminder2h;
    }

    public void setReminder2h(boolean reminder2h) {
        this.reminder2h = reminder2h;
    }

    public boolean isReminder15min() {
        return reminder15min;
    }

    public void setReminder15min(boolean reminder15min) {
        this.reminder15min = reminder15min;
    }

    public List<Integer> getCustomIntervals() {
        return customIntervals;
    }

    public void setCustomIntervals(List<Integer> customIntervals) {
        this.customIntervals = customIntervals;
    }
}
