/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples;

import java.util.*;

import org.testng.annotations.*;

import com.univocity.api.*;
import com.univocity.api.config.builders.*;
import com.univocity.api.data.*;
import com.univocity.api.engine.*;

public class Tutorial003SchemaMapping extends ExampleWithDatabase {

	private static final String engineName = "engine_003";

	/**
	 * In this example we want to migrate data across different schemas. The source data comes in 2 CSV files:
	 *
	 * <ul>
	 *  <li>FD_GROUP - groups of foods</li>
	 *  <li>FOOD_DES - descriptions of foods</li>
	 * </ul>
	 *
	 * The destination is a peculiar database that stores food information and their descriptions in multiple languages. It contains the following tables:
	 *
	 * <ul>
	 *  <li>LOCALE - the languages supported in the database.</li>
	 *  <li>FOOD_GROUP - groups of foods (without their descriptions)</li>
	 *  <li>FOOD_GROUP_DETAILS - descriptions and locale dependent information of each FOOD_GROUP</li>
	 *  <li>FOOD_NAME - names of foods (without locale dependent information)</li>
	 *  <li>FOOD_NAME_DETAILS - descriptions and locale dependent information of each FOOD_NAME</li>
	 *  <li>FOOD - food information: name, proteins, energy, etc.</li>
	 *  <li>GROUP_OF_FOOD - join table between foods and different groups.</li>
	 * </ul>
	 *
	 * The destination schema was made complex on purpose so we can explore how uniVocity can be used to handle virtually any schema with ease.
	 * We created an Entity-Relationship diagram in "resources/examples/new_schema/diagram.png" for your reference.
	 */
	@Test
	public void example001SchemaMapping() {
		// initializes an engine with a CSV data store with the files under /examples/source_data/csv/
		// and a JDBC data store with the tables created with the scripts under /examples/new_schema/
		initializeEngine(engineName);

		//##CODE_START
		DataIntegrationEngine engine = Univocity.getEngine(engineName);

		//Let's create a mapping between the CSV and JDBC data stores
		DataStoreMapping dsMapping = engine.map("csvDataStore", "newSchema");

		//Every entity mapping will be created with the following defaults for handling data
		dsMapping.configurePersistenceDefaults().usingMetadata().deleteDisabled().updateModified().insertNewRows();

		//Let's create two variables to represent the current locale information.
		engine.setVariable("locale", "en_US");
		engine.setVariable("localeDescription", "American English");

		EntityMapping mapping;

		//Here we create mapping from expressions to the destination entity "locale". No data in the source data store is required
		mapping = dsMapping.map("locale");
		//The value of the "locale" variable will be associated to the generated ID in the "locale" table.
		mapping.identity().associate("{$locale}").toGeneratedId("id");
		//The values of variables "locale" and "localeDescription" will be copied to "acronym" and "description" columns
		mapping.value().copy("{$locale}", "{$localeDescription}").to("acronym", "description");

		//This mapping just creates generated ID's in "food_group". Each generated ID will be associated to a value of "FdGrp_CD"
		mapping = dsMapping.map("FD_GROUP", "food_group");
		mapping.identity().associate("FdGrp_CD").toGeneratedId("id");

		//Next, we map the values in FD_GROUP to the locale-specific "food_group_details"
		mapping = dsMapping.map("FD_GROUP", "food_group_details");
		//Using the code in "FdGrp_CD" and the current value of the "locale" variable, we define an identifier association to "id" and "loc", respectively.
		//The values in "id" and "loc" are references to the identifiers mapped above.
		mapping.identity().associate("FdGrp_CD", "{$locale}").to("id", "loc");
		//We use the "FdGrp_CD" to obtain the identifiers generated in a mapping from "FD_GROUP" to "food_group".
		//The identifier written to "food_group" will be copied to "id".
		mapping.reference().using("FdGrp_CD").referTo("FD_GROUP", "food_group").on("id");
		//Here we use the current value in the "locale" variable to restore the identifier generated in the mapping to "locale".
		//This identifier will be copied to "loc"
		mapping.reference().using("{$locale}").referTo(null, "locale").on("loc");
		//Just copy the food group description
		mapping.value().copy("FdGrp_Desc").to("description");

		//Again, we just associate values of "FOOD_DES" to a generated ID in the destination "food_name".
		mapping = dsMapping.map("FOOD_DES", "food_name");
		mapping.identity().associate("NDB_No").toGeneratedId("id");

		//Similarly to the mapping from "FD_GROUP" to "food_group_details", we just map identity fields and references
		mapping = dsMapping.map("FOOD_DES", "food_name_details");
		mapping.identity().associate("NDB_No", "{$locale}").to("id", "loc");
		mapping.reference().using("NDB_No").referTo("FOOD_DES", "food_name").on("id");
		mapping.reference().using("{$locale}").referTo(null, "locale").on("loc");
		mapping.value().copy("Long_Desc").to("description");

		//Here the basic food composition data in "FOOD_DES" is mapped to the "food" entity.
		mapping = dsMapping.map("FOOD_DES", "food");
		mapping.identity().associate("NDB_No").toGeneratedId("id");
		// "food.name_id" is a reference to "food_name". We use "NDB_No" to obtain the ID generated in the mapping from "FOOD_DES" to "food_name"
		mapping.reference().using("NDB_No").referTo("FOOD_DES", "food_name").on("name_id");
		// let's finally copy the food composition data
		mapping.value().copy("CHO_Factor", "Fat_Factor", "Pro_Factor", "N_Factor").to("carbohydrate_factor", "fat_factor", "protein_factor", "nitrogen_protein_factor");

		//Lastly, we associate each "food" to a "food_group". Once again, this is easily achieved with the use of reference mappings.
		mapping = dsMapping.map("FOOD_DES", "group_of_food");
		mapping.identity().associate("NDB_No", "FdGrp_CD").to("food_id", "group_id");
		mapping.reference().using("NDB_No").referTo("FOOD_DES", "food").on("food_id");
		mapping.reference().using("FdGrp_CD").referTo("FD_GROUP", "food_group").on("group_id");

		//In this cycle, the current locale is "en_US".
		engine.executeCycle();

		//Let's now use another locale. All tables with locale-specific data will have new descriptions associated to this new locale.
		engine.setVariable("locale", "en_GB");
		engine.setVariable("localeDescription", "British English");
		engine.executeCycle();

		//Finally, let's print all tables and see how the data looks like:
		printAndValidate(printTables());

		//##CODE_END
	}

