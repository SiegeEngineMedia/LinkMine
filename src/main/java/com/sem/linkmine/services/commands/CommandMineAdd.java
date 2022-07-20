package com.sem.linkmine.services.commands;

import com.sem.linkmine.services.ConstantsService;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;

import java.util.Arrays;

public class CommandMineAdd {
    private final String commandText;
    private final String type;
    private final String link;
    private final String[] tags;

    public CommandMineAdd(SlashCommandRequest req, ConstantsService config) {
        commandText = req.getPayload().getText().trim();
        var segments = commandText.split(" ");
        type = segments[0];
        link = segments[1];
        tags = Arrays.copyOfRange(segments, 2, segments.length);
    }

    public String getCommandText() {
        return commandText;
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
}