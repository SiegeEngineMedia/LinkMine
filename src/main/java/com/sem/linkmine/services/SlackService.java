package com.sem.linkmine.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sem.linkmine.errors.AuthenticationException;
import com.sem.linkmine.models.LinkModel;
import com.sem.linkmine.models.LinkResource;
import com.sem.linkmine.services.commands.CommandMineAdd;
import com.sem.linkmine.services.commands.CommandMineQuery;
import com.sem.linkmine.services.commands.LinkResourceBlockAction;
import com.slack.api.app_backend.interactive_components.response.ActionResponse;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.ModelConfigurator;
import com.slack.api.model.block.ContextBlockElement;
import com.slack.api.model.block.ImageBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.element.BlockElement;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.*;

@Service
@Configuration
public class SlackService {
    private final LinkService linkService;
    private final ConstantsService constants;

    private final ObjectMapper mapper;

    private final ActionResponse REMOVE_MESSAGE_RESPONSE = ActionResponse
            .builder()
            .deleteOriginal(true)
            .replaceOriginal(true)
            .text("")
            .build();

    public SlackService(LinkService linkService, ConstantsService constants) {
        this.linkService = linkService;
        this.constants = constants;
        this.mapper = new ObjectMapper();
    }

    //region Command handlers
    public Response handleMineQuery(SlashCommandRequest req, SlashCommandContext ctx) throws IOException {
        var payload = req.getPayload();
        var command = new CommandMineQuery(req, constants);
        var resource = linkService.retrieveOneRandom(command.getType(), command.getTags());

        if (resource != null) {
            var actionValue = new LinkResourceBlockAction(command.getCommandText(), command.getType(), command.getTags(), resource);

            try {
                var actionElements = buildMineActionElements(actionValue);

                if (payload.getChannelName().equals("directmessage")) {
                    ctx.respond(r -> r
                            .responseType("in_channel")
                            .blocks(asBlocks(
                                    buildMineActionOutput(actionValue),
                                    actions(actions -> actions.elements(actionElements))
                            ))
                    );
                } else {
                    ctx.respond(r -> r
                            .blocks(asBlocks(
                                    buildMineActionOutput(actionValue),
                                    actions(actions -> actions.elements(actionElements))
                            ))
                    );
                }
                return ctx.ack();
            } catch (JsonProcessingException e) {
                // TODO: handle better;
                return ctx.ack();
            }
        }
        return ctx.ack(":pick: No links to speak of! Try adding some.");
    }

    public Response handleMineAdd(SlashCommandRequest req, SlashCommandContext ctx) {
        var command = new CommandMineAdd(req, constants);
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
        List<CountObject> counts = linkService.getCounts();
        List<ContextBlockElement> textElements = new ArrayList<ContextBlockElement>();
        int total = 0;
        for (var element : counts) {
            total += element.count;
            textElements.add(plainText(element._id + ": " + element.count));
        }
        textElements.add(0, plainText("Total: " + total));

        ctx.respond(r -> r
                .text("Count for each tag:")
                .blocks(asBlocks(
                        section(s -> s.text(markdownText("Count for each tag:"))),
                        context(c -> c.elements(textElements))
                ))
        );

        return ctx.ack();
    }
    //endregion

    //region Block handlers
    public Response handleBlockMineConfirm(BlockActionRequest req, ActionContext ctx) throws IOException, SlackApiException {
        var payload = req.getPayload();
        var action = payload.getActions().get(0);

        try {
            var client = ctx.client();
            var actionValue = mapper.readValue(action.getValue(), LinkResourceBlockAction.class);
            var userId = ctx.getRequestUserId();
            var userInfo = client.usersInfo(r -> r.user(userId));
            var userProfile = userInfo.getUser().getProfile();

            if (payload.getChannel().getName().equals("directmessage")) {
                ctx.respond(r -> r
                        .responseType("in_channel")
                        .replaceOriginal(true)
                        .blocks(asBlocks(
                                buildMineActionOutput(actionValue),
                                buildMineActionRider()
                        ))
                        .text(actionValue.getCommand().getText())
                );
            } else {
                ctx.respond(REMOVE_MESSAGE_RESPONSE);
                client.chatPostMessage(r -> r
                        .channel(payload.getChannel().getId())
                        .username(userProfile.getDisplayName())
                        .iconUrl(userProfile.getImageOriginal())
                        .blocks(asBlocks(
                                buildMineActionOutput(actionValue),
                                buildMineActionRider()
                        ))
                        .text(actionValue.getCommand().getText())
                );
            }

            return ctx.ack();
        } catch (SlackApiException e) {
            // TODO: handle me!
            System.out.println(e);
            throw e;
        }
    }