	private String printTables() {
		StringBuilder out = new StringBuilder();
		out.append(printLocaleTable());
		out.append(printFoodGroupTables());
		out.append(printFoodNameTables());
		out.append(printFoodTable());
		out.append(printGroupOfFoodTable());
		return out.toString();
	}

	@Test(dependsOnMethods = "example001SchemaMapping")
	public void example002UpdateAgainstDataset() {
		//##CODE_START

		//Data increments can be used to update certain records without re-reading the entire source entity.
		//To define a data increment, you must first create a dataset with the updated data.
		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "0100", "Milk, eggs and stuff" }); //update to FD_GROUP with FdGrp_CD = "0100"
		rows.add(new Object[] { "9999", "Some new group" }); // new row to insert into FD_GROUP with FdGrp_CD = "9999"
		rows.add(new Object[] { "1500", "Bird meat" });  //update to FD_GROUP with FdGrp_CD = "1500"

		//For convenience, uniVocity provides a dataset factory to create common instances of Dataset
		DatasetFactory factory = Univocity.datasetFactory();

		//Let's create a dataset with the list rows created above:
		final Dataset changesToFoodGroup = factory.newDataset(rows, "FdGrp_CD", "FdGrp_CD", "FdGrp_Desc");

		//With a dataset, we can create a data increment. A single data increment can provide datasets for multiple
		//data stores and data entities.
		DataIncrement increment = new DataIncrement() {
			@Override
			public Dataset getDataset(String datastoreName, String entityName) {
				if (datastoreName.equalsIgnoreCase("csvDataStore") && entityName.equalsIgnoreCase("FD_GROUP")) {
					return changesToFoodGroup;
				}
				//If no data set is provided then an empty dataset will be used in place of the original entity.
				//In this example, mappings where data is mapped from "FOOD_DES" won't receive any new data
				return null;
			}
		};

		DataIntegrationEngine engine = Univocity.getEngine(engineName);

		//These changes should be applied only to the "en_GB" locale.
		engine.setVariable("locale", "en_GB");
		engine.setVariable("localeDescription", "British English");

		//Let's execute a mapping cycle using this data increment and see the results.
		engine.executeCycle(increment);

		//##CODE_END
		printAndValidate(printFoodGroupTables());
	}

	@Test(dependsOnMethods = "example002UpdateAgainstDataset")
	public void example003UpdatePrevention() {
		DataIntegrationEngine engine = Univocity.getEngine(engineName);
		StringBuilder output = new StringBuilder();

		//##CODE_START
		DataStoreMapping dsMapping = engine.getMapping("csvDataStore", "newSchema");
		EntityMapping mapping = dsMapping.getMapping("FD_GROUP", "food_group_details");
		mapping.persistence().usingMetadata().deleteDisabled().updateModified().insertNewRows();

		//To prevent data updates on some rows in the destination entity, you must give uniVocity a
		//dataset with the identifiers of records that should not be modified
		List<Object[]> rowsToKeep = new ArrayList<Object[]>();
		rowsToKeep.add(new Object[] { 0, 1 });  //we want to keep the row with description "Milk, eggs and stuff".
		//0, 1 is the identifier of such row, as you can see in the output produced previously

		//Here we create a dataset with the row identifiers that indicate what records must be preserved
		Dataset dataset = Univocity.datasetFactory().newDataset(rowsToKeep, new String[] { "id", "loc" });

		//This will disable updates on "food_group_details", where id = 0 and loc = 1.
		engine.disableUpdateOnRecords("food_group_details", dataset);

		//Let's execute a data mapping cycle now.
		engine.executeCycle();

		//row with "Milk, eggs and stuff" is still there
		//row with "bird meat" must have been updated to "Poultry Products"
		println(output, " -- After disabling updates --");
		println(output, printFoodGroupDetailsTable());

		//Let's enable updates for all records now and execute another mapping cycle
		engine.enableUpdateOnAllRecords("food_group_details");
		engine.executeCycle();

		//row with "Milk, eggs and stuff" must have been updated to "Dairy and Egg Products"
		println(output, " -- After re-enabling updates --");
		println(output, printFoodGroupDetailsTable());

		//##CODE_END
		printAndValidate(output);
	}

	public static void main(String... args) {
		Tutorial003SchemaMapping tutorial = new Tutorial003SchemaMapping();

		try {
			tutorial.example001SchemaMapping();
			tutorial.example002UpdateAgainstDataset();
			tutorial.example003UpdatePrevention();
		} finally {
			Univocity.shutdown(engineName);
		}
		System.exit(0);
	}
}
