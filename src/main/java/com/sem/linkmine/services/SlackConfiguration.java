package com.sem.linkmine.services;

import com.slack.api.bolt.App;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.slack.api.model.block.composition.BlockCompositions.plainText;

@Configuration
public class SlackConfiguration {
    private final SlackService slackService;
    private final ConstantsService config;

    public SlackConfiguration(SlackService slackService, ConstantsService config) {
        this.slackService = slackService;
        this.config = config;
    }

    @Bean
    public App initSlackApp() {
        var app = new App();

        app.blockAction(config.ACTION_MINE_CONFIRM, (req, ctx) -> slackService.handleBlockMineConfirm(req, ctx));
        app.blockAction(config.ACTION_MINE_SHUFFLE, (req, ctx) -> slackService.handleBlockShuffle(req, ctx));
        app.blockAction(config.ACTION_MINE_CANCEL, (req, ctx) -> slackService.handleBlockCancel(req, ctx));

        app.command(config.COMMAND_MINE_QUERY, (req, ctx) -> slackService.handleMineQuery(req, ctx));
        app.command(config.COMMAND_MINE_ADD, (req, ctx) -> slackService.handleMineAdd(req, ctx));
        app.command(config.COMMAND_MINE_REM, (req, ctx) -> slackService.handleMineRem(req, ctx));

        return app;
    }
}
