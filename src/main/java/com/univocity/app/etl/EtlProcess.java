/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.etl;

import com.univocity.api.*;
import com.univocity.api.engine.*;

public abstract class EtlProcess {

	public final void execute() {
		DataIntegrationEngine engine = Univocity.getEngine(getEngineName());
		engine.executeCycle();
	}

	public final void execute(String... entitiesToProcess) {
		DataIntegrationEngine engine = Univocity.getEngine(getEngineName());
		engine.executeCycle(entitiesToProcess);
	}

	public abstract String getEngineName();
}
