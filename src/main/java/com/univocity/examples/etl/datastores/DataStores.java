/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.etl.datastores;

import java.io.*;

import com.univocity.api.common.*;
import com.univocity.api.entity.custom.*;
import com.univocity.api.entity.jdbc.*;
import com.univocity.api.entity.text.csv.*;
import com.univocity.examples.utils.*;

public class DataStores {

	private final SourceData sourceData;
	private final DatabaseAccessor sourceDatabase;
	private final DatabaseAccessor destinationDatabase;

	private CsvDataStoreConfiguration sourceDataConfig;
	private JdbcDataStoreConfiguration sourceDatabaseConfig;
	private JdbcDataStoreConfiguration destinationDatabaseConfig;

	private static final DataStores instance = new DataStores();

	public static DataStores getInstance() {
		return instance;
	}

	private DataStores() {
		sourceData = new SourceData();
		sourceDatabase = new DatabaseAccessor("source", "source/db/", "source/db/queries.properties");
		;
		destinationDatabase = new DatabaseAccessor("destination", "destination/db/", "destination/db/queries.properties");

		initializeSourceDataConfig();
		initializeSourceDatabaseConfig();
		initializeDestinationDatabaseConfig();
	}

	private void initializeSourceDataConfig() {
		sourceDataConfig = new CsvDataStoreConfiguration("data");
		sourceDataConfig.setLimitOfRowsLoadedInMemory(10000);

		sourceDataConfig.getDefaultEntityConfiguration().setHeaderExtractionEnabled(false);

		CsvFormat format = sourceDataConfig.getDefaultEntityConfiguration().getFormat();
		format.setDelimiter('^');
		format.setQuote('~');
		format.setQuoteEscape('~');

		addEntryFromZip(sourceDataConfig, "FD_GROUP", "FdGrp_CD, FdGrp_Desc");
		addEntryFromZip(sourceDataConfig, "FOOD_DES", "NDB_No,FdGrp_Cd,Long_Desc,Shrt_Desc,ComName,ManufacName,Survey,Ref_Desc,Refuse,SciName,N_Factor,Pro_Factor,Fat_Factor,CHO_Factor");
		addEntryFromZip(sourceDataConfig, "NUT_DATA", "NDB_No,Nutr_No, Nutr_Val,Num_Data_Pts,Std_Error,Src_Cd,Deriv_Cd,Ref_NDB_No,Add_Nutr_Mark,Num_Studies,Min,Max,DF,Low_EB,Up_EB,Stat_cmt,AddMod_Date,CC");
		addEntryFromZip(sourceDataConfig, "NUTR_DEF", "Nutr_No,Units,Tagname,NutrDesc,Num_Dec,SR_Order");
		addEntryFromZip(sourceDataConfig, "WEIGHT", "NDB_No,Seq,Amount,Msre_Desc,Gm_Wgt,Num_Data_Pts");
	}

	private void addEntryFromZip(CsvDataStoreConfiguration dataStore, final String zipEntryName, String headers) {
		ReaderProvider readerProvider = new ReaderProvider() {
			@Override
			public Reader getResource() {
				return sourceData.openFile(zipEntryName);
			}
		};
		dataStore.addEntity(zipEntryName, readerProvider);

		CsvEntityConfiguration entity = dataStore.getEntityConfiguration(zipEntryName);
		entity.setHeaders(headers.split(","));
	}

	private void initializeSourceDatabaseConfig() {
		sourceDatabaseConfig = new JdbcDataStoreConfiguration("source", this.sourceDatabase.getDataSource());
		sourceDatabaseConfig.setSchema("public");
		sourceDatabaseConfig.setLimitOfRowsLoadedInMemory(10000);
		sourceDatabaseConfig.getDefaultEntityConfiguration().retrieveGeneratedKeysUsingStatement(true);
	}

	private void initializeDestinationDatabaseConfig() {
		destinationDatabaseConfig = new JdbcDataStoreConfiguration("destination", this.destinationDatabase.getDataSource());
		destinationDatabaseConfig.setSchema("public");
		destinationDatabaseConfig.setLimitOfRowsLoadedInMemory(10000);
		destinationDatabaseConfig.getDefaultEntityConfiguration().retrieveGeneratedKeysUsingStatement(true);
	}

	public DataStoreConfiguration getSourceDataConfig() {
		return sourceDataConfig;
	}

	public DataStoreConfiguration getSourceDatabaseConfig() {
		return sourceDatabaseConfig;
	}

	public DataStoreConfiguration getDestinationDatabaseConfig() {
		return destinationDatabaseConfig;
	}

	public SourceData getSourceData() {
		return sourceData;
	}

	public DatabaseAccessor getSourceDatabase() {
		return sourceDatabase;
	}

	public DatabaseAccessor getDestinationDatabase() {
		return destinationDatabase;
	}
}
