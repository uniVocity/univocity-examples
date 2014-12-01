/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples;

import com.univocity.api.*;
import com.univocity.api.common.*;
import com.univocity.api.config.*;
import com.univocity.api.entity.text.csv.*;
import com.univocity.api.entity.text.fixed.*;
import com.univocity.test.*;

/**
 * Just a parent class for all examples provided, with basic entity configurations and some utility methods to validate outputs.
 */
abstract class Example extends OutputTester {

	public Example() {
		super("examples/expectedOutputs/", "UTF-8");
	}

	// To make testing easier and clearer, all data written in the example test cases will end up in a String
	protected StringWriterProvider foodGroupOutput;
	protected StringWriterProvider foodOutput;

	protected CsvDataStoreConfiguration getCsvDataStore() {
		//##CODE_START
		//This creates a new data store for CSV files. uniVocity provides a few basic data stores out of the box,
		//but you can always create your own (see com.univocity.api.entity.custom.CustomDataStoreFactory)
		CsvDataStoreConfiguration csvDataStore = new CsvDataStoreConfiguration("csvDataStore");

		//This adds all files under the specified path to the data store. Each file will be
		//considered an individual data entity.
		csvDataStore.addEntities("examples/source_data/csv");
		//##CODE_END
		return csvDataStore;
	}

	protected FixedWidthDataStoreConfiguration getFixedWidthDataStore() {
		//##CODE_START
		//This creates a new data store for fixed-width entities. Let's call it "fixedWidthDestination"
		FixedWidthDataStoreConfiguration fixedWidthDataStore = new FixedWidthDataStoreConfiguration("fixedWidthDestination");

		//Here we create a write-only fixed-with entity named "food_group". All data written to this entity
		//will end up in a String. You can also use files, resources in the classpath or custom writers/readers as entities.
		foodGroupOutput = new StringWriterProvider();
		fixedWidthDataStore.addEntity("food_group", foodGroupOutput);

		//We will also need a "food" entity.
		foodOutput = new StringWriterProvider();
		fixedWidthDataStore.addEntity("food", foodOutput);

		//Let's define the default settings that should be used for all entities in this data store
		FixedWidthEntityConfiguration defaults = fixedWidthDataStore.getDefaultEntityConfiguration();

		//By default, we want to write headers to the output (i.e. the first row that displays the name of each column).
		defaults.setHeaderWritingEnabled(true);

		//We also want underscore (instead of whitespace) to highlight unwritten spaces in the fixed-width fields.
		defaults.getFormat().setPadding('_');

		//Use a question mark to denote null values in the output (instead of simply leaving it blank in the output)
		defaults.setNullValue("?");

		//Let's configure the entities: uniVocity needs to know what the records of each entity look like.
		//As these are not files with headers, nor database tables, we need to provide this information manually.

		//To configure an entity, simply get its configuration from the data store:
		FixedWidthEntityConfiguration foodGroupConfig = fixedWidthDataStore.getEntityConfiguration("food_group");

		//Set the names of each field in the entity food_group. These will become the headers in the output.
		foodGroupConfig.setHeaders("id", "name");

		//A fixed-width entity depends on the length of each field. This configures "id" to use 6 spaces, and "name" to use 35
		foodGroupConfig.setFieldLengths(6, 35);

		//Marks the "id" field as the identifier of records in food_group.
		foodGroupConfig.setIdentifiers("id");

		//Now for the "food" entity:
		FixedWidthEntityConfiguration foodConfig = fixedWidthDataStore.getEntityConfiguration("food");
		foodConfig.setHeaders("id", "group", "description", "scientific_name");
		foodConfig.setFieldLengths(8, 6, 40, 20);
		foodConfig.setIdentifiers("id");
		//##CODE_END
		return fixedWidthDataStore;
	}

	protected void initializeEngine(String engineName) {
		CsvDataStoreConfiguration csvDataStore = getCsvDataStore();
		FixedWidthDataStoreConfiguration fixedWidthDataStore = getFixedWidthDataStore();

		//##CODE_START
		//Creates a new engine configuration to map data between entities in CSV and fixed-width data stores
		EngineConfiguration engineConfig = new EngineConfiguration(engineName, csvDataStore, fixedWidthDataStore);

		//Registers this engine configuration.
		Univocity.registerEngine(engineConfig);
		//##CODE_END
	}
}