    public Response handleBlockMineBack(BlockActionRequest req, ActionContext ctx) throws IOException {
        var payload = req.getPayload();
        var action = payload.getActions().get(0);
        var actionValue = mapper.readValue(action.getValue(), LinkResourceBlockAction.class);

        actionValue.dropLastId();
        var id = actionValue.getLastId();
        var resourceOption = linkService.findById(new ObjectId(id));

        if (resourceOption.isPresent()) {
            var resource = resourceOption.get();
            var response = buildBlockShuffleResponse(actionValue, resource);
            var attrs = resource.getAttrs();

            ctx.respond(response);

            return ctx.ack();
        }
        ctx.respond(":pick: No links to speak of! Try adding some.");
        return ctx.ack();
    }

    public Response handleBlockMineFirst(BlockActionRequest req, ActionContext ctx) throws IOException {
        var payload = req.getPayload();
        var action = payload.getActions().get(0);
        var actionValue = mapper.readValue(action.getValue(), LinkResourceBlockAction.class);

        actionValue.dropIdsKeepFirst();
        var id = actionValue.getLastId();
        var resourceOption = linkService.findById(new ObjectId(id));

        if (resourceOption.isPresent()) {
            var resource = resourceOption.get();
            var response = buildBlockShuffleResponse(actionValue, resource);

            ctx.respond(response);

            return ctx.ack();
        }
        ctx.respond(":pick: No links to speak of! Try adding some.");
        return ctx.ack();
    }

    public Response handleBlockShuffle(BlockActionRequest req, ActionContext ctx) throws IOException {
        var payload = req.getPayload();
        var action = payload.getActions().get(0);
        var actionValue = mapper.readValue(action.getValue(), LinkResourceBlockAction.class);
        var command = actionValue.getCommand();

        var resource = linkService.retrieveOneRandom(command.getType(), command.getTags());

        if (resource != null) {
            var attrs = resource.getAttrs();
            actionValue.addId(resource.getId());
            actionValue.setType(attrs.getType());
            actionValue.setLink(attrs.getLink());

            var actionElements = buildMineActionElements(actionValue);
            var response = ActionResponse
                    .builder()
                    .replaceOriginal(true)
                    .blocks(asBlocks(
                            buildMineActionOutput(actionValue),
                            actions(actions -> actions.elements(actionElements))
                    ))
                    .text(command.getText())
                    .build();

            ctx.respond(response);

            return ctx.ack();
        }
        ctx.respond(":pick: No links to speak of! Try adding some.");
        return ctx.ack();
    }

    public Response handleBlockCancel(BlockActionRequest req, ActionContext ctx) throws IOException {
        ctx.respond(REMOVE_MESSAGE_RESPONSE);
        return ctx.ack();
    }
    //endregion

    private ActionResponse buildBlockShuffleResponse(LinkResourceBlockAction actionValue, LinkResource resource) throws JsonProcessingException {
        var attrs = resource.getAttrs();
        actionValue.setType(attrs.getType());
        actionValue.setLink(attrs.getLink());

        var actionElements = buildMineActionElements(actionValue);
        var response = ActionResponse
                .builder()
                .replaceOriginal(true)
                .blocks(asBlocks(
                        buildMineActionOutput(actionValue),
                        actions(actions -> actions.elements(actionElements))
                ))
                .text(actionValue.getCommand().getText())
                .build();

        return response;
    }

    private LayoutBlock buildMineActionOutput(LinkResourceBlockAction actionValue) {
        var command = actionValue.getCommand();
        switch (actionValue.getType()) {
            // FIXME: make this a flexible value with some defined types
            case "image":
                return image((ModelConfigurator<ImageBlock.ImageBlockBuilder>) i -> i
                        .title(plainText(command.getText()))
                        .imageUrl(actionValue.getLink())
                        .altText(command.getText())
                );
            default:
                return section(s -> s.text(markdownText(actionValue.getLink())));
        }
    }

    private LayoutBlock buildMineActionRider() {
        return context(c -> c.elements(asContextElements(
                plainText(pt -> pt.text(":pick: Posted with " + constants.COMMAND_MINE_QUERY))
        )));
    }

    private List<BlockElement> buildMineActionElements(LinkResourceBlockAction actionValue) throws JsonProcessingException {
        var actionPayload = mapper.writeValueAsString(actionValue);
        var actionElements = new ArrayList<BlockElement>();
        actionElements.add(button(b -> b.text(plainText("Confirm")).value(actionPayload).actionId(constants.ACTION_MINE_CONFIRM).style("primary")));
        actionElements.add(button(b -> b.text(plainText("Shuffle")).value(actionPayload).actionId(constants.ACTION_MINE_SHUFFLE)));
        actionElements.add(button(b -> b.text(plainText("Cancel")).value(actionPayload).actionId(constants.ACTION_MINE_CANCEL).style("danger")));

        if (actionValue.getIds().length > 1) {
            actionElements.add(button(b -> b.text(plainText("Back")).value(actionPayload).actionId(constants.ACTION_MINE_BACK)));
        }
        if (actionValue.getIds().length > 2) {
            actionElements.add(button(b -> b.text(plainText("First")).value(actionPayload).actionId(constants.ACTION_MINE_FIRST)));
        }

        return actionElements;
    }
}
