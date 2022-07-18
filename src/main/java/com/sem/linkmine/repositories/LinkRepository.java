package com.sem.linkmine.repositories;

import com.sem.linkmine.models.LinkResource;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkRepository extends CrudRepository<LinkResource, ObjectId> {
    @Query("{ \"attrs.link\" : ?0 }")
    LinkResource findByLink(String link);
}
