/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.app;

import com.univocity.app.app.data.*;
import com.univocity.app.etl.*;
import com.univocity.app.etl.datastores.*;

public class SampleAppConfig extends DataIntegrationConfig {

	public SampleAppConfig() {

		setSourceDatabaseConfig(DataStores.getInstance().getSourceDatabase());
		setDestinationDatabaseConfig(DataStores.getInstance().getDestinationDatabase());

		LoadSourceDatabase loadSourceDatabaseProcess = new LoadSourceDatabase();
		MigrateDatabase migrateFromSourceDatabaseProcess = new MigrateDatabase("source");

		addProcess("Load/update source database using SR25 files", loadSourceDatabaseProcess, true);
		addProcess("Load/update source database using SR26 files", loadSourceDatabaseProcess, false);
		addProcess("Migrate from source database to destination database", migrateFromSourceDatabaseProcess, null);

		String[] entities = new String[] { "FD_GROUP", "FOOD_DES", "NUTR_DEF", "WEIGHT" };
		addProcess("Skip nutrient data - Load/update source database using SR25 files", loadSourceDatabaseProcess, true, entities);
		addProcess("Skip nutrient data - Load/update source database using SR26 files", loadSourceDatabaseProcess, false, entities);

		entities = new String[] { "food_name", "food_state", "food", "state_of_food", "food_group", "group_of_Food", "weight", "weight_of_food", "nutrient" };
		addProcess("Skip nutrient data - Migrate from source database to destination database", migrateFromSourceDatabaseProcess, null, entities);
	}

	private void addProcess(String description, final EtlProcess process, final Boolean fileVersion, final String... entitiesToUse) {
		addProcess(description, new Runnable() {
			@Override
			public void run() {
				if (fileVersion != null) {
					DataStores.getInstance().getSourceData().setOldVersion(fileVersion);
				}
				process.execute(entitiesToUse);
			}
		});
	}
}
