/*******************************************************************************
 * Copyright (c) 2015 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app;

import org.testng.annotations.*;

import com.univocity.app.etl.*;
import com.univocity.app.etl.datastores.*;

public class BasicDataMigrationTest {

	private LoadSourceDatabase sourceLoad;
	private MigrateDatabase migrate;

	@BeforeClass
	public void initialize() {
		sourceLoad = new LoadSourceDatabase() {
			@Override
			public String getEngineName() {
				return "SourceLoader2";
			}
		};
		migrate = new MigrateDatabase() {
			@Override
			public String getEngineName() {
				return "Migration2";
			}
		};
	}

	@AfterClass
	public void shutdown() {
		sourceLoad.shutdown();
		migrate.shutdown();
		DataStores.getInstance().getDestinationDatabase().shutdown();
		DataStores.getInstance().getSourceDatabase().shutdown();
	}

	@Test
	public void executeLoadAndUpdateTest() {
		DataStores.getInstance().getSourceData().setOldVersion(true);

		sourceLoad.execute(); //Load SR25
		migrate.execute(); //Migrate

		DataStores.getInstance().getSourceData().setOldVersion(false);
		sourceLoad.execute(); //Upgrade to SR26
		migrate.execute(); //Migrate again

		DataStores.getInstance().getSourceData().setOldVersion(true);
		sourceLoad.execute(); //Downgrade to SR25
		migrate.execute(); //Migrate one last time
	}
}
