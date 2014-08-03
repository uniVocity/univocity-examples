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

public class Tutorial001Basics extends Example {

	private static final String engineName = "engine_001";

	@Test
	public void example001SimpleCopy() {
		initializeEngine(engineName);

		//##CODE_START
		//Obtains the configured engine instance
		DataIntegrationEngine engine = Univocity.getEngine(engineName);

		//Creates a mapping between the csv and fixed-width data stores.
		DataStoreMapping mapping = engine.map("csvDataStore", "fixedWidthDestination");

		//With a data store mapping, we can define the mapping between their entities.

		//We will map csvDataStore.FD_GROUP to fixedWidthDestination.food_group
		EntityMapping foodGroupMapping = mapping.map("FD_GROUP", "food_group");

		//Here we associate FD_GROUP.FdGrp_CD to food_group.id. Values will be copied directly between source and destination.
		//The identity mapping defines that an association should be created for each record mapped between source and destination.
		//A new entry will be added to uniVocity's metadata, containing the source and destination values mapped here.
		//This linkage enables complex mapping operations that will be demonstrated later on.
		foodGroupMapping.identity().associate("FdGrp_CD").to("id");

		//Copies values from FD_GROUP.FdGrp_Desc to food_group.name
		foodGroupMapping.value().copy("FdGrp_Desc").to("name");

		//Configures the mapping to delete all rows in the destination to then insert all rows mapped from the source
		foodGroupMapping.persistence().usingMetadata().deleteAll().insertNewRows();

		//Executes a data mapping cycle with the entity mappings defined so far.
		engine.executeCycle();

		//##CODE_END

		//We expect the CSV data in FD_GROUP to be fully mapped to our fixed-width entity food_group.
		//food_group was configured to store values into a String, so let's print it.
		String writtenData = foodGroupOutput.getString();
		printAndValidate(writtenData);
	}

	@Test(dependsOnMethods = "example001SimpleCopy")
	public void example002RowReaders() {
		//##CODE_START
		//Obtains the engine instance already configured
		DataIntegrationEngine engine = Univocity.getEngine(engineName);

		//Gets the existing data store mapping
		DataStoreMapping mapping = engine.getMapping("csvDataStore", "fixedWidthDestination");

		//Gets the existing entity mapping
		EntityMapping foodGroupMapping = mapping.getMapping("FD_GROUP", "food_group");

		//We can easily manipulate the input and output rows before, during and after they are mapped. To do so we use a RowReader.
		//Here we add a RowReader to discard the first 3 rows in the input.
		foodGroupMapping.addInputRowReader(new RowReader() {

			@Override
			public void processRow(Object[] inputRow, Object[] outputRow, RowMappingContext context) {
				if (context.getCurrentRow() <= 3) {
					context.discardRow();
				}
			}
		});

		//This rowReader converts descriptions to uppercase. This is an output rowReader, so we have access to
		//all data already mapped from the input, and ready to be written to the output.
		foodGroupMapping.addOutputRowReader(new RowReader() {

			private int name;

			@Override
			public void initialize(RowMappingContext context) {
				//for performance reasons, we use the initialize method to get the index of any fields we are interested in
				//before changing the output Rows. Here we get index of the output field "name"
				name = context.getOutputIndex("name");
			}

			@Override
			public void processRow(Object[] inputRow, Object[] outputRow, RowMappingContext context) {
				//converts the food group name in the destination row to uppercase
				outputRow[name] = outputRow[name].toString().toUpperCase();
			}
		});

		//Executes a data mapping cycle again
		engine.executeCycle();

		//##CODE_END

		//We expect to have 2 records in the output, all uppercase.
		String writtenData = foodGroupOutput.getString();
		printAndValidate(writtenData);
	}

