package com.sem.linkmine.services.commands;

import com.sem.linkmine.services.ConfigService;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;

import java.util.Arrays;

public class CommandMineQuery {
    private final String commandText;
    private final String type;
    private final String[] tags;

    public CommandMineQuery(SlashCommandRequest req, ConfigService config) {
        commandText = req.getPayload().getText().trim();
        var tags = commandText.split(" ");
        if (tags.length > 0 && tags[0].startsWith(config.MINE_TYPE_PREFIX)) {
            type = tags[0].substring(1);
            tags = Arrays.copyOfRange(tags, 1, tags.length);
        } else {
            type = null;
        }
        this.tags = tags;
    }

    public String getCommandText() {
        return commandText;
    }

    public String getType() {
        return type;
    }

    public String[] getTags() {
        return tags;
    }
}