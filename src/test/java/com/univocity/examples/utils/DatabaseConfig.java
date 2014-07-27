/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.utils;

public class DatabaseConfig {

	private final String databaseName;
	private String ddlDirectory;
	private String queryProperties;

	private Database database;
	private Queries queries;

	public DatabaseConfig(String databaseName) {
		if (databaseName == null || databaseName.trim().isEmpty()) {
			throw new IllegalStateException("Database name cannot be null or empty");
		}
		this.databaseName = databaseName;
	}

	public String getDdlDirectory() {
		return ddlDirectory;
	}

	public void setDdlDirectory(String ddlDirectory) {
		this.ddlDirectory = ddlDirectory;
	}

	public String getQueryProperties() {
		return queryProperties;
	}

	public void setQueryProperties(String queryProperties) {
		this.queryProperties = queryProperties;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public Queries getQueries() {
		if (queries == null) {
			if (queryProperties == null) {
				queries = new Queries();
			}
			queries = new Queries(queryProperties);
		}
		return queries;
	}

	public Database getDatabase() {
		if (database == null) {
			if (ddlDirectory == null) {
				throw new IllegalStateException("Database definition directory not set");
			}
			database = new Database(databaseName, ddlDirectory);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					database.shutdown();
				}
			});
		}
		return database;
	}
}
