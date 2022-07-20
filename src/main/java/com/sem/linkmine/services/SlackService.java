package com.sem.linkmine.services;

import com.sem.linkmine.errors.AuthenticationException;
import com.sem.linkmine.models.LinkModel;
import com.sem.linkmine.models.LinkResource;
import com.sem.linkmine.services.commands.CommandMineAdd;
import com.sem.linkmine.services.commands.CommandMineQuery;
import com.slack.api.app_backend.interactive_components.response.ActionResponse;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.ModelConfigurator;
import com.slack.api.model.block.ImageBlock;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;

@Service
@Configuration
public class SlackService {
    private final LinkService linkService;
    private final ConstantsService consts;

    private final JSONParser parser;

    public SlackService(LinkService linkService, ConstantsService consts) {
        this.linkService = linkService;
        this.consts = consts;
        this.parser = new JSONParser();
    }

    //region Command handlers
    public Response handleMineQuery(SlashCommandRequest req, SlashCommandContext ctx) throws IOException, SlackApiException {
        var command = new CommandMineQuery(req, consts);
        var resource = linkService.retrieveOneRandom(command.getType(), command.getTags());

        if (resource != null) {
            var obj = resource.getAttrs();
            var link = obj.getLink();
            var commandValue = new LinkResourceBlockActionJSON(command.getCommandText(), command.getType(), command.getTags(), resource);
            var resourceTags = String.join(" ", obj.getTags());

            ctx.respond(asBlocks(
                    image((ModelConfigurator<ImageBlock.ImageBlockBuilder>) image -> image.title(plainText(command.getCommandText())).imageUrl(link).altText(resourceTags)),
                    actions(actions -> actions
                            .elements(asElements(
                                    button(b -> b.text(plainText("Cancel")).value(commandValue.toString()).actionId(consts.ACTION_MINE_CANCEL).style("danger")),
                                    button(b -> b.text(plainText("Shuffle")).value(commandValue.toString()).actionId(consts.ACTION_MINE_SHUFFLE)),
                                    button(b -> b.text(plainText("Confirm")).value(commandValue.toString()).actionId(consts.ACTION_MINE_CONFIRM).style("primary"))
                            ))
                    )
            ));

            return ctx.ack();
        }
        return ctx.ack(":pick: No images to speak of! Try adding some.");
    }

    public Response handleMineAdd(SlashCommandRequest req, SlashCommandContext ctx) {
        var command = new CommandMineAdd(req, consts);
        var resource = new LinkResource(new LinkModel(
                command.getType(),
                req.getPayload().getUserId(),
                command.getLink(),
                command.getTags()
        ));

        try {
            linkService.upsert(resource);
            return ctx.ack(":pick: Link updated.");
        } catch (AuthenticationException e) {
            // TODO: handle me better
            return ctx.ack(":pick: That link has been entered by someone else, you aren't able to update it.");
        }
    }

    public Response handleMineRem(SlashCommandRequest req, SlashCommandContext ctx) throws IOException, SlackApiException {
        return ctx.ack(":pick: Not implemented yet!");
    }
    //endregion

    //region Block handlers
    public Response handleBlockMineConfirm(BlockActionRequest req, ActionContext ctx) throws IOException {
        var payload = req.getPayload();
        var action = payload.getActions().get(0);
        try {
            var obj = (JSONObject) parser.parse(action.getValue());
            var actionValue = new LinkResourceBlockActionJSON(obj);

            // FIXME: this posts the link as the bot, not the user
            var response = ActionResponse
                    .builder()
                    .deleteOriginal(true)
                    .replaceOriginal(true)
                    .responseType("in_channel")
                    .blocks(asBlocks(
                            image((ModelConfigurator<ImageBlock.ImageBlockBuilder>) image -> image
                                    .title(plainText(actionValue.text))
                                    .imageUrl(actionValue.link)
                                    .altText(actionValue.text)
                            )
                    ))
                    .text(actionValue.text)
                    .build();

            ctx.respond(response);

            return ctx.ack();
        } catch (ParseException e) {
            // TODO: handle me!
            return ctx.ack();
        }
    }

    public Response handleBlockShuffle(BlockActionRequest req, ActionContext ctx) throws IOException {
        var payload = req.getPayload();
        var action = payload.getActions().get(0);
        try {
            var obj = (JSONObject) parser.parse(action.getValue());
            var actionValue = new LinkResourceBlockActionJSON(obj);
            var resource = linkService.retrieveOneRandom(actionValue.type, actionValue.tags);

            if (resource != null) {
                var updatedActionValue = new LinkResourceBlockActionJSON(actionValue.text, actionValue.type, actionValue.tags, resource);
                var resourceTags = String.join(" ", resource.getAttrs().getTags());

                var response = ActionResponse
                        .builder()
                        .replaceOriginal(true)
                        .blocks(asBlocks(
                                image((ModelConfigurator<ImageBlock.ImageBlockBuilder>) image -> image.title(plainText(updatedActionValue.text)).imageUrl(updatedActionValue.link).altText(resourceTags)),
                                actions(actions -> actions
                                        .elements(asElements(
                                                button(b -> b.text(plainText("Cancel")).value(updatedActionValue.toString()).actionId(consts.ACTION_MINE_CANCEL).style("danger")),
                                                button(b -> b.text(plainText("Shuffle")).value(updatedActionValue.toString()).actionId(consts.ACTION_MINE_SHUFFLE)),
                                                button(b -> b.text(plainText("Confirm")).value(updatedActionValue.toString()).actionId(consts.ACTION_MINE_CONFIRM).style("primary"))
                                        ))
                                )
                        ))
                        .text(updatedActionValue.text)
                        .build();

                ctx.respond(response);

                return ctx.ack();
            }
            ctx.respond(":pick: No images to speak of! Try adding some.");
            return ctx.ack();
        } catch (ParseException e) {
            // TODO: handle me!
            return ctx.ack();
        }
    }

    public Response handleBlockCancel(BlockActionRequest req, ActionContext ctx) throws IOException {
        var response = ActionResponse
                .builder()
                .deleteOriginal(true)
                .replaceOriginal(true)
                .text("")
                .build();

        ctx.respond(response);

        return ctx.ack();
    }
    //endregion

    private class LinkResourceBlockActionJSON extends JSONObject {
        private final String id;
        private final String link;
        private final String text;
        private final String type;
        private final String[] tags;

        public LinkResourceBlockActionJSON(String commandText, String type, String[] tags, LinkResource resource) {
            super();
            var attrs = resource.getAttrs();
            put("id", id = resource.getId().toString());
            put("link", link = attrs.getLink());
            put("text", text = commandText);
            put("type", this.type = type);
            this.tags = tags;
            put("tags", String.join(" ", tags));
        }

        public LinkResourceBlockActionJSON(JSONObject obj) {
            super();
            put("id", id = obj.get("id").toString());
            put("link", link = obj.get("link").toString());
            put("text", text = obj.get("text").toString());
            put("type", type = obj.get("type") != null ? obj.get("type").toString() : null);
            tags = obj.get("tags").toString().split(" ");
            put("tags", obj.get("tags").toString());
        }
    }
}
