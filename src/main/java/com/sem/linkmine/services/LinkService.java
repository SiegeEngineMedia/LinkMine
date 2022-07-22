package com.sem.linkmine.services;

import com.sem.linkmine.errors.AuthenticationException;
import com.sem.linkmine.models.LinkModel;
import com.sem.linkmine.models.LinkResource;
import com.sem.linkmine.repositories.LinkRepository;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LinkService {
    private final MongoTemplate mongoTemplate;
    private final LinkRepository linkRepository;
    private final ConstantsService constants;

    public LinkService(MongoTemplate mongoTemplate, LinkRepository linkRepository, ConstantsService constants) {
        this.mongoTemplate = mongoTemplate;
        this.linkRepository = linkRepository;
        this.constants = constants;
    }

    public LinkResource insert(LinkModel attrs) throws AuthenticationException {
        return tryUpsert(ObjectId.get(), attrs);
    }

    public LinkResource update(ObjectId id, LinkModel attrs) throws AuthenticationException {
        return tryUpsert(id, attrs);
    }

    public List<CountObject> getCounts() {
//        Aggregation aggregation = Aggregation.newAggregation(
//                Aggregation.unwind("$attrs.tags"),
//                Aggregation.group("$attrs.tags").sum("count")
//        );
//        return mongoTemplate.aggregate(aggregation, CountObject.class);
        return new ArrayList<CountObject>();
    }

    public void deleteById(ObjectId id, String userId) throws AuthenticationException {
        var existingOption = linkRepository.findById(id);
        if (existingOption.isPresent()) {
            delete(existingOption.get(), userId);
            return;
        }
        throw new AuthenticationException();
    }

    public void deleteByLink(String link, String userId) throws AuthenticationException {
        var existingOption = linkRepository.findByLink(link);
        if (existingOption.isPresent()) {
            delete(existingOption.get(), userId);
            return;
        }
        throw new AuthenticationException();
    }

    private void delete(LinkResource resource, String userId) throws AuthenticationException {
        if (resource.getAttrs().getUserId().equals(userId)) {
            linkRepository.deleteById(resource.getId());
            return;
        }
        throw new AuthenticationException();
    }

    public Optional<LinkResource> findById(ObjectId id) {
        return linkRepository.findById(id);
    }

    public List<LinkResource> retrieveRandom(String type, String[] tags, int count) {
        Criteria criteria = buildCriteria(type, tags);
        MatchOperation matchOp = Aggregation.match(criteria);
        SampleOperation sampleOp = Aggregation.sample(count);

        Aggregation aggregation = Aggregation.newAggregation(matchOp, sampleOp);

        var results = mongoTemplate.aggregate(aggregation, constants.LINKS_COLLECTION_NAME, LinkResource.class);
        return results.getMappedResults();
    }

    public LinkResource retrieveOneRandom(String type, String[] tags) {
        var resources = retrieveRandom(type, tags, 1);
        return resources.isEmpty() ? null : resources.get(0);
    }

    private LinkResource tryUpsert(ObjectId id, LinkModel attrs) throws AuthenticationException {
        var existingOption = linkRepository.findByLink(attrs.getLink());
        if (existingOption.map(e -> e.getId().equals(id)).orElse(false)) {
            var existing = existingOption.get();
            if (existing.getAttrs().getUserId().equals(attrs.getUserId())) {
                var existingQuery = Query.query(Criteria.where("_id").is(existing.getId()));
                var existingUpdate = Update.update("attrs", attrs);
                return mongoTemplate.findAndModify(existingQuery, existingUpdate, LinkResource.class, constants.LINKS_COLLECTION_NAME);
            }
            throw new AuthenticationException();
        }
        return linkRepository.save(new LinkResource(id, attrs));
    }

    private Criteria buildCriteria(String type, String[] tags) {
        Criteria criteria = new Criteria();

        if (type != null) {
            criteria = criteria.and("attrs.type").is(type);
        }
        if (tags.length > 0) {
            criteria = criteria.and("attrs.tags").in((Object[]) tags);
        }

        return criteria;
    }
}

// TODO: fix me somewhere better
class CountObject {
    public String _id;
    public Integer count;
}