	@Test(dependsOnMethods = "example002RowReaders")
	public void example003Functions() {
		//Obtains the engine instance already configured
		DataIntegrationEngine engine = Univocity.getEngine(engineName);

		//##CODE_START

		//Adds a function to the engine that converts Strings to Integers.
		//The APPLICATION scope makes the engine retain and reuse the result of function calls
		//using a given parameter, until the engine is shut down.
		engine.addFunction(EngineScope.APPLICATION, "toInteger", new FunctionCall<Integer, String>() {
			@Override
			public Integer execute(String input) {
				return Integer.parseInt(input);
			}
		});

		//Adds a function to the engine that converts Strings to lower case.
		//With the CYCLE scope, values are held while a cycle is active. Once it completes all
		//values in this scope will be discarded.
		engine.addFunction(EngineScope.CYCLE, "toLowerCase", new FunctionCall<String, String>() {
			@Override
			public String execute(String input) {
				return input.toLowerCase();
			}
		});

		//Adds a function to the engine that trims Strings.
		//With the STATELESS scope, results of function calls are never reused.
		engine.addFunction(EngineScope.STATELESS, "trim", new FunctionCall<String, String>() {
			@Override
			public String execute(String input) {
				return input.trim();
			}
		});

		//Gets the existing data store mapping
		DataStoreMapping mapping = engine.getMapping("csvDataStore", "fixedWidthDestination");

		//Removes the existing entity mapping
		mapping.removeMapping("FD_GROUP", "food_group");

		//We will map csvDataStore.FD_GROUP to fixedWidthDestination.food_group again
		EntityMapping foodGroupMapping = mapping.map("FD_GROUP", "food_group");

		//Here we associate FD_GROUP.FdGrp_CD to food_group.id, but now FdGrp_CD will be converted to an integer value
		//before being associated with the destination id
		foodGroupMapping.identity().associate("FdGrp_CD").to("id").readWith("toInteger");

		//Copies values from FD_GROUP.FdGrp_Desc to food_group.name. All values read from FdGrp_Desc will be
		//trimmed then converted to lower case, as specified in the sequence of functions
		foodGroupMapping.value().copy("FdGrp_Desc").to("name").readingWith("trim", "toLowerCase");

		//Configures the mapping to keep all rows in the destination and only insert new rows in the source
		//Records mapped in a previous cycle won't be discarded nor updated.
		foodGroupMapping.persistence().usingMetadata().deleteDisabled().updateDisabled().insertNewRows();

		//Executes a data mapping cycle again
		engine.executeCycle();

		//##CODE_END

		//Expect to have 5 records in the output, 2 with descriptions in uppercase (from the previous cycle) and 3 new.
		String writtenData = foodGroupOutput.getString();
		printAndValidate(writtenData);
	}

	@Test(dependsOnMethods = "example003Functions")
	public void example004ReferenceMapping() {
		//Obtains the engine instance already configured
		DataIntegrationEngine engine = Univocity.getEngine(engineName);

		//##CODE_START
		//Gets the existing data store mapping
		DataStoreMapping mapping = engine.getMapping("csvDataStore", "fixedWidthDestination");

		//Let's add a mapping between the food description CSV and the in-memory, fixed-with entity
		EntityMapping foodMapping = mapping.map("FOOD_DES", "food");
		foodMapping.identity().associate("NDB_No").to("id");
		foodMapping.value().copy("Long_Desc", "SciName").to("description", "scientific_name");

		//In the source data store, FOOD_DES contains a reference to FD_GROUP. In the destination, "food" contains a reference to "food_group".
		//The reference mapping uses uniVocity's metadata to restore references to identifiers that were mapped and transformed.
		//
		//Our mapping from FD_GROUP to "group" transformed the values in "FdGrp_Cd" to integers, using the "toInteger" function.
		//Now, when reading the field in FOOD_DES that references FD_GROUP, we need to convert its values using the "toInteger" function.
		//The results will be used to query uniVocity's metadata and restore the corresponding values used as identifiers of "food_group"
		foodMapping.reference().using("FdGrp_Cd").referTo("FD_GROUP", "food_group").on("group").readingWith("toInteger");

		foodMapping.persistence().usingMetadata().deleteAll().insertNewRows();

		//Executes a data mapping cycle again
		engine.executeCycle();

		//##CODE_END

		String foodGroupData = foodGroupOutput.getString();
		String foodData = foodOutput.getString();

		printAndValidate(foodGroupData + "\n" + foodData);
	}

	@Test(dependsOnMethods = "example004ReferenceMapping")
	public void example005LifecycleInterceptors() {
		//In this test we will intercept lifecycle events of the data integration engine
		//Let's print information obtained from these events into a String.
		final StringBuilder out = new StringBuilder();

		//Obtains the engine instance already configured
		DataIntegrationEngine engine = Univocity.getEngine(engineName);

		//##CODE_START
		engine.addInterceptor(new EngineLifecycleInterceptor() {

			//prints the current active scope in the engine.
			private String getScope(EngineLifecycleContext context) {
				return ". Current scope: " + context.getExecutionContext().getCurrentActiveScope();
			}

			//prints the current mapping being executed. The --X arrow represents a data removal
			//operation, whilst the --> represents a data mapping.
			private String getMapping(EngineLifecycleContext context) {
				EntityMappingContext entityMapping = context.getCurrentEntityMapping();
				String arrow = entityMapping.isExclusionMapping() ? " --X " : " --> ";
				return entityMapping.getSourceEntity() + arrow + entityMapping.getDestinationEntity();
			}

			//Executed when the engine becomes available after a data mapping cycle.
			@Override
			public void engineReady(EngineLifecycleContext context) {
				String name = context.getEngineName();
				int cycle = context.getCurrentCycle();

				println(out, name + " ready. Cycles executed: " + cycle + getScope(context));
			}

			//Called when a data mapping cycle has been started
			@Override
			public void cycleStarted(EngineLifecycleContext context) {
				println(out);
				println(out, "Starting new cycle: " + context.getCurrentCycle() + getScope(context));
			}

			//Called when an individual mapping between two entities is about to be executed.
			//Note some mappings can be exclusion mappings generated automatically by uniVocity
			//when deletions are enabled in the entity mappings.
			@Override
			public void mappingStarted(EngineLifecycleContext context) {
				println(out, "  Executing: " + getMapping(context) + getScope(context));
			}

			//Called when an individual mapping between two entities has been completed
			@Override
			public void mappingCompleted(EngineLifecycleContext context) {
				println(out, "  Completed: " + getMapping(context) + getScope(context));
			}

			//Called when a data mapping cycle has been completed
			@Override
			public void cycleCompleted(EngineLifecycleContext context) {
				println(out, "Completed cycle: " + context.getCurrentCycle() + getScope(context));
				println(out);
			}

			//Called when the engine begins to shut down
			@Override
			public void engineShuttingDown(EngineLifecycleContext context) {
				println(out, "Shutting down " + context.getEngineName());
			}

			//Called when the shut down process is finalized and the engine is effectively stopped.
			@Override
			public void engineStopped(EngineLifecycleContext context) {
				println(out, context.getEngineName() + " shut down.");
			}
		});

		//executes a mapping cycle to trigger some interceptor methods.
		engine.executeCycle();

		//shuts down the engine to trigger "engineShuttingDown" and "engineStopped"
		Univocity.shutdown(engineName);

		//##CODE_END
		printAndValidate(out);
	}

