/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.utils;

import java.io.*;

import org.springframework.core.io.*;

public class FileFinder {

	public static File findFile(String path) {
		Resource resource = new ClassPathResource(path);
		File file;
		try {
			file = resource.getFile();
		} catch (IOException e) {
			file = new File(path);
			if (!file.exists()) {
				//we are not including the resources into the jars
				//this is needed to find the resources when executing from the IDE & test cases.
				file = new File("src/main/resources/" + path);
			}
		}

		if (file == null || !file.exists()) {
			throw new IllegalArgumentException("Unable to find file specified by path: " + path);
		}

		return file;
	}
}
