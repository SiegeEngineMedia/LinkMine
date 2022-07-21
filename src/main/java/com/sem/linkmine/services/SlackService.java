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
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.ModelConfigurator;
import com.slack.api.model.block.ContextBlockElement;
import com.slack.api.model.block.ImageBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.element.BlockElement;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            var actionValue = new LinkResourceBlockActionJSON(command.getCommandText(), command.getType(), command.getTags(), resource);

            ctx.respond(asBlocks(
                    buildMineActionOutput(actionValue),
                    actions(actions -> actions.elements(buildMineActionElements(actionValue)))
            ));

            return ctx.ack();
        }
        return ctx.ack(":pick: No images to speak of! Try adding some.");
    }

    public Response handleMineAdd(SlashCommandRequest req, SlashCommandContext ctx) {
        var command = new CommandMineAdd(req, consts);
        var attrs = new LinkModel(
                command.getType(),
                req.getPayload().getUserId(),
                command.getLink(),
                command.getTags()
        );

        try {
            linkService.insert(attrs);
            return ctx.ack(":pick: Link updated.");
        } catch (AuthenticationException e) {
            return ctx.ack(":pick: That link has been entered by someone else, you aren't able to update it.");
        }
    }

    public Response handleMineRem(SlashCommandRequest req, SlashCommandContext ctx) throws IOException, SlackApiException {
        var payload = req.getPayload();
        var link = payload.getText().trim();
        var userId = payload.getUserId();

        try {
            linkService.deleteByLink(link, userId);
            return ctx.ack(":pick: Link removed.");
        } catch (AuthenticationException e) {
            return ctx.ack(":pick: That link has been entered by someone else, you aren't able to remove it.");
        }
    }

    public Response handleMineSum(SlashCommandRequest req, SlashCommandContext ctx) throws IOException, SlackApiException {
        var payload = req.getPayload();

        List<CountObject> counts = linkService.getCounts();
        List<ContextBlockElement> textElements = new ArrayList<ContextBlockElement>();
        int total = 0;
        for (var element : counts) {
            total += element.count;
            textElements.add(plainText(element._id + ": " + element.count));
        }
        textElements.add(0, plainText("Total: " + total));

        ctx.client().chatPostMessage(r -> r
                .channel(ctx.getChannelId())
                .blocks(asBlocks(context(c -> c.elements(textElements))))
                .text("Count for each tag:")
        );

        return ctx.ack();
    }
    //endregion

    //region Block handlers
    public Response handleBlockMineConfirm(BlockActionRequest req, ActionContext ctx) throws IOException, SlackApiException {
        var payload = req.getPayload();
        var action = payload.getActions().get(0);
        try {
            var obj = (JSONObject) parser.parse(action.getValue());
            var actionValue = new LinkResourceBlockActionJSON(obj);

            var response = ActionResponse
                    .builder()
                    .deleteOriginal(true)
                    .replaceOriginal(true)
                    .text("")
                    .build();

            ctx.respond(response);
            ctx.client().chatPostMessage(r -> r
                    .channel(req.getPayload().getChannel().getId())
                    .blocks(asBlocks(buildMineActionOutput(actionValue)))
                    .text(actionValue.text)
            );

            return ctx.ack();
        } catch (ParseException e) {
            // TODO: handle me!
            return ctx.ack();
        }
    }

    public Response handleBlockBack(BlockActionRequest req, ActionContext ctx) throws IOException {
        var payload = req.getPayload();
        var action = payload.getActions().get(0);
        try {
            var obj = (JSONObject) parser.parse(action.getValue());
            var actionValue = new LinkResourceBlockActionJSON(obj);
            actionValue.dropLastId();
            var id = actionValue.getLastId();
            var resourceOption = linkService.findById(new ObjectId(id));

            if (resourceOption.isPresent()) {
                var resource = resourceOption.get();
                var attrs = resource.getAttrs();
                actionValue.setLink(attrs.getLink());

                var response = ActionResponse
                        .builder()
                        .replaceOriginal(true)
                        .blocks(asBlocks(
                                buildMineActionOutput(actionValue),
                                actions(actions -> actions.elements(buildMineActionElements(actionValue)))
                        ))
                        .text(actionValue.text)
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

    public Response handleBlockShuffle(BlockActionRequest req, ActionContext ctx) throws IOException {
        var payload = req.getPayload();
        var action = payload.getActions().get(0);
        try {
            var obj = (JSONObject) parser.parse(action.getValue());
            var actionValue = new LinkResourceBlockActionJSON(obj);
            var resource = linkService.retrieveOneRandom(actionValue.type, actionValue.tags);

            if (resource != null) {
                var attrs = resource.getAttrs();
                actionValue.addId(resource.getId());
                actionValue.setLink(attrs.getLink());

                var response = ActionResponse
                        .builder()
                        .replaceOriginal(true)
                        .blocks(asBlocks(
                                buildMineActionOutput(actionValue),
                                actions(actions -> actions.elements(buildMineActionElements(actionValue)))
                        ))
                        .text(actionValue.text)
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

    private LayoutBlock buildMineActionOutput(LinkResourceBlockActionJSON actionValue) {
        return image((ModelConfigurator<ImageBlock.ImageBlockBuilder>) image -> image.title(plainText(actionValue.text)).imageUrl(actionValue.link).altText(actionValue.text));
    }

    private List<BlockElement> buildMineActionElements(LinkResourceBlockActionJSON actionValue) {
        var actionPayload = actionValue.asJSON().toString();
        var actionElements = new ArrayList<BlockElement>();
        actionElements.add(button(b -> b.text(plainText("Confirm")).value(actionPayload).actionId(consts.ACTION_MINE_CONFIRM).style("primary")));
        actionElements.add(button(b -> b.text(plainText("Shuffle")).value(actionPayload).actionId(consts.ACTION_MINE_SHUFFLE)));
        actionElements.add(button(b -> b.text(plainText("Cancel")).value(actionPayload).actionId(consts.ACTION_MINE_CANCEL).style("danger")));

        if (actionValue.ids.length > 1) {
            actionElements.add(button(b -> b.text(plainText("Back")).value(actionPayload).actionId(consts.ACTION_MINE_BACK)));
        }

        return actionElements;
    }

    private class LinkResourceBlockActionJSON extends JSONObject {
        private final String text;
        private final String type;
        private final String[] tags;
        private String link;
        private String[] ids;

        public LinkResourceBlockActionJSON(String commandText, String type, String[] tags, LinkResource resource) {
            super();
            var attrs = resource.getAttrs();
            ids = new String[]{resource.getId().toString()};
            link = attrs.getLink();
            text = commandText;
            this.type = type;
            this.tags = tags;
        }

        public LinkResourceBlockActionJSON(JSONObject obj) {
            super();
            ids = obj.get("ids").toString().split(" ");
            link = obj.get("link").toString();
            text = obj.get("text").toString();
            type = obj.get("type") != null ? obj.get("type").toString() : null;
            tags = obj.get("tags").toString().split(" ");
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

        public void setLink(String link) {
            this.link = link;
        }

        public String getLastId() {
            return ids.length > 0 ? ids[ids.length - 1] : null;
        }

        public JSONObject asJSON() {
            var json = new JSONObject();
            json.put("ids", String.join(" ", ids));
            json.put("link", link);
            json.put("text", text);
            json.put("type", type);
            json.put("tags", String.join(" ", tags));
            return json;
        }
    }
}
