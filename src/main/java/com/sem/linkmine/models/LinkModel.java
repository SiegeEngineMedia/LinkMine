package com.sem.linkmine.models;

public class LinkModel {
    private String type;
    private String link;
    private String[] tags;

    /**
     * Slack users' ID that added this link
     */
    private String userId;

    public LinkModel(
            String type,
            String userId,
            String link,
            String[] tags
    ) {
        this.type = type;
        this.userId = userId;
        this.link = link;
        this.tags = tags;
    }

    public String getType() {
        return type;
    }

    public String getLink() {
        return link;
    }

    public String[] getTags() {
        return tags;
    }

    public String getUserId() {
        return userId;
    }
}