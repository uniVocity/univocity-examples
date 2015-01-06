package com.univocity.examples;

import java.io.*;

import org.testng.annotations.*;

import com.univocity.api.*;
import com.univocity.api.config.*;
import com.univocity.api.config.builders.*;
import com.univocity.api.engine.*;
import com.univocity.api.entity.jdbc.*;
import com.univocity.api.entity.text.tsv.*;

public class Tutorial001_1Autodetection extends ExampleWithOriginalDatabase {

	private static final String engineName = "engine_001_1";

	@Test
	public void example001AutodetectMappings() {
		// initializes an engine with a CSV data store with the files under /examples/source_data/csv/
		// and a JDBC data store with the tables created with the scripts under /examples/original_schema/
		initializeEngine(engineName);

		DataIntegrationEngine engine = Univocity.getEngine(engineName);

		//##CODE_START
		//Let's create a mapping between the CSV and JDBC data stores
		DataStoreMapping dsMapping = engine.map("csvDataStore", "originalSchema");

		//Names of tables and columns match exactly. We can just autodetect everything.
		dsMapping.autodetectMappings();

		//And map all data from CSV to our database tables.
		engine.executeCycle();

		//Let's print the data in our database tables
		print(readFoodGroupTable());
		print(readFoodDescriptionTable());

		//##CODE_END

		//Here we print the results to the console and validate the output:
		printAndValidate();
	}

	@Test(dependsOnMethods = "example001AutodetectMappings")
	public void example002DumpToDir() {

		//##CODE_START
		//Now, let's map from the database tables to TSV files that do not exist anywhere (yet).
		//All we need to do is to provide an output directory where the files should be created:
		File tsvOutputDir = new File(System.getProperty("user.home") + File.separator + "TSV");

		//Let's create a TSV data store configuration with this output directory:
		TsvDataStoreConfiguration tsvConfig = new TsvDataStoreConfiguration("tsvOutput");
		tsvConfig.setOutputDirectory(tsvOutputDir, "UTF-8");
		//We want to print out headers on the TSV files to identify the column names
		tsvConfig.getDefaultEntityConfiguration().setHeaderWritingEnabled(true);

		//Let's just reuse the dataSource we already have to connect to the database loaded in the previous example
		JdbcDataStoreConfiguration jdbcConfig = new JdbcDataStoreConfiguration("database", this.dataSource);
		jdbcConfig.setSchema("public");

		//And let's configure a new data integration engine for our purposes.
		Univocity.registerEngine(new EngineConfiguration("TSV_DUMP", tsvConfig, jdbcConfig));
		DataIntegrationEngine engine = Univocity.getEngine("TSV_DUMP");

		//Now, we map from our database to the TSV data store:
		DataStoreMapping dsMapping = engine.map("database", "tsvOutput");
		//dsMapping.configurePersistenceDefaults().notUsingMetadata().deleteAll().insertNewRows();
		
		//Here, the boolean argument means that destination entities should be created automatically.
		//The destination entities will be created to match the respective configurations of detected source entities.
		//The destination data store must support dynamic creation of entities for this to work. 
		dsMapping.autodetectMappings(true);

		//After executing a mapping cycle, we will have a TSV file for each table in the database. 
		engine.executeCycle();
		//##CODE_END

		//Let's print the contents of these files:
		println(readFile(new File(tsvOutputDir.getAbsolutePath() + File.separator + "FD_GROUP.tsv")));
		println(readFile(new File(tsvOutputDir.getAbsolutePath() + File.separator + "FOOD_DES.tsv")));

		//Here we print the results to the console and validate the output:
		printAndValidate();
	}

	@Test(dependsOnMethods = "example002DumpToDir")
	public void example003GenerateSchema() {

		DataIntegrationEngine engine = Univocity.getEngine("TSV_DUMP");

		//##CODE_START
		//Here we export each entity from the "database" data store to a SQL "create table" script 
		String schemaExport = engine.exportEntities("database")
				.asCreateTableScript(DatabaseDialect.HSQLDB) //generate the script using HSQLDB's dialect
				.toObject(); //returns the export result as a String.

		//Let's print the script:
		println("--[ HSQLDB script ]--");
		println(schemaExport);

		//Again, we export the same entities, but this time we want the script generated differently:
		schemaExport = engine.exportEntities("database")
				.asCreateTableScript(DatabaseDialect.SQLServer_2012) //generate a script compatible with SQL Server 2012 dialect 
				.noNotNullConstraint() //do not create NOT NULL constraints
				.noPrimaryKeyConstraint() // do not create PRIMARY KEY constraints
				.toObject();

		//Let's see how this one shows up:
		println("--[ SQL Server script ]--");
		print(schemaExport);
		//##CODE_END
		
		Univocity.shutdown("TSV_DUMP");

		printAndValidate();

	}

}
