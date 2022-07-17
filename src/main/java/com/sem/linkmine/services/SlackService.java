package com.sem.linkmine.services;

import com.sem.linkmine.models.LinkResource;
import com.sem.linkmine.services.commands.CommandMineQuery;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.ModelConfigurator;
import com.slack.api.model.block.ImageBlock;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@Configuration
public class SlackService {
    @Inject
    private MongoTemplate mongoTemplate;
    private final ConfigService config;

    public SlackService(ConfigService config) {
        this.config = config;
    }

    @Bean
    public App initSlackApp() {
        var app = new App();
        var parser = new JSONParser();

        app.blockAction("mine-confirm", (req, ctx) -> {
            var action = req.getPayload().getActions().get(0);
            try {
                var obj = (JSONObject) parser.parse(action.getValue());
                var actionValue = new LinkResourceBlockActionJSON(obj);
                var client = ctx.client();
                var payload = req.getPayload();

                // TODO: figure out how to delete the `ctx.respond` bot message
                client.chatDelete(r -> r);
                client.chatPostMessage(r -> r.channel(req.getPayload().getChannel().getId())
                        .text(actionValue.text)
                        .blocks(asBlocks(
                                image((ModelConfigurator<ImageBlock.ImageBlockBuilder>) image -> image.title(plainText(actionValue.text)).imageUrl(actionValue.link).altText(actionValue.text))
                        ))
                );

                return ctx.ack();
            } catch (ParseException e) {
                // TODO: handle me!
                return ctx.ack();
            }
        });

        app.blockAction("mine-shuffle", (req, ctx) -> {
            // TODO: implement me!
            System.out.println(req.toString());
            return ctx.ack();
        });

        app.blockAction("mine-cancel", (req, ctx) -> {
            // TODO: implement me!
            System.out.println(req.toString());
            return ctx.ack();
        });

        app.command(config.COMMAND_MINE_QUERY, (req, ctx) -> handleMineQuery(req, ctx));
        app.command(config.COMMAND_MINE_ADD, (req, ctx) -> handleMineAdd(req, ctx));
        app.command(config.COMMAND_MINE_REM, (req, ctx) -> handleMineRem(req, ctx));

        return app;
    }

    private Response handleMineAdd(SlashCommandRequest req, SlashCommandContext ctx) throws IOException, SlackApiException {
        return ctx.ack(":pick: Not implemented yet!");
    }

    private Response handleMineRem(SlashCommandRequest req, SlashCommandContext ctx) throws IOException, SlackApiException {
        return ctx.ack(":pick: Not implemented yet!");
    }

    private Response handleMineQuery(SlashCommandRequest req, SlashCommandContext ctx) throws IOException, SlackApiException {
        var command = new CommandMineQuery(req, config);
        var ids = new String[0];
        var resource = retrieveOneRandom(command.getType(), command.getTags(), ids);

        if (resource != null) {
            var obj = resource.getAttrs();
            var link = obj.getLink();
            var resourceTags = String.join(" ", obj.getTags());

            var commandValue = new LinkResourceBlockActionJSON(command.getCommandText(), resource);

            ctx.respond(asBlocks(
                    image((ModelConfigurator<ImageBlock.ImageBlockBuilder>) image -> image.title(plainText(command.getCommandText())).imageUrl(link).altText(resourceTags)),
                    actions(actions -> actions
                            .elements(asElements(
                                    button(b -> b.text(plainText("Cancel")).value(commandValue.toString()).actionId("mine-cancel").style("danger")),
                                    button(b -> b.text(plainText("Shuffle")).value(commandValue.toString()).actionId("mine-shuffle")),
                                    button(b -> b.text(plainText("Confirm")).value(commandValue.toString()).actionId("mine-confirm").style("primary"))
                            ))
                    )
            ));

            return ctx.ack();
        }
        return ctx.ack(":pick: No images to speak of! Try adding some.");
    }

    public List<LinkResource> retrieveRandom(String type, String[] tags, String[] excludeIds, int count) {
        Query query = buildQuery(type, tags, excludeIds);
        query.limit(count);
        return mongoTemplate.find(query, LinkResource.class, config.COMMAND_MINE_DB_COLLECTION);
    }

    public LinkResource retrieveOneRandom(String type, String[] tags, String[] excludeIds) {
        var resources = retrieveRandom(type, tags, excludeIds, 1);
        return resources.get(0);
    }

    private Query buildQuery(String type, String[] tags, String[] excludeIds) {
        Query query = new Query();

        if (type != null) {
            query.addCriteria(Criteria.where("attrs.type").is(type));
        }
        if (tags.length > 0) {
            query.addCriteria(Criteria.where("attrs.tags").in(tags));
        }
        if (excludeIds.length > 0) {
            query.addCriteria(Criteria.where("_id").not().in(excludeIds));
        }

        return query;
    }

    private class LinkResourceBlockActionJSON extends JSONObject {
        private final String id;
        private final String text;
        private final String link;
        private final String[] tags;

        public LinkResourceBlockActionJSON(String commandText, LinkResource resource) {
            super();
            var attrs = resource.getAttrs();
            put("id", id = resource.getId().toString());
            put("text", text = commandText);
            put("link", link = attrs.getLink());
            tags = attrs.getTags();
            put("tags", String.join(" ", tags));
        }

        public LinkResourceBlockActionJSON(JSONObject obj) {
            super();
            put("id", id = obj.get("id").toString());
            put("text", text = obj.get("text").toString());
            put("link", link = obj.get("link").toString());
            tags = obj.get("tags").toString().split(" ");
            put("tags", obj.get("tags").toString());
        }
    }
}
