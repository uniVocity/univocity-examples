/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples;

import java.io.*;
import java.util.*;

import org.springframework.jdbc.core.*;
import org.testng.annotations.*;

import com.univocity.api.*;
import com.univocity.api.config.*;
import com.univocity.api.config.builders.*;
import com.univocity.api.engine.*;

public class Tutorial004Advanced extends ExampleWithDatabase {

	@Test
	public void example001DataSetProducer() {
		initializeEngine("Producer");
		DataIntegrationEngine engine = Univocity.getEngine("Producer");

		//##CODE_START
		//To keep it simple, we will just insert a locale to the database directly and use its ID in our mappings.
		new JdbcTemplate(dataSource).update("INSERT INTO locale (acronym, description) VALUES ('en_US', 'American English')");
		int localeId = new JdbcTemplate(dataSource).queryForObject("SELECT id FROM locale WHERE acronym = 'en_US'", Integer.class);

		//Let's define the ID of the inserted locale as a constant which is accessible from anywhere.
		engine.setConstant("locale", localeId);

		//Here we add our dataset producer to the "FOOD_DES" entity and tell uniVocity to extract the columns used to generate the datasets.
		engine.addDatasetProducer(EngineScope.CYCLE, new FoodProcessor()).on("FOOD_DES", "Ndb_no", "Long_Desc");

		DataStoreMapping dsMapping = engine.map("csvDataStore", "newSchema");
		EntityMapping mapping;

		//The first mapping uses the "food_names" dataset that is produced with our FoodProcessor.
		mapping = dsMapping.map("food_names", "food_name");
		mapping.identity().associate("name").toGeneratedId("id");

		mapping = dsMapping.map("food_names", "newSchema.food_name_details");
		//Here we read the locale constant
		mapping.identity().associate("name", "{$locale}").to("id", "loc");
		mapping.reference().using("name").referTo("food_names", "food_name").on("id").directly().onMismatch().abort();
		mapping.value().copy("name").to("description");

		//"food_name_details" exist in multiple data stores. In this case uniVocity cannot resolve what source entity to use,
		//it is necessary to prepend the data store name to resolve the ambiguity.
		//Datasets are part of a special data store named "<datasets>".
		mapping = dsMapping.map("<datasets>.food_name_details", "food");
		mapping.identity().associate("food_code").toGeneratedId("id");
		mapping.reference().using("name").referTo("food_names", "food_name").on("name_id").directly().onMismatch().abort();

		mapping = dsMapping.map("food_state_names", "food_state");
		mapping.identity().associate("name").toGeneratedId("id");

		mapping = dsMapping.map("food_state_names", "newSchema.food_state_details");
		mapping.identity().associate("name", "{$locale}").to("id", "loc");
		mapping.reference().using("name").referTo("food_state_names", "food_state").on("id").directly().onMismatch().abort();
		mapping.value().copy("name").to("description");

		mapping = dsMapping.map("<datasets>.food_state_details", "state_of_food");
		mapping.identity().associate("name", "food_code").to("state_id", "food_id");
		mapping.reference().using("name").referTo("food_state_names", "food_state").on("state_id").directly().onMismatch().abort();
		mapping.reference().using("food_code").referTo("<datasets>.food_name_details", "food").on("food_id").directly().onMismatch().abort();
		mapping.value().copy("order").to("sequence");

		//After mapping food names and food states, we still need to set the food properties.
		//This mapping will use the original source entity "FOOD_DES"
		mapping = dsMapping.map("FOOD_DES", "food");
		mapping.identity().associate("NDB_No").to("id");
		mapping.reference().using("NDB_No").referTo("<datasets>.food_name_details", "food").on("id").directly().onMismatch().abort();
		mapping.value().copy("CHO_Factor", "Fat_Factor", "Pro_Factor", "N_Factor").to("carbohydrate_factor", "fat_factor", "protein_factor", "nitrogen_protein_factor");
		//The mapping defined above creates rows for "food", but they are updates to the records mapped from "<datasets>.food_name_details" to "food".
		//To avoid inserting these rows as new records, we use the "updateNewRows" insert option.
		mapping.persistence().usingMetadata().deleteDisabled().updateDisabled().updateNewRows();

		//Let's execute the mapping cycle and see the results
		engine.executeCycle();
		//##CODE_END

		StringBuilder output = new StringBuilder();
		println(output, printFoodNameTables());
		println(output, printFoodTable());
		println(output, printFoodStateTables());
		println(output, printStateOfFoodTable());
		//As the data is distributed across too many tables, it may be a bit hard to reason about how things are associated.
		//We thought it would be easier for you to see it coming from a query that produces an output similar to the input data:
		println(output, queryTablesAndPrintMigratedFoodData());
		printAndValidate(output);
		Univocity.shutdown("Producer");
	}

