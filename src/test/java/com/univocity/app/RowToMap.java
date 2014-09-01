/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app;

import java.util.*;

import com.univocity.parsers.common.*;
import com.univocity.parsers.common.processor.*;

public class RowToMap implements RowProcessor {

	private Map<String, String> processedRows;
	private final String[] fieldNames;

	public RowToMap(String[] fieldNames) {
		this.fieldNames = fieldNames;
	}

	@Override
	public void processEnded(ParsingContext arg0) {

	}

	@Override
	public void processStarted(ParsingContext arg0) {
		processedRows = new HashMap<String, String>();
	}

	@Override
	public void rowProcessed(String[] row, ParsingContext arg1) {
		String key;
		if (fieldNames.length == 1) {
			key = row[0];
		} else {
			key = row[0] + '^' + row[1];
		}

		String value = Arrays.toString(row);
		processedRows.put(key, value);
	}

	public Map<String, String> getMap() {
		return processedRows;
	}
}
