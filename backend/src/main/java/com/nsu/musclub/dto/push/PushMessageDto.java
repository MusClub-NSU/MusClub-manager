package com.nsu.musclub.dto.push;

public class PushMessageDto {

    private String title;
    private String body;
    private String icon;
    private String badge;
    private String tag;
    private String actionUrl;
    private boolean requireInteraction;
    private long timestamp;

    public PushMessageDto() {
    }

    public PushMessageDto(String title, String body) {
        this.title = title;
        this.body = body;
        this.timestamp = System.currentTimeMillis();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public boolean isRequireInteraction() {
        return requireInteraction;
    }

    public void setRequireInteraction(boolean requireInteraction) {
        this.requireInteraction = requireInteraction;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final PushMessageDto dto = new PushMessageDto();

        public Builder title(String title) {
            dto.setTitle(title);
            return this;
        }

        public Builder body(String body) {
            dto.setBody(body);
            return this;
        }

        public Builder icon(String icon) {
            dto.setIcon(icon);
            return this;
        }

        public Builder badge(String badge) {
            dto.setBadge(badge);
            return this;
        }

        public Builder tag(String tag) {
            dto.setTag(tag);
            return this;
        }

        public Builder actionUrl(String actionUrl) {
            dto.setActionUrl(actionUrl);
            return this;
        }

        public Builder requireInteraction(boolean requireInteraction) {
            dto.setRequireInteraction(requireInteraction);
            return this;
        }

        public PushMessageDto build() {
            dto.setTimestamp(System.currentTimeMillis());
            return dto;
        }
    }
}
