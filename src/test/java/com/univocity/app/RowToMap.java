package com.univocity.app;

import java.util.*;

import com.univocity.parsers.common.*;
import com.univocity.parsers.common.processor.*;

public class RowToMap implements RowProcessor {

	private Map<String, String> processedRows;

	@Override
	public void processEnded(ParsingContext arg0) {

	}

	@Override
	public void processStarted(ParsingContext arg0) {
		processedRows = new HashMap<String, String>();
	}

	@Override
	public void rowProcessed(String[] row, ParsingContext arg1) {
		String key = row[0] + "^" + row[1];
		String value = Arrays.toString(row);
		processedRows.put(key, value);
	}

	public Map<String, String> getMap() {
		return processedRows;
	}
}
