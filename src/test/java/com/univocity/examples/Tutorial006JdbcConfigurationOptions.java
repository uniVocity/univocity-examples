/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples;

import java.util.*;

import org.springframework.jdbc.core.*;
import org.testng.annotations.*;

import com.univocity.api.*;
import com.univocity.api.config.builders.*;
import com.univocity.api.engine.*;
import com.univocity.api.entity.jdbc.*;

public class Tutorial006JdbcConfigurationOptions extends ExampleWithDatabase {

	@Override
	protected JdbcDataStoreConfiguration getNewSchemaDataStore() {
		JdbcDataStoreConfiguration database = super.getNewSchemaDataStore();

		//##CODE_START
		//Let's add new columns to store uniVocity's data to track generated IDs
		new JdbcTemplate(dataSource).update("ALTER TABLE locale ADD id_tracker INTEGER");
		new JdbcTemplate(dataSource).update("ALTER TABLE locale ADD process_id INTEGER");

		//The "locale" table also works with logical exclusion.
		new JdbcTemplate(dataSource).update("ALTER TABLE locale ADD deleted CHAR(1) DEFAULT 'N' NOT NULL");

		JdbcEntityConfiguration config = database.getEntityConfiguration("locale");
		//Here we configure the locale entity to use 2 numeric columns to allow batch inserts
		//After each batch, uniVocity will fetch the ID's generated for each row using the information stored in these columns.
		config.retrieveGeneratedKeysUsingNumericColumns("process_id", "id_tracker", "id");

		//We also define a custom SQL producer to generate proper SELECT statements that
		//take into account the logical exclusion implemented for the locale table..
		config.setSqlProducer(new LogicalExclusionSelect());

		//##CODE_END
		return database;
	}

	@Override
	public String printLocaleTable() {
		return printTable("locale", "id", "acronym", "description", "deleted");
	}

	@Test
	public void example001BatchInsertAndGeneratedKeyRetrieval() {
		initializeEngine("batch");

		DataIntegrationEngine engine = Univocity.getEngine("batch");
		//##CODE_START
		//Let's create a dataset with some locales for the English language.
		Map<String, String> locales = new TreeMap<String, String>();
		locales.put("en_AU", "English (Australia)");
		locales.put("en_CA", "English (Canada)");
		locales.put("en_GB", "English (United Kingdom)");
		locales.put("en_IE", "English (Ireland)");
		locales.put("en_IN", "English (India)");
		locales.put("en_MT", "English (Malta)");
		locales.put("en_NZ", "English (New Zealand)");
		locales.put("en_PH", "English (Philippines)");
		locales.put("en_SG", "English (Singapore)");
		locales.put("en_US", "English (United States)");
		locales.put("en_ZA", "English (South Africa)");

		engine.addDataset("localesDataset", Univocity.datasetFactory().newDataset(locales, "acronym", "description"));

		DataStoreMapping dsMapping = engine.map("newSchema", "newSchema");
		//Now we just create a mapping from our dataset to the locale table
		EntityMapping mapping = dsMapping.map("localesDataset", "locale");
		//Here we tell uniVocity to handle generated ID's as usual
		mapping.identity().associate("acronym").toGeneratedId("id");
		mapping.value().copy("acronym", "description").to("acronym", "description");

		//After executing this cycle, the locale table will have some data
		engine.executeCycle();

		//This will perform a logical exclusion here to "delete" some locales.
		new JdbcTemplate(dataSource).update("UPDATE locale SET deleted = 'Y' WHERE acronym in ('en_US', 'en_AU', 'en_GB')");

		//Now, let's map data from the locale table to a new dataset.
		//It is just a map from the locale acronym to its generated ID in the locale table.
		Map<String, String> generatedLocaleIds = new TreeMap<String, String>();
		engine.addDataset("generatedLocaleIds", Univocity.datasetFactory().newDataset(generatedLocaleIds, "acronym", "generated_id"));

		mapping = dsMapping.map("locale", "generatedLocaleIds");
		mapping.identity().associate("id").to("generated_id");
		mapping.value().copy("acronym").to("acronym");

		//After executing another data mapping cycle, the "generatedLocaleIds" map should contain
		//all locales that were not logically deleted
		engine.executeCycle();
		//##CODE_END

		printAndValidate(printLocaleTable() + "\nMap to locale IDs:\n" + generatedLocaleIds);
		Univocity.shutdown("batch");
	}

	public static void main(String... args) {
		Tutorial006JdbcConfigurationOptions tutorial = new Tutorial006JdbcConfigurationOptions();

		try {
			tutorial.example001BatchInsertAndGeneratedKeyRetrieval();
		} finally {
			Univocity.shutdown("batch");
		}

		System.exit(0);
	}
}
