package com.univocity.examples;


public class ExampleWithOriginalDatabase extends ExampleWithDatabase {

	public ExampleWithOriginalDatabase() {
		super("examples/original_schema", "originalSchema");
	}

	public String printFoodGroupTable() {
		return printTable("FD_GROUP", "FdGrp_CD", "FdGrp_Desc");
	}

	public String printFoodDescriptionTable() {
		return printTable("FOOD_DES", "NDB_No", "FdGrp_Cd", "Long_Desc", "Shrt_Desc", "ComName", "ManufacName", "Survey", "Ref_Desc", "Refuse", "SciName", "N_Factor", "Pro_Factor", "Fat_Factor", "CHO_Factor");
	}
	
	
}
