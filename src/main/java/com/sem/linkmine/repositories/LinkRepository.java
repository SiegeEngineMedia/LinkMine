package com.sem.linkmine.repositories;

import com.sem.linkmine.models.LinkResource;
import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkRepository extends CrudRepository<LinkResource, ObjectId> {
}
