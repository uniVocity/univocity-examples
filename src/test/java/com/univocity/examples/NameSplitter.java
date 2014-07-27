/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples;

import java.util.*;
import java.util.Map.Entry;

import com.univocity.api.config.annotation.*;
import com.univocity.api.engine.*;

/**
 * This class demonstrates how an object can be used to back some specific function used by uniVocity.
 * 
 * This is a simple example that splits strings between commas and converts them into a numeric code.
 * An input string such as "something, then something else, something" will have its 
 * components converted to an output string such as "5|10|5"
 * 
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 *
 */
public class NameSplitter {

	private int lastCode;
	private Map<String, Integer> nameToCode = new HashMap<String, Integer>();
	private Map<Integer, String> codeToName = new HashMap<Integer, String>();

	//the scope of this function determines that this method will be called once for each individual parameter
	//passed through the method. After that the previous result will be reused for repeated parameters.
	//This is useful to manage expensive operations such as retrieving data from an external service. 
	@FunctionWrapper(scope = EngineScope.APPLICATION)
	public String toCodes(String name) {
		StringBuilder out = new StringBuilder();

		String[] parts = name.split(",");
		for (String part : parts) {
			if (out.length() > 0) {
				out.append('|');
			}
			int code = addAndReturnCode(part);
			out.append(code);
		}

		return out.toString();
	}

	private int addAndReturnCode(String name) {
		name = name.trim().toLowerCase();
		if (!nameToCode.containsKey(name)) {
			lastCode++;
			nameToCode.put(name, lastCode);
			codeToName.put(lastCode, name);
		}

		return nameToCode.get(name);
	}

	public String printMapOfCodesToNames() {
		StringBuilder out = new StringBuilder();
		for (Entry<Integer, String> e : codeToName.entrySet()) {
			out.append(e.getKey()).append(" - ").append(e.getValue()).append('\n');
		}
		return out.toString();
	}
}
