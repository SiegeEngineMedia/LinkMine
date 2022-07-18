package com.sem.linkmine.services;

import com.sem.linkmine.errors.AuthenticationException;
import com.sem.linkmine.models.LinkResource;
import com.sem.linkmine.repositories.LinkRepository;
import com.sem.linkmine.services.commands.CommandMineQuery;
import com.slack.api.app_backend.interactive_components.response.ActionResponse;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.ModelConfigurator;
import com.slack.api.model.block.ImageBlock;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SampleOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;

@Service
public class LinkService {
    private final MongoTemplate mongoTemplate;
    private final LinkRepository linkRepository;
    private final ConfigService config;

    public LinkService(MongoTemplate mongoTemplate, LinkRepository linkRepository, ConfigService config) {
        this.mongoTemplate = mongoTemplate;
        this.linkRepository = linkRepository;
        this.config = config;
    }

    public LinkResource upsert(LinkResource resource) throws AuthenticationException {
        var existing = linkRepository.findByLink(resource.getAttrs().getLink());
        if (existing != null && existing.getId().equals(resource.getId())) {
            if (existing.getAttrs().getUserId().equals(resource.getAttrs().getUserId())) {
                return linkRepository.save(resource);
            }
            throw new AuthenticationException();
        }
        return linkRepository.save(resource);
    }

    public void delete(ObjectId id, String userId) throws AuthenticationException {
        var existing = linkRepository.findById(id);
        if (existing.isEmpty()) {
            return;
        } else if (existing.map(e -> e.getAttrs().getUserId().equals(userId)).orElse(false)) {
            linkRepository.deleteById(id);
            return;
        }
        throw new AuthenticationException();
    }

    public List<LinkResource> retrieveRandom(String type, String[] tags, int count) {
        Criteria criteria = buildCriteria(type, tags);
        MatchOperation matchOp = Aggregation.match(criteria);
        SampleOperation sampleOp = Aggregation.sample(count);

        Aggregation aggregation = Aggregation.newAggregation(matchOp, sampleOp);

        var results = mongoTemplate.aggregate(aggregation, config.COMMAND_MINE_DB_COLLECTION, LinkResource.class);
        return results.getMappedResults();
    }

    public LinkResource retrieveOneRandom(String type, String[] tags) {
        var resources = retrieveRandom(type, tags, 1);
        return resources.isEmpty() ? null : resources.get(0);
    }

    private Criteria buildCriteria(String type, String[] tags) {
        Criteria criteria = new Criteria();

        if (type != null) {
            criteria = criteria.and("attrs.type").is(type);
        }
        if (tags.length > 0) {
            criteria = criteria.and("attrs.tags").in(tags);
        }

        return criteria;
    }
}
