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
import com.univocity.api.engine.*;
import com.univocity.api.entity.text.csv.*;

public class Tutorial002QueriesFunctionsAndVariables extends Example {

	private static final String engineName = "engine_002";

	@Override
	protected CsvDataStoreConfiguration getCsvDataStore() {
		CsvDataStoreConfiguration csvDataStore = super.getCsvDataStore();

		//Tells uniVocity to load these entities into in-memory database tables. Each column will be created using type VARCHAR with a fixed length.
		//This enables operations such as updates and deletions in CSV files, and even creating SQL queries.
		csvDataStore.enableDatabaseOperationsIn("FD_GROUP", "FOOD_DES");

		//In order to enable database operations in a text-based data entity, some information is required, namely: 
		//the length of each field and which ones can be used in primary key of the in-memory table  

		//FdGrp_Cd will be the primary key of the in-memory table.  
		csvDataStore.getEntityConfiguration("FD_GROUP").setIdentifiers("FdGrp_Cd");
		//As the FD_GROUP.csv file contains only two fields, we can simply give their lengths (the names will extracted from the first row in file)
		csvDataStore.getEntityConfiguration("FD_GROUP").setFieldLengths(4, 40);

		//"NDB_No" will be the primary key of the FOOD_DES in-memory table
		csvDataStore.getEntityConfiguration("FOOD_DES").setIdentifiers("NDB_No");

		//As FOOD_DES has lots of fields, it will be more convenient and clear to use a map 
		LinkedHashMap<String, Integer> foodFields = new LinkedHashMap<String, Integer>();
		foodFields.put("NDB_No", 5);
		foodFields.put("FdGrp_Cd", 4);
		foodFields.put("Long_Desc", 80);
		foodFields.put("Shrt_Desc", 50);
		foodFields.put("ComName", 50);
		foodFields.put("ManufacName", 50);
		foodFields.put("Survey", 1);
		foodFields.put("Ref_Desc", 50);
		foodFields.put("Refuse", 5);
		foodFields.put("SciName", 50);
		foodFields.put("N_Factor", 6);
		foodFields.put("Pro_Factor", 6);
		foodFields.put("Fat_Factor", 6);
		foodFields.put("CHO_Factor", 6);

		//Finally, let's set the fields with their lengths.
		csvDataStore.getEntityConfiguration("FOOD_DES").setFieldsAndLengths(foodFields);

		return csvDataStore;
	}

	@Test
	public void example001QueryMapping() {
		initializeEngine(engineName);
		DataIntegrationEngine engine = Univocity.getEngine(engineName);

		//##CODE_START
		//As database-like operations are enabled for FD_GROUP and FOOD_DES, we can create a SQL query with them
		engine.addQuery(EngineScope.CYCLE, "groupsInUse") //scope and name of the query. CYCLE scope will make the engine reuse results within a cycle
				.onDataStore("csvDataStore") // data store name where the query will be executed.
				.fromString(" " +
						" SELECT fdgrp_cd AS id, fdgrp_desc AS name " +	// *NOTE* we are giving labels to the column names here.
						" FROM fd_group g " +
						" WHERE fdgrp_cd IN ( SELECT DISTINCT fdgrp_Cd FROM food_des ) " +
						" ORDER BY fdgrp_desc ASC "
				).returnDataset(); // this query returns a dataset that can be used as the source entity in entity mappings.

		//Let's create a mapping between the CSV and fixed-width data stores.
		DataStoreMapping mapping = engine.map("csvDataStore", "fixedWidthDestination");

		//Here we map the result of the query to the fixed-width entity "food_group"
		//Note the query name is declared within curly braces.
		EntityMapping queryMapping = mapping.map("{groupsInUse}", "food_group");

		//As we labeled the columns in the query to be the same as in the destination entity,
		//we can just use auto-detection and all fields with similar names will be associated, including identifiers.
		queryMapping.autodetectMappings();

		//Let's execute and  check the output
		engine.executeCycle();
		//##CODE_END

		String writtenData = foodGroupOutput.getString();
		printAndValidate(writtenData);
	}

