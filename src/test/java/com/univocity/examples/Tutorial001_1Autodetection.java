package com.univocity.examples;

import org.testng.annotations.*;

import com.univocity.api.*;
import com.univocity.api.config.builders.*;
import com.univocity.api.engine.*;

public class Tutorial001_1Autodetection extends ExampleWithOriginalDatabase {

	private static final String engineName = "engine_001_1";

	@Test
	public void example001AutodetectFields() {
		// initializes an engine with a CSV data store with the files under /examples/source_data/csv/
		// and a JDBC data store with the tables created with the scripts under /examples/original_schema/
		initializeEngine(engineName);

		DataIntegrationEngine engine = Univocity.getEngine(engineName);
		
		//##CODE_START
		//Let's create a mapping between the CSV and JDBC data stores
		DataStoreMapping dsMapping = engine.map("csvDataStore", "originalSchema");
		
		dsMapping.autodetectMappings();
		
		engine.executeCycle();

		print(printFoodGroupTable());
		print(printFoodDescriptionTable());
		
		//Finally, let's print all tables and see how the data looks like:
		printAndValidate();

		//##CODE_END
	}

}
