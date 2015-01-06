/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples;

abstract class ExampleWithNewDatabase extends ExampleWithDatabase {

	public ExampleWithNewDatabase() {
		super("examples/new_schema", "newSchema");
	}

	public String printFoodGroupDetailsTable() {
		return printTable("food_group_details", "id", "loc", "description");
	}

	public String printFoodGroupTables() {
		StringBuilder out = new StringBuilder();

		out.append(printTable("food_group", "id"));
		out.append(printFoodGroupDetailsTable());

		return out.toString();
	}

	public String printFoodNameTables() {
		StringBuilder out = new StringBuilder();
		out.append(printTable("food_name", "id"));
		out.append(printTable("food_name_details", "id", "loc", "description"));
		return out.toString();
	}

	public String printLocaleTable() {
		return printTable("locale", "id", "acronym", "description");
	}

	public String printFoodTable() {
		return printTable("food", "id", "name_id", "carbohydrate_factor", "fat_factor", "protein_factor", "nitrogen_protein_factor");
	}

	public String printGroupOfFoodTable() {
		return printTable("group_of_food", "group_id", "food_id");
	}

	public String printFoodStateTables() {
		StringBuilder out = new StringBuilder();
		out.append(printTable("food_state", "id"));
		out.append(printTable("food_state_details", "id", "loc", "description"));
		return out.toString();
	}

	public String printStateOfFoodTable() {
		return printTable("state_of_food", "food_id", "state_id", "sequence");
	}
}
