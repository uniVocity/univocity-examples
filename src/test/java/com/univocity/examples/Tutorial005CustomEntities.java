/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples;

import static org.testng.Assert.*;

import java.util.*;
import java.util.Map.Entry;

import org.testng.annotations.*;

import com.univocity.api.*;
import com.univocity.api.config.*;
import com.univocity.api.config.builders.*;
import com.univocity.api.data.*;
import com.univocity.api.engine.*;
import com.univocity.examples.custom.*;

public class Tutorial005CustomEntities extends Example {

	private static final String ENGINE = "engine_005";

	private Map<Object, Object[]> groupValues;
	private Map<Object, Object[]> queryValues;
	private Map<Object, Object[]> foodValues;

	private MyDataStoreConfiguration getMyDataStore() {
		//Let's create and configure our custom data store configuration class
		MyDataStoreConfiguration customDataStore = new MyDataStoreConfiguration("MyDataStore");

		//These entities will be used to store data mapped from CSV files
		customDataStore.addEntity("MyFoodGroupEntity", "description");
		customDataStore.addEntity("MyFoodEntity", "group", "description");

		//This data store supports querying through its own particular syntax
		customDataStore.addQuery("queryCodeOf", " MyFoodGroupEntity (row_id, description) with (description = ?) ");

		return customDataStore;
	}

	private Map<Object, Object[]> addMapDataSet(String datasetName, String keyField, String... valueFields) {
		//simply creates a HashMap to be used as a dataset to store values mapped from our custom entity.
		//we will print the values added to this map at the end of the example.
		Map<Object, Object[]> map = new HashMap<Object, Object[]>();

		Dataset dataset = Univocity.datasetFactory().newDataset(map, keyField, valueFields);

		DataIntegrationEngine engine = Univocity.getEngine(ENGINE);
		engine.addDataset(datasetName, dataset);

		return map;
	}

	private void printMap(StringBuilder out, Map<Object, Object[]> map) {
		if (map.isEmpty()) {
			println(out, "<empty>");
			return;
		}
		for (Entry<Object, Object[]> e : map.entrySet()) {
			out.append(e.getKey());
			out.append(" => ");
			out.append(Arrays.toString(e.getValue()));
			println(out);
		}
	}

	private String printFoodGroupDataSets() {
		StringBuilder out = new StringBuilder();

		println(out, "Food groups:");
		printMap(out, groupValues);

		println(out);
		println(out, "Query for dairy group:");
		printMap(out, queryValues);

		return out.toString();
	}

	private String printAllDatasets() {
		StringBuilder out = new StringBuilder();

		out.append(printFoodGroupDataSets());

		println(out);
		println(out, "Foods :");
		printMap(out, foodValues);
		return out.toString();
	}

	private void initialize() {
		//Let's map data from CSVs to the custom entities
		EngineConfiguration engineConfig = new EngineConfiguration(ENGINE, getCsvDataStore(), getMyDataStore());
		//The engine needs to know how obtain new instances of our custom data store. We need to provide a data store factory
		//that knows how to build a data store from our custom configuration class.
		engineConfig.addCustomDataStoreFactories(new MyDataStoreFactory());

		Univocity.registerEngine(engineConfig);

		//Here we create datasets that will receive values mapped from the custom entities
		//The field names in these datasets match the field names in our custom entities in order to use automatic detection of field mappings.
		groupValues = addMapDataSet("GroupDataSet", "id", "id", "description");
		queryValues = addMapDataSet("GroupQueryDataSet", "id", "id", "description");
		foodValues = addMapDataSet("FoodDataset", "id", "id", "group", "description");
	}

	@Test
	public void example001MappingToAndFromCustomEntity() {
		//##CODE_START
		//Initializes the engine with the CSV and custom data stores.
		initialize();
		DataIntegrationEngine engine = Univocity.getEngine(ENGINE);

		//Creates a mapping from the CSV data store to the custom data store
		DataStoreMapping dsMapping = engine.map("csvDataStore", "MyDataStore");
		dsMapping.configurePersistenceDefaults().usingMetadata().deleteAbsent().updateModified().insertNewRows();

		EntityMapping mapping;
		//Maps the "FD_GROUP" CSV file to our custom entity "MyFoodGroupEntity"
		mapping = dsMapping.map("FD_GROUP", "MyFoodGroupEntity");
		//The custom entities we created generate identifiers upon insertion of new records.
		mapping.identity().associate("FdGrp_CD").toGeneratedId("row_id");
		mapping.value().copy("FdGrp_Desc").to("description");

		//Maps the "FOOD_DES" CSV file to our custom entity "MyFoodEntity"
		mapping = dsMapping.map("FOOD_DES", "MyFoodEntity");
		mapping.identity().associate("ndb_no").toGeneratedId("row_id");
		//We also create a reference mapping here. This is managed by uniVocity and the custom entity does not need to care about it.
		mapping.reference().using("FdGrp_CD").referTo("FD_GROUP", "MyFoodGroupEntity").on("group");
		mapping.value().copy("Long_Desc").to("description");

		//Here we map the data in our custom entities to the datasets created on initialization.
		dsMapping = engine.map("MyDataStore", "<datasets>");
		dsMapping.configurePersistenceDefaults().notUsingMetadata().deleteAll().insertNewRows();

		mapping = dsMapping.map("MyFoodGroupEntity", "GroupDataSet");
		mapping.identity().associate("row_id").to("id");
		mapping.autodetectMappings();

		//Here we invoke our custom query implementation and store the results in "GroupQueryDataSet"
		mapping = dsMapping.map("{queryCodeOf('Dairy and Egg Products')}", "GroupQueryDataSet");
		mapping.identity().associate("row_id").to("id");
		mapping.autodetectMappings();

		mapping = dsMapping.map("MyFoodEntity", "FoodDataset");
		mapping.identity().associate("row_id").to("id");
		mapping.autodetectMappings();

		//Let's execute a mapping cycle and check the output.
		engine.executeCycle();
		//##CODE_END
		printAndValidate(printAllDatasets());
	}

