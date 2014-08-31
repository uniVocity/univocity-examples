package com.univocity.app;

import java.util.*;

import com.univocity.api.engine.*;
import com.univocity.parsers.common.*;
import com.univocity.parsers.common.processor.*;

public class RowDataCollector extends RowReader implements RowProcessor {

	private final int[] fieldIndexes;
	private final String[] fieldNames;
	private final List<String> persisted = new ArrayList<String>();
	private final StringBuilder row = new StringBuilder();

	private final List<String> expected = new ArrayList<String>();

	public RowDataCollector(String... fieldNames) {
		this.fieldNames = fieldNames.clone();
		this.fieldIndexes = new int[fieldNames.length];
	}

	@Override
	public void initialize(RowMappingContext context) {
		persisted.clear();
		for (int i = 0; i < fieldNames.length; i++) {
			fieldIndexes[i] = context.getOutputIndex(fieldNames[i]);
		}
	}

	@Override
	public void processRow(Object[] inputRow, Object[] outputRow, RowMappingContext context) {
		row.setLength(0);

		for (int index : fieldIndexes) {
			if (row.length() > 0) {
				row.append('^');
			}
			row.append(outputRow[index]);
		}

		persisted.add(row.toString());
	}

	public List<String> getPersistedData() {
		return persisted;
	}

	@Override
	public void processEnded(ParsingContext arg0) {
	}

	@Override
	public void processStarted(ParsingContext arg0) {
		expected.clear();
	}

	@Override
	public void rowProcessed(String[] parsedRow, ParsingContext arg1) {
		row.setLength(0);

		//identifiers are the first columns in our update files.
		for (int i = 0; i < fieldNames.length; i++) {
			if (row.length() > 0) {
				row.append('^');
			}
			row.append(parsedRow[i]);
		}

		expected.add(row.toString());
	}

	public List<String> getExpected() {
		return expected;
	}
}