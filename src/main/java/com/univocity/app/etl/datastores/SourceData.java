/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.etl.datastores;

import java.io.*;

import org.zeroturnaround.zip.*;

import com.univocity.app.utils.*;

public class SourceData {

	private boolean useOldVersion = true;

	public boolean isUseOldVersion() {
		return useOldVersion;
	}

	public void setOldVersion(boolean usingOldVersion) {
		this.useOldVersion = usingOldVersion;
	}

	public Reader openFile(String fileName) {
		String directoryName = useOldVersion ? "sr25" : "sr26";
		String entry = directoryName + "/" + fileName + ".txt";
		return open(entry);
	}

	public Reader openUpdateFile(String fileName) {
		return open("sr26upd/" + fileName + ".txt");
	}

	private Reader open(String entryName) {
		File zip = FileFinder.findFile("source/data/data.zip");
		try {
			byte[] bytes = ZipUtil.unpackEntry(zip, entryName);
			ByteArrayInputStream is = new ByteArrayInputStream(bytes);
			return new InputStreamReader(is, "windows-1252");
		} catch (Exception ex) {
			throw new IllegalStateException("Error extracting contents of entry '" + entryName + "' from data.zip", ex);
		}
	}

}
