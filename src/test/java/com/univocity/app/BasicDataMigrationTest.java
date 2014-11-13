package com.univocity.app;

import org.testng.annotations.*;

import com.univocity.app.etl.*;
import com.univocity.app.etl.datastores.*;

public class BasicDataMigrationTest {

	@Test
	public void executeLoadAndUpdateTest() {

		DataStores.getInstance().getSourceData().setOldVersion(true);
		LoadSourceDatabase sourceLoad = new LoadSourceDatabase();
		MigrateDatabase migrate = new MigrateDatabase();

		try {
			sourceLoad.execute(); //Load SR25
			migrate.execute(); //Migrate

			DataStores.getInstance().getSourceData().setOldVersion(false);
			sourceLoad.execute(); //Upgrade to SR26
			migrate.execute(); //Migrate again

			DataStores.getInstance().getSourceData().setOldVersion(true);
			sourceLoad.execute(); //Downgrade to SR25
			migrate.execute(); //Migrate one last time
		} finally {
			try {
				sourceLoad.shutdown();
			} finally {
				migrate.shutdown();
			}
		}
	}
}
