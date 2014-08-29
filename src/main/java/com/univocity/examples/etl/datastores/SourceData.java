/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.etl.datastores;

import java.io.*;

import org.zeroturnaround.zip.*;

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

		InputStream zip = SourceData.class.getResourceAsStream("/source/data/data.zip");
		try {
			byte[] bytes = ZipUtil.unpackEntry(zip, entry);
			ByteArrayInputStream is = new ByteArrayInputStream(bytes);
			return new InputStreamReader(is, "windows-1252");
		} catch (Exception ex) {
			throw new IllegalStateException("Error extracting contents of entry '" + entry + "' from data.zip", ex);
		} finally {
			try {
				zip.close();
			} catch (IOException ex) {
				throw new IllegalStateException("Error closing data.zip stream", ex);
			}
		}
	}
}