	@Test(dependsOnMethods = "example005LifecycleInterceptors")
	public void example006MapFunctions() {
		initializeEngine("hashMap");
		DataIntegrationEngine engine = Univocity.getEngine("hashMap");

		DataStoreMapping mapping = engine.map("csvDataStore", "fixedWidthDestination");

		//##CODE_START
		//Maps can be used as functions as well. Here we create a map from
		//codes of food groups to descriptions:
		Map<String, String> groupCodeToGroupNames = new HashMap<String, String>();
		groupCodeToGroupNames.put("0100", "Dairy");
		groupCodeToGroupNames.put("0300", "Baby");
		groupCodeToGroupNames.put("1400", "Fats");

		//This will create a function named "getNameOfGroup" that will use the given map to produce values
		engine.addMap("getNameOfGroup", groupCodeToGroupNames);

		//Let's test this function with a mapping to food descriptions:
		EntityMapping foodMapping = mapping.map("FOOD_DES", "food");
		foodMapping.identity().associate("NDB_No").to("id");
		//Here we invoke the "getNameOfGroup" function using values read from FdGrp_CD
		//This will return the description associated with each code in the map.
		foodMapping.value().copy("FdGrp_CD").to("group").readingWith("getNameOfGroup");
		foodMapping.value().copy("Long_Desc").to("description");

		engine.executeCycle();
		//##CODE_END
		printAndValidate(foodOutput.getString());

	}

	@Test(dependsOnMethods = "example006MapFunctions")
	public void example007ObjectsWithFunctions() {

		initializeEngine("functions");
		DataIntegrationEngine engine = Univocity.getEngine("functions");

		//##CODE_START
		//Here we use an object whose class contains methods annotated with @FunctionWrapper.
		//The annotated methods in this object will be used to create functions accessible by uniVocity.
		//This is useful to manipulate the state of data using multiple functions and to expose
		//information from a complex object that can't be easily used in mappings or datasets.
		NameSplitter splitter = new NameSplitter();
		engine.addFunctions(splitter);

		DataStoreMapping mapping = engine.map("csvDataStore", "fixedWidthDestination");

		EntityMapping foodMapping = mapping.map("FOOD_DES", "food");
		foodMapping.identity().associate("NDB_No").to("id");
		//Here we call the "toCodes" function in our splitter object to convert each string after a comma into a code.
		//Each description in the destination will have numeric codes concatenated with the pipe character.
		foodMapping.value().copy("Long_Desc").to("description").readingWith("toCodes");

		engine.executeCycle();
		//##CODE_END
		String mapOfCodes = splitter.printMapOfCodesToNames();

		printAndValidate(mapOfCodes + "\n" + foodOutput.getString());
	}

	public static void main(String... args) {
		Tutorial001Basics tutorial = new Tutorial001Basics();

		try {
			tutorial.example001SimpleCopy();
			tutorial.example002RowReaders();
			tutorial.example003Functions();
			tutorial.example004ReferenceMapping();
			tutorial.example005LifecycleInterceptors();
		} finally {
			Univocity.shutdown(engineName);
		}

		try {
			tutorial.example006MapFunctions();
		} finally {
			Univocity.shutdown("hashMap");
		}

		try {
			tutorial.example007ObjectsWithFunctions();
		} finally {
			Univocity.shutdown("functions");
		}
		System.exit(0);
	}
}
