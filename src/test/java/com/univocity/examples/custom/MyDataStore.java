/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.custom;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import com.univocity.api.entity.custom.*;
import com.univocity.examples.custom.MyDataStoreConfiguration.EntityConfig;

/**
 * This {@link CustomDataStore} demonstrates how you can implement your own data store for
 * abstract virtually any repository of information you want to integrate with uniVocity.
 *
 * We implemented simulated transactional support. It persists data of custom entities into a byte array
 * before any modification is made, and restores their data in case of errors.
 *
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 *
 */
public class MyDataStore implements CustomDataStore<MyReadOnlyEntity> {

	private final Set<MyQuery> queries = new HashSet<MyQuery>();
	private final Set<MyReadOnlyEntity> entities = new HashSet<MyReadOnlyEntity>();

	private final MyDataStoreConfiguration configuration;

	private Map<MyReadOnlyEntity, byte[]> dataInTransaction = null;

	//Creates a new custom data store and initializes custom entities based on our own configuration class.
	public MyDataStore(MyDataStoreConfiguration configuration) {
		this.configuration = configuration;

		createEntities();
		createQueries();
	}

	private void createQueries() {
		for (Entry<String, String> entry : configuration.queries.entrySet()) {
			String name = entry.getKey();
			String query = entry.getValue();
			queries.add(new MyQuery(name, query, this));
		}
	}

	private void createEntities() {
		for (EntityConfig config : configuration.entities) {
			MyReadOnlyEntity entity = null;
			if (config.readOnly) {
				//creates and initializes read only custom entities
				entity = new MyReadOnlyEntity(config.name, config.fields);
			} else {
				//creates and initializes custom entities
				entity = new MyEntity(this, config.name, config.fields);
			}

			//adds some data to the newly created entity
			for (Object[] values : config.getInitialData()) {
				entity.getMyData().insert(config.fields, values);
			}

			entities.add(entity);
		}
	}

	@Override
	public Set<MyReadOnlyEntity> getDataEntities() {
		return entities;

	}

	@Override
	public Set<? extends CustomQuery> getQueries() {
		return queries;
	}

	@Override
	public CustomQuery addQuery(String queryName, String query) {
		MyQuery newQuery = new MyQuery(queryName, query, this);
		queries.add(newQuery);
		return newQuery;
	}

	@Override
	public void executeInTransaction(TransactionalOperation operation) {
		if (dataInTransaction != null) {
			throw new IllegalStateException("There's already an active transaction on data store " + getConfiguration().getDataStoreName());
		}
		try {
			dataInTransaction = new HashMap<MyReadOnlyEntity, byte[]>();
			//You must always invoke this, even if you don't provide any transactional support.
			//All mapping operations in a data mapping cycle are contained within this TransactionOperation instance.
			//If you don't invoke the execute() method, no mapping will be executed.
			operation.execute();
		} catch (Exception ex) {
			rollbackChanges();
			throw new IllegalStateException("Aborting transaction due to exception", ex);
		} finally {
			dataInTransaction = null;
		}
	}

	/**
	 * Custom data entities will invoke this method from their parent data store in order to notify it
	 * their data will change. This data store will serialize their data into a byte array and in any error
	 * occurs in the transaction, the data store will automatically restore the data.
	 *
	 * @param entity the entity whose data will be modified by uniVocity within a {@link TransactionalOperation}
	 */
	void saveMyDataBeforeModifying(MyReadOnlyEntity entity) {

		if (dataInTransaction == null) {
			throw new IllegalStateException("No active transaction on data store " + getConfiguration().getDataStoreName());
		}
		//Serializes the entity data into a byte array.
		if (!dataInTransaction.containsKey(entity)) {
			try {
				ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

				ObjectOutputStream out = new ObjectOutputStream(byteOutput);
				out.writeObject(entity.getMyData());
				out.flush();
				out.close(); //we're not adding this into a finally block just to make the example easier to read.

				byte[] bytes = byteOutput.toByteArray();
				dataInTransaction.put(entity, bytes);

			} catch (Exception ex) {
				throw new IllegalStateException("Unable to save data of entity " + entity.getEntityName(), ex);
			}
		}
	}

	/**
	 * In case of errors while executing the {@link TransactionalOperation#execute()},
	 * this data store will restore the data serialized for each entity that got modified.
	 */
	private void rollbackChanges() {
		for (Entry<MyReadOnlyEntity, byte[]> entry : dataInTransaction.entrySet()) {
			MyReadOnlyEntity entity = entry.getKey();
			byte[] originalData = entry.getValue();

			try {
				ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(originalData));
				entity.setMyData((MyData) in.readObject());
			} catch (Exception ex) {
				throw new IllegalStateException("Unable to restore data of entity " + entity.getEntityName(), ex);
			}
		}
	}

	@Override
	public DataStoreConfiguration getConfiguration() {
		return configuration;
	}

}
