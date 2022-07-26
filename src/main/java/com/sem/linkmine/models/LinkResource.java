package com.sem.linkmine.models;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("links")
public class LinkResource extends Resource<LinkModel> {
    public LinkResource() {
        super();
    }

    public LinkResource(ObjectId id, LinkModel attrs) {
        super(id, attrs);
    }

    public LinkResource(LinkModel attrs) {
        super(ObjectId.get(), attrs);
    }
}