	@Test(dependsOnMethods = "example001MappingToAndFromCustomEntity")
	public void example002TestTransaction() {
		DataIntegrationEngine engine = Univocity.getEngine(ENGINE);

		//##CODE_START
		DataIncrement increment = new DataIncrement() {
			@Override
			public Dataset getDataset(String datastoreName, String entityName) {
				//Returns our data set. It will work as expected and the destination "MyFoodGroupEntity" will store this new data
				if (datastoreName.equalsIgnoreCase("csvDataStore") && entityName.equalsIgnoreCase("FD_GROUP")) {
					//Our custom data store is transactional. Let's try to insert some data and make the mapping process crash to see what happens
					List<Object[]> rows = new ArrayList<Object[]>();
					rows.add(new Object[] { "1111", "I won't be inserted" });
					rows.add(new Object[] { "9999", "I won't be updated" });

					//Creates a regular dataset for food group
					return Univocity.datasetFactory().newDataset(rows, "FdGrp_CD", "FdGrp_CD", "FdGrp_Desc");
				}
				//Returns a dataset that will "explode" as soon as uniVocity tries to read its rows.
				if (datastoreName.equalsIgnoreCase("csvDataStore") && entityName.equalsIgnoreCase("FOOD_DES")) {
					return new Dataset() {
						@Override
						public String[] getFieldNames() {
							return new String[] { "ndb_no", "FdGrp_CD", "Long_Desc" };
						}

						@Override
						public String[] getIdentifiers() {
							return new String[] { "ndb_no" };
						}

						@Override
						public Iterable<Object[]> getRows() {
							//blows everything up to trigger a rollback in our custom data store.
							throw new IllegalStateException("Crash and burn!");
						}

						@Override
						public int size() {
							return -1;
						}
					};
				}
				return null;
			}
		};

		//Let's execute a cycle using the data increment created above.
		//If you check the logs you should see that the mapping to "MyFoodGroupEntity" completes successfully.
		try {
			engine.executeCycle(increment, "MyFoodGroupEntity", "MyFoodEntity");
			fail("Expected exception while executing this cycle.");
		} catch (Throwable ex) {
			//any changed made in the entities of our custom data store must have been rolled back due to this exception.
			ex.printStackTrace();
		}

		//This cycle executes only the mappings where the destination entities are the datasets "GroupDataSet" and "GroupQueryDataSet"
		engine.executeCycle("GroupDataSet", "GroupQueryDataSet");
		//Let's see what data got mapped after the transaction rollback.
		printAndValidate(printAllDatasets());
		//##CODE_END
	}

	@Test(dependsOnMethods = "example002TestTransaction")
	public void example003TestDataUpdates() {
		DataIntegrationEngine engine = Univocity.getEngine(ENGINE);

		//##CODE_START
		//Updates and deletions were also implemented in our custom entities, let's test it using a data increment
		DataIncrement increment = new DataIncrement() {
			@Override
			public Dataset getDataset(String datastoreName, String entityName) {
				if (datastoreName.equalsIgnoreCase("csvDataStore") && entityName.equalsIgnoreCase("FD_GROUP")) {
					List<Object[]> rows = new ArrayList<Object[]>();
					rows.add(new Object[] { "0100", "Milk, eggs and stuff" }); //update to FD_GROUP with FdGrp_CD = "0100"
					rows.add(new Object[] { "9999", "Some new group" }); // new row to insert into FD_GROUP with FdGrp_CD = "9999"
					rows.add(new Object[] { "1500", "Bird meat" });  //update to FD_GROUP with FdGrp_CD = "1500"

					return Univocity.datasetFactory().newDataset(rows, "FdGrp_CD", "FdGrp_CD", "FdGrp_Desc");

				}
				return null;
			}
		};

		//Executes a cycle to update the "MyFoodGroupEntity" only, using the data increment created above.
		engine.executeCycle(increment, "MyFoodGroupEntity");

		//This cycle executes only the mappings where the destination entities are the datasets "GroupDataSet" and "GroupQueryDataSet"
		engine.executeCycle("GroupDataSet", "GroupQueryDataSet");
		//##CODE_END

		printAndValidate(printFoodGroupDataSets());
	}

	public static void main(String... args) {
		Tutorial005CustomEntities tutorial = new Tutorial005CustomEntities();

		try {
			tutorial.example001MappingToAndFromCustomEntity();
			tutorial.example002TestTransaction();
			tutorial.example003TestDataUpdates();
		} finally {
			Univocity.shutdown(ENGINE);
		}

		System.exit(0);
	}
}
