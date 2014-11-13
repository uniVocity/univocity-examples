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

public class LoadSourceDatabase extends EtlProcess {

	public LoadSourceDatabase() {
		DataStores stores = DataStores.getInstance();
		DataStoreConfiguration sourceDataConfig = stores.getSourceDataConfig();
		DataStoreConfiguration sourceDatabaseConfig = stores.getSourceDatabaseConfig();

		EngineConfiguration engineConfig = new EngineConfiguration(getEngineName(), sourceDataConfig, sourceDatabaseConfig);
		Univocity.registerEngine(engineConfig);

		configureMappings();
	}

	private void configureMappings() {
		DataIntegrationEngine engine = Univocity.getEngine(getEngineName());

		DataStoreMapping mapping = engine.map("data", "source");

		mapping.configurePersistenceDefaults().usingMetadata().deleteAbsent().updateModified().insertNewRows();

		mapping.autodetectMappings();

		engine.setMappingSequence("FD_GROUP", "FOOD_DES", "NUTR_DEF", "WEIGHT", "NUT_DATA");
	}

	@Override
	public String getEngineName() {
		return "SourceLoader";
	}

	public static void main(String... args) {
		LoadSourceDatabase process = new LoadSourceDatabase();
		try {
			process.execute();
		} finally {
			process.shutdown();
		}
	}
}
