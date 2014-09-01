/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app;

import static org.testng.Assert.*;

import java.io.*;
import java.util.*;

import javax.sql.*;

import org.apache.commons.collections.*;
import org.apache.commons.lang.*;
import org.springframework.jdbc.core.*;
import org.testng.annotations.*;

import com.univocity.api.*;
import com.univocity.api.config.builders.*;
import com.univocity.api.engine.*;
import com.univocity.app.etl.*;
import com.univocity.app.etl.datastores.*;
import com.univocity.parsers.csv.*;

public class DataUpdateTest {

	private LoadSourceDatabase loadProcess;
	private DataIntegrationEngine engine;

	private CsvParserSettings parserSettings;

	@BeforeClass
	private void initialize() {
		loadProcess = new LoadSourceDatabase();

		engine = Univocity.getEngine(loadProcess.getEngineName());

		DataStores.getInstance().getSourceData().setOldVersion(true);
		loadProcess.execute("FD_GROUP", "FOOD_DES", "NUTR_DEF", "WEIGHT", "NUT_DATA");

		DataStores.getInstance().getSourceData().setOldVersion(false);

		parserSettings = new CsvParserSettings();
		parserSettings.getFormat().setDelimiter('^');
		parserSettings.getFormat().setQuote('~');
		parserSettings.getFormat().setQuoteEscape('~');
		parserSettings.setHeaderExtractionEnabled(false);
	}

	@DataProvider(name = "entityProvider")
	public Object[][] entityProvider() {
		return new Object[][] {
				getParams("FD_GROUP", null, "CHG_FDGP", null, "FdGrp_CD"),
				getParams("FOOD_DES", "ADD_FOOD", "CHG_FOOD", "DEL_FOOD", "NDB_No"),
				getParams("NUTR_DEF", "ADD_NDEF", "CHG_NDEF", null, "Nutr_No"),
				getParams("WEIGHT", "ADD_WGT", "CHG_WGT", "DEL_WGT", "NDB_No", "Seq"),
				getParams("NUT_DATA", "ADD_NUTR", "CHG_NUTR", "DEL_NUTR", "NDB_No", "Nutr_No"),
		};
	}

	private Object[] getParams(String entityName, String insertFile, String updateFile, String deleteFile, String... ids) {
		DataStoreMapping mappings = engine.getMapping("data", "source");
		return new Object[] { mappings.getMapping(entityName, entityName), new RowDataCollector(ids), entityName, insertFile, updateFile, deleteFile };
	}

	@DataProvider(name = "absentProvider")
	public Object[][] absentProvider() {
		Object[][] params = entityProvider();
		ArrayUtils.reverse(params);
		return params;
	}

	@Test(dataProvider = "entityProvider")
	public void testDataUpdate(EntityMapping mapping, RowDataCollector dataCollector, String entityName, String insertFile, String updateFile, String deleteFile) {
		//to get ids of persisted rows
		mapping.addPersistedRowReader(dataCollector);

		if (insertFile != null) {
			mapping.persistence().usingMetadata().deleteDisabled().updateDisabled().insertNewRows();

			//populates list with IDs of expected rows to be inserted
			parserSettings.setRowProcessor(dataCollector);
			new CsvParser(parserSettings).parse(DataStores.getInstance().getSourceData().openUpdateFile(insertFile));

			//inserting
			executeAndValidate(entityName, true, dataCollector);
		}

		if (updateFile != null) {
			mapping.persistence().usingMetadata().deleteDisabled().updateModified().insertDisabled();

			//populates list with IDs of expected rows to be updated
			parserSettings.setRowProcessor(dataCollector);
			new CsvParser(parserSettings).parse(DataStores.getInstance().getSourceData().openUpdateFile(updateFile));

			//updating
			executeAndValidate(entityName, false, dataCollector);
		}
	}

	@Test(dataProvider = "absentProvider", dependsOnMethods = "testDataUpdate")
	public void testDeleteAbsent(EntityMapping mapping, RowDataCollector dataCollector, String entityName, String insertFile, String updateFile, String deleteFile) {
		System.gc();
		mapping.persistence().usingMetadata().deleteAbsent().updateDisabled().insertDisabled();
		loadProcess.execute(entityName);
		validateAbsentRecordsGotRemoved(entityName, dataCollector, deleteFile);
		System.gc();
	}

	private void executeAndValidate(String entityName, boolean inserting, RowDataCollector dataCollector) {
		System.gc();
		loadProcess.execute(entityName);

		Set<String> persistedRows = dataCollector.getPersistedData();
		Set<String> expectedRows = dataCollector.getExpected();

		if (!expectedRows.containsAll(persistedRows)) {
			@SuppressWarnings("unchecked")
			Collection<String> intersection = CollectionUtils.intersection(persistedRows, expectedRows);

			persistedRows.removeAll(intersection);
			expectedRows.removeAll(intersection);

			if (!isExpectedDiscrepancy(entityName, dataCollector.getFieldNames(), inserting, persistedRows)) {
				fail("Unexpected rows persisted:\n" + persistedRows.size() + "\n\nRows expected but not persisted:\n" + expectedRows.size());
			}
		}
		System.gc();
	}

