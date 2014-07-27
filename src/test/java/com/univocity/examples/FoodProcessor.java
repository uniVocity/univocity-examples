/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples;

import java.util.*;

import com.univocity.api.*;
import com.univocity.api.data.*;

/**
 * In this example we want to populate the particular states of a food (e.g. cooked, raw, sliced) based on its description in the source database. 
 * 
 * A dataset producer will be used to process food names in "FOOD_DES.csv" and split them into food names and states. For example, consider the row:
 *  
 * <p>"Butter, whipped, with salt"
 * 
 * <p> This row will be divided in two parts:
 * <ul>
 *  <li>"Butter": the first string before a comma is the food name</li>
 *  <li>"whipped", "with salt": values after each comma will be a food state</li> 
 * </ul> 
 * 
 * Each row read from the input will be used to generate values to 4 datasets:
 * <ul>
 *  <li>foodNames: all food names processed from the input</li>
 *  <li>foodNameDetails: pairs of food name and food code</li>
 *  <li>foodStateNames: all food states processed from the input</li>
 *  <li>foodStateDetails: rows with food state, food code and the position this food state occurred in the input</li> 
 * </ul>
 * 
 * With this information, we will create mappings to produce data to the following tables:
 * 
 * <ul>
 *  <li>FOOD_NAME - names of foods (without locale dependent information)</li>
 *  <li>FOOD_NAME_DETAILS - descriptions and locale dependent information of each FOOD_NAME</li>
 *  <li>FOOD - food information: name, proteins, energy, etc.</li>
 *  <li>FOOD_STATE - descriptions of states applicable to any food (without locale dependent information), such as "salted", "cooked", "raw", etc</li>
 *  <li>FOOD_STATE_DETAILS - descriptions and locale dependent information of each FOOD_STATE</li>
 *  <li>STATE_OF_FOOD - join table between foods and different food states.</li>
 * </ul>
 * 
 * The destination schema was made complex on purpose so we can explore how uniVocity can be used to handle virtually any schema with ease.
 * We created an Entity-Relationship diagram in "resources/examples/new_schema/diagram.png" for your reference.
 */
public class FoodProcessor extends DatasetProducer {
	private final Set<String> foodNames = new TreeSet<String>();
	private final Set<Object[]> foodNameDetails = new LinkedHashSet<Object[]>();

	private final Set<String> foodStateNames = new TreeSet<String>();
	private final Set<Object[]> foodStateDetails = new LinkedHashSet<Object[]>();

	private int codeIndex;
	private int nameIndex;

	public FoodProcessor() {
		//these are the dataset names this processor creates 
		super("food_names", "food_name_details", "food_state_names", "food_state_details");
	}

	@Override
	public void processStarted() {
		foodNames.clear();
		foodNameDetails.clear();
		foodStateNames.clear();
		foodStateDetails.clear();

		//before starting to read all rows from the input, let's get the indexes of each input field we are interested in:
		codeIndex = getFieldPosition("Ndb_no");
		nameIndex = getFieldPosition("Long_Desc");
	}

	@Override
	public void processNext(Object[] row) {
		String code = String.valueOf(row[codeIndex]);
		String description = String.valueOf(row[nameIndex]);

		//splits the description, trims and lowercases each part in it 
		String[] nameParts = splitFoodDescription(description);

		//the first element is the food name. Whatever comes after is a food state
		String foodName = nameParts[0];

		//here we keep an association between the each name and the code that comes from the input row.
		foodNames.add(foodName);
		foodNameDetails.add(new Object[] { foodName, code });

		//we do the same for each food state
		for (int order = 1; order < nameParts.length; order++) {
			String foodState = nameParts[order];
			foodStateNames.add(foodState);
			foodStateDetails.add(new Object[] { foodState, code, order });
		}
	}

	@Override
	public Dataset getDataset(String name) {
		//returns one of the datasets produced by this class.
		DatasetFactory factory = Univocity.datasetFactory();

		if ("food_names".equals(name)) {
			return factory.newDataset(foodNames, "name");

		} else if ("food_name_details".equals(name)) {
			return factory.newDataset(foodNameDetails, "name", "name", "food_code");

		} else if ("food_state_names".equals(name)) {
			return factory.newDataset(foodStateNames, "name");

		} else if ("food_state_details".equals(name)) {
			return factory.newDataset(foodStateDetails, "name", "name", "food_code", "order");

		} else {
			throw new IllegalArgumentException("Unknown dataset name: " + name);
		}
	}

	public String[] splitFoodDescription(String description) {
		String[] parts = description.toLowerCase().split(",");

		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}

		return parts;
	}
}
