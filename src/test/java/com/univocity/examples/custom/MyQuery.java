/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.custom;

import java.util.*;

import com.univocity.api.entity.custom.*;

/**
 *
 * A custom query implementation that extends from {@link MyReadOnlyEntity} and translates String in the format
 *
 * <code>
 * entityName (field1, field2, ...) with (field1 = ?, field2 = ?, ...)
 * </code>
 *
 * This custom query will simply match the parameters provided for each field name in the "with" clause
 * and return the selected values from records that match this criteria.
 *
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 *
 */
public class MyQuery extends MyReadOnlyEntity implements CustomQuery {

	private final MyReadOnlyEntity queriedEntity;
	private final String[] parameterSequence;
	private final Map<String, Object> parameters = new HashMap<String, Object>();

	public MyQuery(String name, String query, MyDataStore dataStore) {
		super(name, getFieldsToRead(query));

		//obtains the entity from where to extract data
		this.queriedEntity = getEntity(query, dataStore);

		//identifies the sequence of parameters in this query
		this.parameterSequence = getParameters(query);

		//initializes the map of parameters assigning null to each
		for (String parameter : parameterSequence) {
			parameters.put(parameter, null);
		}
	}

	private MyReadOnlyEntity getEntity(String query, MyDataStore dataStore) {
		//parses the query String to identify what entity is being queried
		int fieldStart = query.indexOf('(');
		String entityName = "";

		if (fieldStart > 0) {
			entityName = query.substring(0, fieldStart).trim();

			//gets the entity instance from the data store
			for (MyReadOnlyEntity entity : dataStore.getDataEntities()) {
				if (entity.getEntityName().equals(entityName)) {
					return entity;
				}
			}
		}
		throw new IllegalArgumentException("Cannot determine target entity " + entityName + " using query " + query);
	}

	private static String[] getElementsBetween(String string, int start, int end) {
		String[] elements = string.substring(start, end).split(",");
		for (int i = 0; i < elements.length; i++) {
			elements[i] = elements[i].trim();
		}
		return elements;
	}

	private static String[] getFieldsToRead(String query) {
		int fieldStart = query.indexOf('(') + 1;
		int fieldEnd = query.indexOf(')', fieldStart);

		return getElementsBetween(query, fieldStart, fieldEnd);
	}

	private static String[] getParameters(String query) {
		int paramEnd = query.lastIndexOf(')');
		int paramStart = query.lastIndexOf('(', paramEnd) + 1;

		String[] parameters = getElementsBetween(query, paramStart, paramEnd);
		for (int i = 0; i < parameters.length; i++) {
			String[] pair = parameters[i].split("=");
			parameters[i] = pair[0].trim();
		}
		return parameters;
	}

	@Override
	public String[] getParameters() {
		return parameterSequence;
	}

	@Override
	public void setParameter(String parameterName, Object parameterValue) {
		if (parameters.containsKey(parameterName)) {
			parameters.put(parameterName, parameterValue);
		}
	}

	/**
	 * Builds an array of values that form a search criteria
	 * @return obtains the sequence of values to match against the queried entity records
	 */
	private Object[] getParameterValues() {
		Object[] parameterValues = new Object[parameterSequence.length];
		for (int i = 0; i < parameterSequence.length; i++) {
			String parameterName = parameterSequence[i];
			Object parameterValue = parameters.get(parameterName);
			parameterValues[i] = parameterValue;
		}
		return parameterValues;
	}

	@Override
	public ReadingProcess preareToRead(final String[] fieldNames) {
		//gets queried entity data. The reading process will iterate over it to identify what records to return
		final MyData entityData = queriedEntity.getMyData();

		return new ReadingProcess() {
			final Iterator<Map<String, Object>> dataIterator = entityData.iterator();
			final Object[] parameterValues = getParameterValues();

			@Override
			public void close() {
			}

			@Override
			public Object[] readNext() {
				while (dataIterator.hasNext()) {
					Map<String, Object> record = dataIterator.next();
					//matches the record against the values set in the query parameters.
					if (entityData.matches(record, parameterSequence, parameterValues)) {
						//returns the matched record
						return entityData.toRow(record, fieldNames);
					}
				}
				// return null to notify uniVocity there's no more records.
				return null;
			}
		};
	}
}