	@Test(dependsOnMethods = "example001QueryMapping")
	public void example002QueryWithParameters() {
		//We will print the data in the destination entities after each cycle to this string
		StringBuilder output = new StringBuilder();

		//In this example we want to use parameterized queries as source entities. 
		//Different parameters will be used in each mapping cycle. 
		DataIntegrationEngine engine = Univocity.getEngine(engineName);

		//##CODE_START
		//This query uses the description of a food group to find and return its code.
		//Note that queries that do not return a dataset are simply functions that can be used everywhere.
		//Parameters: groupDescription
		engine.addQuery(EngineScope.MAPPING, "findGroupCode")
				.onDataStore("csvDataStore")
				.fromString("SELECT fdgrp_cd FROM fd_group WHERE fdgrp_desc like :groupDescription")
				.returnSingleValue() //expects one single result.
				.directly() // just returns the result without applying any function to it.
				.onErrorAbort(); //if the query does not return any value, or returns more than one value, abort the data mapping cycle. 

		//Locates all foods that are part of a given group and that have a given description.
		//In SQL queries used by uniVocity's entities, parameters must be set in the order they appear in a query. 
		//Parameters: foodGroupCode and foodDescription
		//Custom entities provided by you can use any other approach to determine how queries and parameters are processed.
		engine.addQuery(EngineScope.STATELESS, "foodsOfGroup")
				.onDataStore("csvDataStore")
				.fromString(" SELECT * FROM food_des WHERE fdgrp_cd = :foodGroupCode AND Shrt_Desc like :foodDescription")
				.returnDataset();

		DataStoreMapping mapping = engine.getMapping("csvDataStore", "fixedWidthDestination");

		//Here we define our mapping using a parameterized query: Parameters can be other functions, variable names, constants or plain strings.
		//This expression will be evaluated as follows:
		// 1 - The value of the variable "groupName" will be sent to the "findGroupCode" query, producing the code of the given food group.
		//     This result is then used to set the "foodGroupCode" parameter.
		// 2 - The value of variable "foodName" will be used to set the "foodDescription" parameter
		// 3 - The "foodsOfGroup" query will be called with these parameter values, and return a dataset that can be used to map data to "food". 
		EntityMapping queryMapping = mapping.map("{foodsOfGroup(findGroupCode($groupName), $foodName)}", "food");

		//Here we map the column names in the dataset produced by the "foodsOfGroup" query to the fields in "food". 
		queryMapping.identity().associate("NDB_No").to("id");
		queryMapping.value().copy("Long_Desc", "SciName").to("description", "scientific_name");
		queryMapping.reference().using("{findGroupCode($groupName)}").referTo("{groupsInUse}", "food_group").on("group");
		queryMapping.persistence().usingMetadata().deleteDisabled().updateDisabled().insertNewRows();

		//Before executing the cycle, let's set the values of each variable we are using as a parameter in "foodsOfGroup"
		engine.setVariable("groupName", "Dairy%");
		engine.setVariable("foodName", "CHEESE%");
		engine.executeCycle();

		printDataInEntities(output, "After first cycle:");

		//Let's change the variables and execute the cyle again.
		engine.setVariable("groupName", "Baby Foods");
		engine.setVariable("foodName", "%");
		engine.executeCycle();

		printDataInEntities(output, "After second cycle:");
		//##CODE_END

		printAndValidate(output);
	}

	private void printDataInEntities(StringBuilder output, String message) {
		println(output, message);
		println(output, " -- Food groups -- ");
		println(output, foodGroupOutput.getString());
		println(output);
		println(output, " -- Foods -- ");
		println(output, foodOutput.getString());
	}

	public static void main(String... args) {
		Tutorial002QueriesFunctionsAndVariables tutorial = new Tutorial002QueriesFunctionsAndVariables();

		try {
			tutorial.example001QueryMapping();
			tutorial.example002QueryWithParameters();
		} finally {
			Univocity.shutdown(engineName);
		}
		System.exit(0);
	}
}
