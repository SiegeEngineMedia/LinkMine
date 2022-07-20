package com.sem.linkmine.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConstantsService {
    @Value("${spring.data.mongodb.uri}")
    public final String MONGO_URI = null;

    @Value("${spring.data.mongodb.database}")
    public final String MONGO_DB_NAME = "link-mine";

    @Value("${linkmine.links.db.collection}")
    public final String COMMAND_MINE_DB_COLLECTION = null;

    @Value("${linkmine.commands.mine.command}")
    public final String COMMAND_MINE_QUERY = null;
    @Value("${linkmine.commands.mine.command.add}")
    public final String COMMAND_MINE_ADD = null;
    @Value("${linkmine.commands.mine.command.rem}")
    public final String COMMAND_MINE_REM = null;
    @Value("${linkmine.commands.mine.type.prefix}")
    public final String MINE_TYPE_PREFIX = null;

    public final String ACTION_MINE_CONFIRM = "mine-confirm";
    public final String ACTION_MINE_SHUFFLE = "mine-shuffle";
    public final String ACTION_MINE_CANCEL = "mine-cancel";
}
