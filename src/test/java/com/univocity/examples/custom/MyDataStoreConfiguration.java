/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.custom;

import java.util.*;

import com.univocity.api.entity.custom.*;

/**
 * This is a configuration class for our custom data store {@link MyDataStore}.
 * It provides methods to add new custom data entities and queries ({@link MyReadOnlyEntity}, {@link MyEntity}, {@link MyQuery})
 *
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 *
 */
public class MyDataStoreConfiguration extends DataStoreConfiguration {

	class EntityConfig {
		final boolean readOnly;
		final String name;
		final String[] fields;
		private List<Object[]> initialData = Collections.emptyList();

		public EntityConfig(boolean readOnly, String name, String... fields) {
			this.name = name;
			this.fields = fields;
			this.readOnly = readOnly;
		}

		public void setInitialData(List<Object[]> initialData) {
			this.initialData = initialData;
		}

		public List<Object[]> getInitialData() {
			if (initialData == null) {
				return Collections.emptyList();
			}
			return initialData;
		}
	}

	final List<EntityConfig> entities = new ArrayList<EntityConfig>();
	final Map<String, String> queries = new HashMap<String, String>();

	public MyDataStoreConfiguration(String dataStoreName) {
		super(dataStoreName);
	}

	/**
	 * Creates a definition of new {@link MyEntity} and defines its fields
	 * @param name the name of the custom entity
	 * @param fields the fields of this entity
	 */
	public void addEntity(String name, String... fields) {
		entities.add(new EntityConfig(false, name, fields));
	}

	/**
	 * Creates a definition of new {@link MyReadOnlyEntity} and defines its fields
	 * @param name the name of the custom read-only entity
	 * @param fields the fields of this entity
	 */
	public void addReadOnlyEntity(String name, String... fields) {
		entities.add(new EntityConfig(true, name, fields));
	}

	/**
	 * Creates a definition of new {@link MyQuery}
	 * @param name the name of the custom query
	 * @param query the String that represents a selection of data from entities in this data store
	 */
	public void addQuery(String name, String query) {
		queries.put(name, query);
	}

	/**
	 * Defines the initial data of an entity configured in this data store.
	 * @param name the name of a custom data entity in this data store
	 * @param initialData the records used to initialize the given entity.
	 */
	public void setEntityData(String name, List<Object[]> initialData) {
		for (EntityConfig entity : entities) {
			if (entity.name.equals(name)) {
				entity.setInitialData(initialData);
				return;
			}
		}
		throw new IllegalArgumentException("Unknown entity name: " + name);
	}

	/**
	 * We don't really care about the number of rows to load in memory in the example, as all data handled by our custom entities is in memory already
	 * Let's just return a value that will avoid preallocating big chunks of memory unnecessarily.
	 */
	@Override
	public int getLimitOfRowsLoadedInMemory() {
		return 100;
	}
}
