/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.custom;

import com.univocity.api.config.*;
import com.univocity.api.engine.*;
import com.univocity.api.entity.custom.*;

/**
 * This is a custom data store factory that "knows" how to instantiate and configure our custom data store {@link MyDataStore}
 *
 * Use {@link EngineConfiguration#addCustomDataStoreFactories(CustomDataStoreFactory...)} to define your custom data store factories to be used
 * by a {@link DataIntegrationEngine}.
 *
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 *
 */
public class MyDataStoreFactory implements CustomDataStoreFactory<MyDataStoreConfiguration> {
	@Override
	public CustomDataStore<?> newDataStore(MyDataStoreConfiguration configuration) {
		return new MyDataStore(configuration);
	}

	@Override
	public Class<MyDataStoreConfiguration> getConfigurationType() {
		return MyDataStoreConfiguration.class;
	}
}
