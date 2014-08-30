/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.etl;

import com.univocity.api.*;

public abstract class EtlProcess {

	public final void execute(String... entitiesToProcess) {
		Univocity.getEngine(getEngineName()).executeCycle(entitiesToProcess);
	}

	protected abstract String getEngineName();
}
