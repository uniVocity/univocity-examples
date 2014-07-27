/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples;

import com.univocity.api.entity.jdbc.*;

public class LogicalExclusionSelect extends SqlProducer {
	@Override
	public String newSelectStatement(String tableName, String[] columnNames) {
		//Only returns rows where the "deleted" flag is set to 'N'
		return "select " + commas(columnNames) + " from " + tableName + " where deleted = 'N'";
	}

	private String commas(String[] columnNames) {
		StringBuilder out = new StringBuilder();
		for (String column : columnNames) {
			if (out.length() > 0) {
				out.append(',');
			}
			out.append(column);
		}
		return out.toString();
	}
}
