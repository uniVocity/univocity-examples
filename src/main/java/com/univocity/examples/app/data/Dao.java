/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.app.data;

import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.jdbc.core.*;

import com.univocity.examples.utils.*;

public class Dao {

	private static final Logger log = LoggerFactory.getLogger(Dao.class);

	private final Database database;
	private final String tableName;
	private Set<String> primaryKeys;

	public Dao(Database database, String tableName) {
		this.database = database;
		this.tableName = tableName;
	}

	private void removeSuffix(StringBuilder script, String str) {
		if (script.toString().endsWith(str)) {
			script.delete(script.length() - str.length(), script.length());
		}
	}

	private String createDeleteScript(Set<Entry<String, Object>> matchingData) {
		final StringBuilder script = new StringBuilder("delete from ").append(tableName);

		script.append(createWhereClause(matchingData));

		return script.toString();
	}

	private String createWhereClause(Set<Entry<String, Object>> whereClauseEntries) {
		if (whereClauseEntries.isEmpty()) {
			return "";
		}

		final StringBuilder script = new StringBuilder(" where ");
		for (Entry<String, Object> e : whereClauseEntries) {
			script.append(e.getKey());
			script.append(" = ? and ");
		}

		removeSuffix(script, " and ");

		return script.toString();
	}

	private String createUpdateScript(Set<Entry<String, Object>> data, Set<Entry<String, Object>> matchingData) {
		final StringBuilder script = new StringBuilder("update ").append(tableName);

		if (!data.isEmpty()) {
			script.append(" set ");
			for (Entry<String, Object> e : data) {
				script.append(e.getKey());
				script.append(" = ?,");
			}
			removeSuffix(script, ",");
		}

		script.append(createWhereClause(matchingData));

		return script.toString();
	}

	private String createInsertScript(Set<Entry<String, Object>> newData) {
		final StringBuilder script = new StringBuilder("insert into ").append(tableName);

		if (newData.isEmpty()) {
			script.append("(null)");
		} else {
			script.append("(");
			for (Entry<String, Object> e : newData) {
				script.append(e.getKey());
				script.append(",");
			}
			removeSuffix(script, ",");

			script.append(") values (");

			for (@SuppressWarnings("unused")
			Entry<String, Object> e : newData) {
				script.append("?,");
			}
			removeSuffix(script, ",");

			script.append(")");
		}
		return script.toString();
	}

	private void execute(final Set<Entry<String, Object>> data, final Set<Entry<String, Object>> matchingEntries, final String script) {

		log.debug("Executing SQL: {}", script);

		database.getJdbcTemplate().execute(new ConnectionCallback<Void>() {
			@Override
			public Void doInConnection(Connection connection) throws SQLException, DataAccessException {
				PreparedStatement statement = connection.prepareStatement(script);
				try {
					int idx = 1;
					for (Entry<String, Object> e : data) {
						log.debug("Parameter {}: {}", idx, e);
						statement.setObject(idx++, e.getValue());
					}

					for (Entry<String, Object> e : matchingEntries) {
						log.debug("Parameter {}: {}", idx, e);
						statement.setObject(idx++, e.getValue());
					}

					statement.executeUpdate();
				} finally {
					statement.close();
				}

				return null;
			}
		});
	}

	private Set<Entry<String, Object>> extractPrimaryKeyValues(Map<String, Object> rowData) {
		Map<String, Object> idsToMatch = new HashMap<String, Object>();
		for (String id : getPrimaryKeys()) {
			Object idValue = rowData.remove(id);
			idsToMatch.put(id, idValue);
		}
		return idsToMatch.entrySet();
	}

	public Set<String> getPrimaryKeys() {
		if (primaryKeys != null) {
			return primaryKeys;
		}

		primaryKeys = new HashSet<String>();
		database.getJdbcTemplate().execute(new ConnectionCallback<Void>() {
			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				DatabaseMetaData metadata = con.getMetaData();
				//if the table name is in lower case it won't work (at least not with HSQLDB)
				ResultSet rs = metadata.getPrimaryKeys(null, null, tableName.toUpperCase());
				try {
					while (rs.next()) {
						primaryKeys.add(rs.getString("COLUMN_NAME").toLowerCase());
					}
				} finally {
					rs.close();
				}
				return null;
			}

		});

		return primaryKeys;
	}

	@SuppressWarnings("unchecked")
	public void insert(Map<String, Object> rowData) {
		final Set<Entry<String, Object>> entries = rowData.entrySet();
		final String insertScript = createInsertScript(entries);

		execute(entries, Collections.EMPTY_SET, insertScript);
	}

	@SuppressWarnings("unchecked")
	public void delete(Map<String, Object> rowData) {
		final Set<Entry<String, Object>> keys = extractPrimaryKeyValues(rowData);
		final String delete = createDeleteScript(keys);

		execute(Collections.EMPTY_SET, keys, delete);
	}

	public void update(Map<String, Object> rowData) {
		final Set<Entry<String, Object>> entries = rowData.entrySet();
		final Set<Entry<String, Object>> keys = extractPrimaryKeyValues(rowData);

		final String updateScript = createUpdateScript(entries, keys);

		execute(entries, keys, updateScript);
	}
}
