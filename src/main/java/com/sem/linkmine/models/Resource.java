package com.sem.linkmine.models;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

public class Resource<T> {
    @Id
    private ObjectId _id;
    private T attrs;

    public Resource() {
        this._id = ObjectId.get();
        this.attrs = null;
    }

    public Resource(ObjectId id, T attrs) {
        this._id = id;
        this.attrs = attrs;
    }

    public Resource(T attrs) {
        this(ObjectId.get(), attrs);
    }

    public ObjectId getId() {
        return _id;
    }

    public T getAttrs() {
        return attrs;
    }
}