	private String queryTablesAndPrintMigratedFoodData() {
		String query = ""
				+ "---[ Query to reconstruct the information we've just mapped from the input ]---"
				+ "\n SELECT n.description AS name, s1.description AS food_state_1,  s2.description AS food_state_2, f.carbohydrate_factor AS carbs,"
				+ "\n\t  f.fat_factor AS fat, f.protein_factor AS proteins, f.nitrogen_protein_factor AS nitrogen "
				+ "\n FROM food f "
				+ "\n JOIN food_name_details n ON n.id = f.name_id "
				+ "\n JOIN state_of_food j1 ON j1.food_id = f.id AND j1.sequence = 1 "
				+ "\n JOIN food_state_details s1 ON s1.id = j1.state_id "
				+ "\n LEFT JOIN state_of_food j2 ON j2.food_id = f.id AND j2.sequence = 2 "
				+ "\n LEFT JOIN food_state_details s2 ON s2.id = j2.state_id "
				+ "\n ORDER BY name, food_state_1, food_state_2"
				+ "\n";
		List<Map<String, Object>> result = new JdbcTemplate(dataSource).queryForList(query);

		String out = printRows(result, "Query result", "name", "food_state_1", "food_state_2", "nitrogen", "proteins", "fat", "carbs");
		return query + "\n" + out;
	}

	@Test(dependsOnMethods = "example001DataSetProducer")
	public void example002PersistentScope() {
		final StringBuilder out = new StringBuilder();
		//##CODE_START
		EngineConfiguration config = new EngineConfiguration("Persistent", getCsvDataStore(), getFixedWidthDataStore());

		//The persistent scope retains values persisted in variables other elements that should be made available
		//after the engine is stopped and subsequently started. It depends on a storage provider defined by the user.
		//The storage can be a file, database, distributed cache or anything else that will outlive the engine.
		config.setPersistentStorageProvider(new ScopeStorageProvider() {

			//We will keep the state in a byte array.
			private byte[] persistentState;

			//This is where our data will be kept. It will be serialized to the byte array
			//When the persisted scope is deactivated.
			private Map<Object, Object> persistentMap;

			@Override
			public Object setValue(Object key, Object value) {
				return persistentMap.put(key, value);
			}

			@SuppressWarnings("unchecked")
			@Override
			public void initialize() {
				//Here we restore the content stored in our "persistent" byte array.
				if (persistentState == null) {
					//No byte array yet, let's just create an empty map
					println(out, "Creating new persistent state");
					persistentMap = new HashMap<Object, Object>();
				} else {
					// Deserialize map from the byte array
					try {
						ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(persistentState));
						persistentMap = ((Map<Object, Object>) in.readObject());
						println(out, "Restored previous persistent state: " + persistentMap);
					} catch (Exception ex) {
						throw new IllegalStateException("Unable to restore state of persistent scope", ex);
					}
				}
			}

			@Override
			public Object getValue(Object key) {
				return persistentMap.get(key);
			}

			@Override
			public void deactivate() {
				//This method is called when the persistent scope is deactivated (i.e. the engine is being stopped)
				try {
					println(out, "Persistent scope deactivated. Saving its state for later recovery.");
					ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

					ObjectOutputStream out = new ObjectOutputStream(byteOutput);
					//let's serialize our map
					out.writeObject(persistentMap);
					out.flush();
					out.close();

					//update the byte array. In a "real" application that would be a file, database,
					//distributed cache or anything you need to use to persist these values.
					persistentState = byteOutput.toByteArray();
				} catch (Exception ex) {
					throw new IllegalStateException("Unable to save data in persistent scope", ex);
				}
			}

			@Override
			public boolean contains(Object key) {
				return persistentMap.containsKey(key);
			}
		});

