/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.custom;

import java.io.*;
import java.util.*;

/**
 * This class defines the data used by our custom data entities, and also provides some methods
 * to facilitate reading/writing data in field selections.
 * 
 * The data generates ID's automatically in the "row_id" field. These ID's correspond to record position
 * in an ArrayList.
 * 
 * Rows are not physically deleted. Instead, the "row_id" of a deleted row is inserted into the {@link #removedIndexes} set.
 * 
 * When reading/writing the data, rows whose "row_id" is in the {@link #removedIndexes} set will be ignored.
 * 
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 *
 */
public class MyData implements Iterable<Map<String, Object>>, Serializable {

	private static final long serialVersionUID = -6576606506512837179L;

	public static final String ROWID = "row_id";

	/**
	 * Each record is a map of field names and their respective values.
	 */
	private final List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();

	/**
	 * This holds the "row_id" of any deleted record.
	 */
	private final Set<Integer> removedIndexes = new HashSet<Integer>();

	/**
	 * Converts the next element in the iterator to an object array. The output values will 
	 * correspond to the order defined by the fieldNames parameter.
	 * 
	 * @param iterator the iterator to records of a {@link MyData} instance
	 * @param fieldNames the sequence of field names whose values should be returned
	 * @return 
	 * 		the values of each selected field name, in the same order of their selection;
	 * 		or null if there are no more entries in the iterator
	 */
	public Object[] toRow(Iterator<Map<String, Object>> iterator, String... fieldNames) {
		if (!iterator.hasNext()) {
			return null;
		}
		Map<String, Object> element = iterator.next();

		return toRow(element, fieldNames);
	}

	/**
	 * Converts a record to an object array. The output values will 
	 * correspond to the order defined by the fieldNames parameter.
	 * 
	 * @param record an individual record of this {@link MyData} instance.
	 * @param fieldNames the sequence of field names whose values should be returned
	 * @return 
	 * 		the values of each selected field name, in the same order of their selection
	 */
	public Object[] toRow(Map<String, Object> record, String... fieldNames) {
		Object[] out = new Object[fieldNames.length];

		for (int i = 0; i < fieldNames.length; i++) {

			String fieldName = fieldNames[i];

			if (fieldName.equals(ROWID)) {
				out[i] = records.indexOf(record);
			} else {
				out[i] = record.get(fieldName);
			}
		}

		return out;
	}

	/**
	 * Identifies whether the values in a selection of fields of a given record match a sequence of values. 
	 * 
	 * @param record the record whose values should be compared.
	 * @param fieldsToMatch the fields to read from the given record
	 * @param matchingValues the values to compare against the ones in this record.
	 * @return true if the values in this record match the criteria; false otherwise
	 */
	public boolean matches(Map<String, Object> record, String[] fieldsToMatch, Object[] matchingValues) {
		//gets the "row_id" of this record
		Integer rowId = records.indexOf(record);
		//if this row has been removed, return false
		if (removedIndexes.contains(rowId)) {
			return false;
		}

		for (int i = 0; i < matchingValues.length; i++) {
			//Gets the next field and value to compare against what the record.
			String fieldName = fieldsToMatch[i];
			Object valueToMatch = matchingValues[i];

			//If we are comparing "row_id" elements, compares the "row_id" of the record (obtained in the first line of this method)
			if (fieldName.equals(ROWID)) {
				String idToMatch = String.valueOf(valueToMatch);
				if (rowId.toString().equals(idToMatch)) {
					continue;
				} else {
					return false;
				}
			}

			//If we are not comparing "row_id" elements, then we are comparing values in the map. 
			//Let's get the value for the field name and compare it to the expected value.
			Object value = record.get(fieldName);
			if (value == null || !value.equals(valueToMatch)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Inserts a new record to the data
	 * @param fieldsToWrite the field names whose values should be written.
	 * @param valuesToWrite sequence of values for each of the given field names.
	 */
	public void insert(String[] fieldsToWrite, Object[] valuesToWrite) {
		Map<String, Object> record = new HashMap<String, Object>();
		setValues(record, fieldsToWrite, valuesToWrite);
		records.add(record);
	}

	/**
	 * Updates the values of a given record. Note "row_id" Can't be updated
	 * 
	 * @param record the record to have its values updated.
	 * @param fieldsToWrite names of the fields to update  
	 * @param valuesToWrite values to set for each given field name
	 */
	public void setValues(Map<String, Object> record, String[] fieldsToWrite, Object[] valuesToWrite) {
		for (int i = 0; i < fieldsToWrite.length; i++) {
			String fieldName = fieldsToWrite[i];

			if (fieldName.equals(ROWID)) {
				throw new IllegalArgumentException("Cannot write '" + valuesToWrite[i] + "' to generated field '" + ROWID + "'");
			}

			Object newValue = valuesToWrite[i];
			record.put(fieldName, newValue);
		}
	}

	/**
	 * Deletes a record. It's "row_id" will be added to the {@link #removedIndexes} set.
	 * @param record the record to delete.
	 */
	public void delete(Map<String, Object> record) {
		int index = records.indexOf(record);
		removedIndexes.add(index);
	}

	/**
	 * Returns an Iterator of records in this {@link MyData} object, bypassing any records whose "row_id" is in the {@link #removedIndexes} set
	 * @return the next valid record.
	 */
	@Override
	public Iterator<Map<String, Object>> iterator() {
		return new Iterator<Map<String, Object>>() {
			int index = 0;

			@Override
			public boolean hasNext() {
				skipRemovedRecords();
				return index < size();
			}

			@Override
			public Map<String, Object> next() {
				skipRemovedRecords();
				return records.get(index++);
			}

			private void skipRemovedRecords() {
				while (removedIndexes.contains(index)) {
					index++;
				}
			}

			@Override
			public void remove() {
				removedIndexes.add(index - 1);
			}
		};
	}

	/**
	 * Returns the number of records in this {@link MyData} object. This number includes removed records.
	 * @return the number of records stored in this object.
	 */
	public int size() {
		return records.size();
	}

	/**
	 * Removes all records in this {@link MyData} object
	 */
	public void clear() {
		records.clear();
		removedIndexes.clear();
	}
}
