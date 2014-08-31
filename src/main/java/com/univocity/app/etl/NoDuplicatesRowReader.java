/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.etl;

import java.util.*;

import com.univocity.api.engine.*;

public class NoDuplicatesRowReader extends RowReader {

	private final int[] fieldIndexes;
	private final String[] fieldNames;
	private final Set<String> inserted = new HashSet<String>();
	private final StringBuilder row = new StringBuilder();

	public NoDuplicatesRowReader(String... fieldNames) {
		this.fieldNames = fieldNames.clone();
		this.fieldIndexes = new int[fieldNames.length];
	}

	@Override
	public void initialize(RowMappingContext context) {
		for (int i = 0; i < fieldNames.length; i++) {
			fieldIndexes[i] = context.getOutputIndex(fieldNames[i]);
		}
	}

	@Override
	public void processRow(Object[] inputRow, Object[] outputRow, RowMappingContext context) {
		row.setLength(0);

		for (int index : fieldIndexes) {
			row.append(outputRow[index]).append('|');
		}

		String data = row.toString();

		if (inserted.contains(data)) {
			context.discardRow();
		} else {
			inserted.add(data);
		}
	}

	@Override
	public void cleanup(RowMappingContext context) {
		inserted.clear();
		row.setLength(0);
	}
}
