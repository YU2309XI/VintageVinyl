package com.vintagevinyl.service;

import java.util.List;

public interface GenericService<T, ID> {
    // Retrieve all entities of type T
    List<T> getAll();

    // Retrieve an entity by its ID
    T getById(ID id);

    // Save a new entity or update an existing one
    T save(T entity);

    // Delete an entity by its ID
    void delete(ID id);
}
