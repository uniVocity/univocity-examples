package com.univocity.app;

import static org.testng.Assert.*;

import java.util.*;

import org.apache.commons.collections.*;
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
				getParams("FD_GROUP", null, "CHG_FDGP", "FdGrp_CD"),
				getParams("FOOD_DES", "ADD_FOOD", "CHG_FOOD", "NDB_No"),
				getParams("NUTR_DEF", "ADD_NDEF", "CHG_NDEF", "Nutr_No"),
				getParams("WEIGHT", "ADD_WGT", "CHG_WGT", "NDB_No", "Seq"),
				getParams("NUT_DATA", "ADD_NUTR", "CHG_NUTR", "NDB_No", "Nutr_No"),
		};
	}

	private Object[] getParams(String entityName, String insertFile, String updateFile, String... ids) {
		DataStoreMapping mappings = engine.getMapping("data", "source");
		return new Object[] { mappings.getMapping(entityName, entityName), new RowDataCollector(ids), entityName, insertFile, updateFile };
	}

	@Test(dataProvider = "entityProvider")
	public void testDataUpdate(EntityMapping mapping, RowDataCollector dataCollector, String entityName, String insertFile, String updateFile) {

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

	private void executeAndValidate(String entityName, boolean inserting, RowDataCollector dataCollector) {
		System.gc();
		loadProcess.execute(entityName);

		List<String> persistedRows = dataCollector.getPersistedData();
		List<String> expectedRows = dataCollector.getExpected();

		if (!expectedRows.containsAll(persistedRows)) {
			@SuppressWarnings("unchecked")
			Collection<String> intersection = CollectionUtils.intersection(persistedRows, expectedRows);

			persistedRows.removeAll(intersection);
			expectedRows.removeAll(intersection);

			if(!isExpectedDiscrepancy(entityName, inserting, persistedRows)){
				fail("Unexpected rows persisted:\n" + persistedRows +"\n\nRows expected but not persisted:\n" + expectedRows);				
			}
		}
		System.gc();
	}

	private boolean isExpectedDiscrepancy(String entityName, boolean inserting, Collection<String> ids) {
		RowToMap processor = new RowToMap();
		parserSettings.setRowProcessor(processor);

		DataStores.getInstance().getSourceData().setOldVersion(true);
		new CsvParser(parserSettings).parse(DataStores.getInstance().getSourceData().openFile(entityName));
		Map<String, String> sr25Rows = processor.getMap();

		DataStores.getInstance().getSourceData().setOldVersion(false);
		new CsvParser(parserSettings).parse(DataStores.getInstance().getSourceData().openFile(entityName));
		Map<String, String> sr26Rows = processor.getMap();

		List<String> newRows = new ArrayList<String>();
		List<String> updatedRows = new ArrayList<String>();
		List<String> unnecessaryUpdates = new ArrayList<String>();

		Set<String> disconsider = new HashSet<String>();
		
		for (String rowId : ids) {
			String r25 = sr25Rows.get(rowId);
			String r26 = sr26Rows.get(rowId);

			assertFalse(r25 == null && r26 == null); //can't have a rowID from nowhere.

			if (!inserting && r25 != null && r26 != null) { //it is an update
				if(r25.equals(r26)){
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
		if(rows.isEmpty()){
			return;
		}
	
		System.err.println("Discrepancy detected in update files of entity " + entityName + ": Detected " + rows.size() + " " + description);
		
		for(String row : rows){
			System.err.println(row);
		}
	}
}
