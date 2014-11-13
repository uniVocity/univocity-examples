/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.etl;

import com.univocity.api.*;
import com.univocity.api.config.*;
import com.univocity.api.config.builders.*;
import com.univocity.api.engine.*;
import com.univocity.api.entity.custom.*;
import com.univocity.app.etl.datastores.*;

public class MigrateDatabase extends EtlProcess {

	public MigrateDatabase() {
		DataStores stores = DataStores.getInstance();
		DataStoreConfiguration sourceConfig = stores.getSourceDatabaseConfig();
		DataStoreConfiguration destinationDatabaseConfig = stores.getDestinationDatabaseConfig();

		EngineConfiguration engineConfig = new EngineConfiguration(getEngineName(), sourceConfig, destinationDatabaseConfig);
		Univocity.registerEngine(engineConfig);

		configureMappings();
	}

	@Override
	public final String getEngineName() {
		return "Migration";
	}

	private void configureMappings() {
		DataIntegrationEngine engine = Univocity.getEngine(getEngineName());
		engine.addDatasetProducer(EngineScope.CYCLE, new FoodProcessor()).on("FOOD_DES", "Ndb_no", "Long_Desc");

		DataStoreMapping mapping = engine.map("source", "destination");
		mapping.configurePersistenceDefaults().usingMetadata().deleteAbsent().updateModified().insertNewRows();

		EntityMapping map = mapping.map("food_names", "food_name");
		map.identity().associate("name").toGeneratedId("id");
		map.value().copy("name").to("description");

		map = mapping.map("food_state_names", "food_state");
		map.identity().associate("name").toGeneratedId("id");
		map.value().copy("name").to("description");

		map = mapping.map("food_name_details", "food");
		map.identity().associate("food_code").toGeneratedId("id");
		map.reference().using("name").referTo("food_names", "food_name").on("name_id").directly().onMismatch().abort();

		map = mapping.map("food_state_details", "state_of_food");
		map.identity().associate("food_code", "name", "order").to("food_id", "state_id", "sequence");
		map.reference().using("food_code").referTo("food_name_details", "food").on("food_id");
		map.reference().using("name").referTo("food_state_names", "food_state").on("state_id");

		map = mapping.map("FD_GROUP", "food_group");
		map.identity().associate("FdGrp_CD").toGeneratedId("id");
		map.value().copy("FdGrp_Desc").to("description");

		map = mapping.map("FOOD_DES", "food");
		map.identity().associate("NDB_No").to("id");
		map.reference().using("NDB_No").referTo("food_name_details", "food").on("id");
		map.value().copy("CHO_Factor", "Fat_Factor", "Pro_Factor", "N_Factor")
				.to("carbohydrate_factor", "fat_factor", "protein_factor", "nitrogen_protein_factor");
		map.persistence().usingMetadata().deleteDisabled().updateModified().updateNewRows();

		map = mapping.map("FOOD_DES", "group_of_food");
		map.identity().associate("NDB_No", "FdGrp_Cd").to("food_id", "group_id");
		map.reference().using("NDB_No").referTo("food_name_details", "food").on("food_id");
		map.reference().using("FdGrp_Cd").referTo("FD_GROUP", "food_group").on("group_id");

		engine.addFunction(EngineScope.STATELESS, "normalize", new FunctionCall<String, String>() {
			@Override
			public String execute(String input) {
				return input == null ? null : input.trim().toLowerCase();
			}
		});

		map = mapping.map("WEIGHT", "weight");
		map.identity().associate("Msre_Desc").toGeneratedId("id").readingWith("normalize");
		map.value().copy("Msre_Desc").to("description").readingWith("normalize");
		map.addOutputRowReader(new NoDuplicatesRowReader("description"));

		map = mapping.map("WEIGHT", "weight_of_food");
		map.identity().associate("NDB_No", "Msre_Desc", "Amount").to("food_id", "weight_id", "amount");
		map.reference().using("NDB_No").referTo("food_name_details", "food").on("food_id");
		map.reference().using("Msre_Desc").referTo("WEIGHT", "weight").on("weight_id").readingWith("normalize");
		map.value().copy("Gm_Wgt").to("grams");
		map.addOutputRowReader(new NoDuplicatesRowReader("weight_id", "food_id", "amount"));

		map = mapping.map("NUTR_DEF", "nutrient");
		map.identity().associate("Nutr_No").toGeneratedId("id");
		map.value().copy("NutrDesc", "units", "tagname").to("description", "unit", "acronym");

		map = mapping.map("NUT_DATA", "nutrient_of_food");
		map.identity().associate("NDB_No", "Nutr_No").to("food_id", "nutrient_id");
		map.reference().using("NDB_No").referTo("food_name_details", "food").on("food_id");
		map.reference().using("Nutr_No").referTo("NUTR_DEF", "nutrient").on("nutrient_id");
		map.value().copy("Nutr_Val").to("amount");
	}

	public static void main(String... args) {
		LoadSourceDatabase load = new LoadSourceDatabase();
		MigrateDatabase migrate = new MigrateDatabase();
		try {
			load.execute();
			migrate.execute();
		} finally {
			try {
				load.shutdown();
			} finally {
				migrate.shutdown();
			}
		}
	}
}