		//Here we register the engine our configuration using a persistent scope.
		Univocity.registerEngine(config);

		//Let's create a mapping to move some data around
		DataIntegrationEngine engine = Univocity.getEngine("Persistent");
		DataStoreMapping mapping = engine.map("csvDataStore", "fixedWidthDestination");

		EntityMapping foodGroupMapping = mapping.map("FD_GROUP", "food_group");
		foodGroupMapping.identity().associate("FdGrp_CD").to("id");
		foodGroupMapping.value().copy("FdGrp_Desc").to("name");

		//Here we define the persistent variable "processedCodes". It will retain values read from "FdGrp_CD"
		//into a set in the persistent scope.
		engine.setPersistentVariable("processedCodes", new TreeSet<String>());

		//This row reader will discard input rows where the value of "FdGrp_CD" has been already mapped.
		//It uses the "processedCodes" variable to ensure each row is mapped only once.
		foodGroupMapping.addInputRowReader(new RowReader() {
			@SuppressWarnings("unchecked")
			@Override
			public void processRow(Object[] inputRow, Object[] outputRow, RowMappingContext context) {
				//gets the value of FdGrp_CD in the input
				String code = (String) inputRow[context.getInputIndex("FdGrp_CD")];

				//gets the set of processed codes stored in the persistent variable "processedCodes"
				Set<String> processedCodes = (Set<String>) context.readVariable("processedCodes");

				//verifies whether a row with this code has been mapped already
				if (processedCodes.contains(code)) {
					//discards row if it has been mapped
					context.discardRow();
				} else {
					//this is a new row. Let's store it's code so it does not get mapped again.
					processedCodes.add(code);
					//Let's also increment the "rowCount" variable to inform us how many new rows were mapped in this cycle.
					Integer rowCount = (Integer) context.readVariable("rowCount");
					context.setVariable("rowCount", rowCount + 1);
				}
			}
		});

		//executes a cycle and returns the number of new rows mapped in this cycle
		println(out, "Row count after first cycle: " + executeAndReturnRowCount());
		println(out, "Shutting down...");
		//shuts down the engine so we can see the persistence provider's messages while stopping and restarting
		Univocity.shutdown("Persistent");
		println(out, "...Starting engine again");
		//starts the engine and executes another cycle, then returns the number of new rows mapped
		println(out, "Row count after shutting down and executing again: " + executeAndReturnRowCount());
		//##CODE_END
		printAndValidate(out);

		Univocity.shutdown("Persistent");
	}

	private int executeAndReturnRowCount() {
		DataIntegrationEngine engine = Univocity.getEngine("Persistent");

		//sets the "rowCount" to 0. It will be incremented in the input row reader defined previously.
		engine.setVariable("rowCount", 0);
		engine.executeCycle();
		return (Integer) engine.readVariable("rowCount");
	}

	public static void main(String... args) {
		Tutorial004Advanced tutorial = new Tutorial004Advanced();

		try {
			tutorial.example001DataSetProducer();
		} finally {
			Univocity.shutdown("Producer");
		}

		try {
			tutorial.example002PersistentScope();
		} finally {
			Univocity.shutdown("Persistent");
		}

		System.exit(0);
	}
}
