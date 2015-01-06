/*******************************************************************************
 * Copyright (c) 2015 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples;

import java.io.*;
import java.util.*;

import javax.sql.*;

import org.springframework.jdbc.core.*;

import com.univocity.api.*;
import com.univocity.api.config.*;
import com.univocity.api.entity.jdbc.*;
import com.univocity.api.entity.text.csv.*;
import com.univocity.app.utils.*;
import com.univocity.parsers.fixed.*;

public class ExampleWithDatabase extends Example {
	protected DataSource dataSource;

	private final String schemaDirPath;
	private final String dataStoreName;

	public ExampleWithDatabase(String schemaDirPath, String dataStoreName) {
		this.schemaDirPath = schemaDirPath;
		this.dataStoreName = dataStoreName;
	}

	protected JdbcDataStoreConfiguration newDatabaseConfiguration() {
		// The next three lines of code use some utility classes we created to easily
		// create and initialize in-memory databases for testing purposes.

		//creates an in-memory database with a random name and a directory with table definition scripts.
		DatabaseAccessor destinationConfig = new DatabaseAccessor(UUID.randomUUID().toString(), schemaDirPath);

		//initializes the database, creates all tables and returns a javax.sql.DataSource
		dataSource = destinationConfig.getDatabase().getDataSource();

		//Back to uniVocity, here we create a data store configuration for database entities.
		//It offers a lot of configuration options, but for this example, we will just need the following:

		//##CODE_START
		//Creates a new JDBC data store based on a javax.sql.DataSource, with the name "new_schema".
		//Upon initialization, uniVocity will try to auto-detect all available tables, columns and primary keys
		//If this doesn't work you still can configure each table manually.
		JdbcDataStoreConfiguration newSchemaDataStore = new JdbcDataStoreConfiguration(dataStoreName, dataSource);

		//The database contains lots of internal tables in addition to the tables we are interested in
		//By setting the schema to "public", these internal database tables won't be made available to uniVocity.
		newSchemaDataStore.setSchema("public");
		//##CODE_END
		return newSchemaDataStore;
	}

	@Override
	protected void initializeEngine(String engineName) {
		CsvDataStoreConfiguration csvDataStore = getCsvDataStore();
		JdbcDataStoreConfiguration newSchemaDataStore = newDatabaseConfiguration();

		//##CODE_START
		//Creates a new engine configuration to map data between the entities in CSV and JDBC data stores
		EngineConfiguration engineConfig = new EngineConfiguration(engineName, csvDataStore, newSchemaDataStore);

		//Registers this engine configuration.
		Univocity.registerEngine(engineConfig);
		//##CODE_END
	}

	private FixedWidthFieldLengths calculateFieldLengths(Map<String, Object> firstRow, String... columns) {

		FixedWidthFieldLengths fieldLengths = new FixedWidthFieldLengths();

		for (String column : columns) {
			int length = column.length();
			String value = String.valueOf(firstRow.get(column));
			if (length < value.length()) {
				length = value.length() * 3;
			}
			fieldLengths.addField(length + 3);
		}

		return fieldLengths;
	}

	protected final String printRows(List<Map<String, Object>> results, String table, String... columns) {
		FixedWidthFieldLengths fieldLengths = calculateFieldLengths(results.get(0), columns);
		FixedWidthWriterSettings writerSettings = new FixedWidthWriterSettings(fieldLengths);
		writerSettings.setHeaders(columns);
		writerSettings.getFormat().setPadding('_');

		StringWriter output = new StringWriter();
		FixedWidthWriter writer = new FixedWidthWriter(output, writerSettings);
		writer.writeRow("===[ " + table + " ]===");

		writer.writeHeaders();

		for (Map<String, Object> row : results) {
			Object[] line = new Object[columns.length];
			for (int i = 0; i < columns.length; i++) {
				line[i] = row.get(columns[i]);
			}
			writer.writeRow(line);
		}
		writer.writeEmptyRow();
		writer.close();

		return output.toString();
	}

	protected final String printTable(String table, String... columns) {
		JdbcTemplate db = new JdbcTemplate(dataSource);

		StringBuilder order = new StringBuilder();
		for (String column : columns) {
			if (order.length() != 0) {
				order.append(',');
			}
			order.append(column);
		}
		String names = order.toString();
		List<Map<String, Object>> results = db.queryForList("select " + names + " from " + table + " order by " + names);

		return printRows(results, table, columns);
	}
}
