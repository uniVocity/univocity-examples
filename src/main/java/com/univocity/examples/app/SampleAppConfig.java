/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.app;

import com.univocity.examples.app.data.*;
import com.univocity.examples.etl.*;
import com.univocity.examples.etl.datastores.*;

public class SampleAppConfig extends DataIntegrationConfig {

	public SampleAppConfig() {

		setSourceDatabaseConfig(DataStores.getInstance().getSourceDatabase());
		setDestinationDatabaseConfig(DataStores.getInstance().getDestinationDatabase());

		final LoadSourceDatabase loadSourceDatabaseProcess = new LoadSourceDatabase();
		final MigrateDatabase migrateFromSourceDatabaseProcess = new MigrateDatabase("source");
		final MigrateDatabase migrateFromDataFileProcess = new MigrateDatabase("data");

		addProcess("Load/update source database using SR25 files", new Runnable() {
			@Override
			public void run() {
				DataStores.getInstance().getSourceData().setOldVersion(true);
				loadSourceDatabaseProcess.run();
			}
		});

		addProcess("Load/update source database using SR26 files", new Runnable() {
			@Override
			public void run() {
				DataStores.getInstance().getSourceData().setOldVersion(false);
				loadSourceDatabaseProcess.run();
			}
		});

		addProcess("Migrate from source database to destination database", new Runnable() {
			@Override
			public void run() {
				migrateFromSourceDatabaseProcess.run();
			}
		});

		addProcess("Migrate from SR25 files to destination database", new Runnable() {
			@Override
			public void run() {
				DataStores.getInstance().getSourceData().setOldVersion(true);
				migrateFromDataFileProcess.run();
			}
		});

		addProcess("Migrate from SR26 files to destination database", new Runnable() {
			@Override
			public void run() {
				DataStores.getInstance().getSourceData().setOldVersion(false);
				migrateFromDataFileProcess.run();
			}
		});
	}

}
