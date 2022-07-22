package com.sem.linkmine.services.commands;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sem.linkmine.models.LinkResource;
import org.bson.types.ObjectId;

import java.util.Arrays;

public class LinkResourceBlockAction {
    private CommandAttrs command;
    private String[] ids;
    private String type;

    private String link;

    public LinkResourceBlockAction() {
    }

    public LinkResourceBlockAction(String commandText, String commandType, String[] commandTags, LinkResource resource) {
        this(commandText, commandType, commandTags);

        var attrs = resource.getAttrs();
        ids = new String[]{resource.getId().toString()};
        link = attrs.getLink();
        this.type = attrs.getType();
    }

    public LinkResourceBlockAction(String commandText, String commandType, String[] commandTags) {
        this.command = new CommandAttrs(commandText, commandType, commandTags);
    }

    public CommandAttrs getCommand() {
        return command;
    }

    public String[] getIds() {
        return ids;
    }

    public String getType() {
        return type;
    }

    public String getLink() {
        return link;
    }

    public void addId(ObjectId id) {
        ids = Arrays.copyOf(ids, ids.length + 1);
        ids[ids.length - 1] = id.toString();
    }

    public String dropLastId() {
        var id = ids[ids.length - 1];
        ids = Arrays.copyOfRange(ids, 0, ids.length - 1);
        return id;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }

    //region setters
    public void setLink(String link) {
        this.link = link;
    }

    public void setType(String type) {
        this.type = type;
    }
    //endregion

    @JsonIgnore
    public String getLastId() {
        return ids.length > 0 ? ids[ids.length - 1] : null;
    }

    public class CommandAttrs {
        private String text;
        private String type;
        private String[] tags;

        public CommandAttrs() {
        }

        public CommandAttrs(String text, String type, String[] tags) {
            this.text = text;
            this.type = type;
            this.tags = tags;
        }

        public String getText() {
            return text;
        }

        public String getType() {
            return type;
        }

        public String[] getTags() {
            return tags;
        }
    }
}