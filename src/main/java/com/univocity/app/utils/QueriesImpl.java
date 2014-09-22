/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.utils;

import java.io.*;
import java.util.*;

class QueriesImpl implements Queries {

	private final Map<String, String> queries = new TreeMap<String, String>();

	public QueriesImpl() {

	}

	public QueriesImpl(File queriesProperties) {
		Properties properties = new Properties();

		try {
			properties.load(new FileInputStream(queriesProperties));
		} catch (IOException e) {
			throw new IllegalArgumentException("Error loading query properties from: " + queriesProperties, e);
		}

		for (Object key : properties.keySet()) {
			String property = String.valueOf(key).trim();
			String queryName = null;
			String query = null;

			String propertyName = null;
			int dotIndex = property.indexOf('.');
			if (dotIndex != -1) {
				propertyName = property.substring(0, dotIndex);
			} else {
				throw new IllegalArgumentException("Invalid property in " + queriesProperties + ": " + property);
			}

			if (property.endsWith(".name")) {
				queryName = properties.getProperty(property);
				query = properties.getProperty(propertyName + ".query");
				if (query == null) {
					throw new IllegalArgumentException("No query defined for name " + queryName);
				}
			} else if (property.endsWith(".query")) {
				query = properties.getProperty(property);
				queryName = properties.getProperty(propertyName + ".name");
				if (queryName == null) {
					throw new IllegalArgumentException("No name defined for query " + query);
				}
			}

			if (queryName == null || query == null) {
				throw new IllegalArgumentException("Invalid property in " + queriesProperties + ": " + property);
			}

			queries.put(queryName, query);
		}
	}

	@Override
	public Set<String> getQueryNames() {
		return Collections.unmodifiableSet(queries.keySet());
	}

	@Override
	public String getQuery(String name) {
		return queries.get(name);
	}
}
