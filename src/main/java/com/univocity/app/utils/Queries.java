/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.utils;

import java.util.*;

public interface Queries {

	public abstract Set<String> getQueryNames();

	public abstract String getQuery(String name);

}
