package com.univocity.examples;

import java.io.*;
import java.util.*;


public class ExampleWithOriginalDatabase extends ExampleWithDatabase {

	public ExampleWithOriginalDatabase() {
		super("examples/original_schema", "originalSchema");
	}

	public String readFoodGroupTable() {
		return printTable("FD_GROUP", "FdGrp_CD", "FdGrp_Desc");
	}

	public String readFoodDescriptionTable() {
		return printTable("FOOD_DES", "NDB_No", "FdGrp_Cd", "Long_Desc", "Shrt_Desc", "ComName", "ManufacName", "Survey", "Ref_Desc", "Refuse", "SciName", "N_Factor", "Pro_Factor", "Fat_Factor", "CHO_Factor");
	}
	
	public String readFile(File file){
		String result = "==[ " +file.getName() + " ]==\n";
		Scanner scanner = null;
		try {
			scanner = new Scanner(file, "UTF-8");
			scanner.useDelimiter("\\A");
			return result + (scanner.hasNext() ? scanner.next() : "");
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Unable to read contents of file " + file.getAbsolutePath(), e);
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}
}
