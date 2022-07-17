package com.sem.linkmine.models;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

public class Resource<T> {
    @Id
    private ObjectId _id;
    private T attrs;

    public ObjectId getId() {
        return _id;
    }

    public T getAttrs() {
        return attrs;
    }
}