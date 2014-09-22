/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.utils;

import javax.sql.*;

public class DatabaseAccessor {

	private final String databaseName;
	private final String ddlDirectory;
	private final String queryProperties;

	private Database database;
	private Queries queries;

	public DatabaseAccessor(String databaseName, String ddlDirectory) {
		this(databaseName, ddlDirectory, null);
	}

	public DatabaseAccessor(String databaseName, String ddlDirectory, String queryProperties) {
		if (databaseName == null || databaseName.trim().isEmpty()) {
			throw new IllegalStateException("Database name cannot be null or empty");
		}
		this.databaseName = databaseName;
		this.ddlDirectory = ddlDirectory;
		this.queryProperties = queryProperties;
	}

	public String getDdlDirectory() {
		return ddlDirectory;
	}

	public String getQueryProperties() {
		return queryProperties;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public Queries getQueries() {
		if (queries == null) {
			if (queryProperties == null) {
				queries = new QueriesImpl();
			}
			queries = new QueriesImpl(FileFinder.findFile(queryProperties));
		}
		return queries;
	}

	public Database getDatabase() {
		if (database == null) {
			if (ddlDirectory == null) {
				throw new IllegalStateException("Database definition directory not set");
			}
			database = new DatabaseImpl(databaseName, FileFinder.findFile(ddlDirectory));
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					shutdown();
				}
			});
		}
		return database;
	}

	public void shutdown() {
		if (database == null) {
			return;
		}
		database.getJdbcTemplate().execute("SHUTDOWN");
		database = null;
	}

	public DataSource getDataSource() {
		return getDatabase().getDataSource();
	}

}
