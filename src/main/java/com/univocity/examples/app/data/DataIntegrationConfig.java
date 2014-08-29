/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.app.data;

import java.util.*;

import com.univocity.examples.utils.*;

public class DataIntegrationConfig {

	private DatabaseAccessor sourceDatabaseConfig;
	private DatabaseAccessor destinationDatabaseConfig;
	private final Map<String, Runnable> processes = new TreeMap<String, Runnable>();

	public DataIntegrationConfig() {
	}

	public DatabaseAccessor getSourceDatabaseConfig() {
		return sourceDatabaseConfig;
	}

	public void setSourceDatabaseConfig(DatabaseAccessor sourceDatabaseConfig) {
		this.sourceDatabaseConfig = sourceDatabaseConfig;
	}

	public DatabaseAccessor getDestinationDatabaseConfig() {
		return destinationDatabaseConfig;
	}

	public void setDestinationDatabaseConfig(DatabaseAccessor destinationDatabaseConfig) {
		this.destinationDatabaseConfig = destinationDatabaseConfig;
	}

	public void addProcess(String name, Runnable process) {
		this.processes.put(name, process);
	}

	public Set<String> getProcessNames() {
		return processes.keySet();
	}

	public Runnable getProcess(String processName) {
		return processes.get(processName);
	}
}