	private Map<String, String> getMapOfRows(String entityName, String[] fieldNames, boolean oldVersion) {
		RowToMap processor = new RowToMap(fieldNames);
		parserSettings.setRowProcessor(processor);

		SourceData sourceData = DataStores.getInstance().getSourceData();
		sourceData.setOldVersion(oldVersion);
		Reader reader = DataStores.getInstance().getSourceData().openFile(entityName);
		new CsvParser(parserSettings).parse(reader);

		return processor.getMap();
	}

	private boolean isExpectedDiscrepancy(String entityName, String[] fieldNames, boolean inserting, Collection<String> ids) {

		Map<String, String> sr25Rows = getMapOfRows(entityName, fieldNames, true);
		Map<String, String> sr26Rows = getMapOfRows(entityName, fieldNames, false);

		List<String> newRows = new ArrayList<String>();
		List<String> updatedRows = new ArrayList<String>();
		List<String> unnecessaryUpdates = new ArrayList<String>();

		Set<String> disconsider = new HashSet<String>();

		for (String rowId : ids) {
			String r25 = sr25Rows.get(rowId);
			String r26 = sr26Rows.get(rowId);

			assertFalse(r25 == null && r26 == null); //can't have a rowID from nowhere.

			if (!inserting && r25 != null && r26 != null) { //it is an update
				if (r25.equals(r26)) {
					disconsider.add(rowId);
					unnecessaryUpdates.add(r26 + " - no changes since SR25");
				} else {
					disconsider.add(rowId);
					updatedRows.add(r26 + " - changed from SR25: " + r25);
				}
			}

			if (inserting && r25 == null && r26 != null) { //new row in SR26
				disconsider.add(rowId);
				newRows.add(r26);
			}

		}

		printDiscrepantRows(entityName, newRows, "new rows");
		printDiscrepantRows(entityName, updatedRows, "updated rows");
		printDiscrepantRows(entityName, unnecessaryUpdates, "unnecessary updates");

		ids.removeAll(disconsider);
		return ids.isEmpty();
	}

	private void printDiscrepantRows(String entityName, List<String> rows, String description) {
		if (rows.isEmpty()) {
			return;
		}

		System.err.println("Discrepancy detected in update files of entity " + entityName + ": Detected " + rows.size() + " " + description);

		for (String row : rows) {
			System.err.println(row);
		}
	}

	private void validateAbsentRecordsGotRemoved(String entityName, RowDataCollector dataCollector, String deleteFile) {

		String[] fieldNames = dataCollector.getFieldNames();

		DataSource dataSource = DataStores.getInstance().getSourceDatabase().getDataSource();
		List<Map<String, Object>> results = new JdbcTemplate(dataSource).queryForList("SELECT " + toString(fieldNames).replace('^', ',') + " FROM " + entityName);

		Set<String> idsOnDatabase = new HashSet<String>(results.size());

		for (Map<String, Object> e : results) {
			String id = toString(fieldNames, e);
			idsOnDatabase.add(id);
		}

		System.gc();

		if (deleteFile == null) {
			Set<String> sr25Rows = getMapOfRows(entityName, fieldNames, true).keySet();
			Set<String> sr26Rows = getMapOfRows(entityName, fieldNames, false).keySet();

			@SuppressWarnings("unchecked")
			Collection<String> commonIds = CollectionUtils.intersection(sr25Rows, sr26Rows);
			sr25Rows.removeAll(commonIds);
			sr26Rows.removeAll(commonIds);

			sr25Rows = new TreeSet<String>(sr25Rows);
			sr26Rows = new TreeSet<String>(sr26Rows);

			for (String id : idsOnDatabase) {
				//table does not retain ids that are not in SR26
				assertFalse(sr25Rows.contains(id));

				sr26Rows.remove(id);
			}

			// ensures no unexpected ID is found on SR26.
			assertTrue(sr26Rows.isEmpty());
		} else {
			parserSettings.setRowProcessor(dataCollector);
			new CsvParser(parserSettings).parse(DataStores.getInstance().getSourceData().openUpdateFile(deleteFile));
			Set<String> expectedToBeRemoved = dataCollector.getExpected();

			for (String toBeRemoved : expectedToBeRemoved) {
				assertFalse(idsOnDatabase.contains(toBeRemoved));
			}
		}
	}

	private String toString(String[] fieldNames, Map<String, Object> map) {
		if (fieldNames.length == 1) {
			return get(map, fieldNames[0]);
		} else {
			return get(map, fieldNames[0]) + "^" + get(map, fieldNames[1]);
		}
	}

	private String get(Map<String, Object> map, String key) {
		return map.get(key).toString().trim();
	}

	private String toString(String[] fieldNames) {
		if (fieldNames.length == 1) {
			return fieldNames[0];
		} else {
			return fieldNames[0] + '^' + fieldNames[1];
		}
	}
}
