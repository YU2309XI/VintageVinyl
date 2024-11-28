package com.vintagevinyl.service;

import java.util.List;

/**
 * Generic service interface for CRUD operations.
 *
 * This interface provides a template for services managing entities of type T
 * with an identifier of type ID. It defines common operations such as retrieving,
 * saving, and deleting entities.
 *
 * @param <T> the type of the entity
 * @param <ID> the type of the entity's identifier
 */
public interface GenericService<T, ID> {

    /**
     * Retrieve all entities of type T.
     *
     * @return a List of all entities
     */
    List<T> getAll();

    /**
     * Retrieve an entity by its ID.
     *
     * @param id the ID of the entity to retrieve
     * @return the entity with the specified ID
     */
    T getById(ID id);

    /**
     * Save a new entity or update an existing one.
     *
     * @param entity the entity to save or update
     * @return the saved or updated entity
     */
    T save(T entity);

    /**
     * Delete an entity by its ID.
     *
     * @param id the ID of the entity to delete
     */
    void delete(ID id);
}
