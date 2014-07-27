/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.custom;

import java.util.*;

import com.univocity.api.entity.custom.*;

/**
 * This custom entity extends the {@link MyReadOnlyEntity} to provide methods for data modification.
 *
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 *
 */
public class MyEntity extends MyReadOnlyEntity implements CustomDataEntity {

	private final MyDataStore dataStore;

	/**
	 * Creates a new instance of a custom data entity, with a given set of field names
	 *
	 * @param dataStore the data store that contains this entity (we need it to manage transactions)
	 * @param entityName the name of the new custom data entity
	 * @param fieldNames the fields in this entity.
	 */
	public MyEntity(MyDataStore dataStore, String entityName, String[] fieldNames) {
		super(entityName, fieldNames);
		this.dataStore = dataStore;
	}

	@Override
	public WritingProcess prepareToWrite(final String[] fieldNames) {
		//Before writing to this entity, we ask the data store to persist the original information
		dataStore.saveMyDataBeforeModifying(this);

		//With the data safe and secure, uniVocity can execute a writing process.
		return new WritingProcess() {

			//This returns the last "row_id". We new row added to myData will
			//"generate" a new id that corresponds to its position in a list.
			final int startIndex = getMyData().size();

			@Override
			public void close() {
			}

			@Override
			public void writeNext(Object[] data) {
				//writes a row provided by uniVocity
				getMyData().insert(fieldNames, data);
			}

			@Override
			public ReadingProcess retrieveGeneratedKeys() {
				//after all rows were written, uniVocity will request for generated keys (if any)
				//we will simply return the indexes of each record added by the writing process
				return new ReadingProcess() {

					int index = startIndex; //starts with the last index before writing anything
					int endIndex = getMyData().size(); //ends with the current size of myData

					@Override
					public void close() {
					}

					@Override
					public Object[] readNext() {
						if (index < endIndex) {
							//returns the next "generated key"
							return new Object[] { index++ };
						} else {
							//if this reading process returned all indexes already, then return null
							//to notify uniVocity there's no more generated keys.
							return null;

						}
					}
				};
			}
		};
	}

	@Override
	public UpdateProcess prepareToUpdate(final String[] fieldsToUpdate, final String[] fieldsToMatch) {
		//Before writing to this entity, ask the data store to persist the original information
		dataStore.saveMyDataBeforeModifying(this);

		return new UpdateProcess() {

			@Override
			public void close() {
			}

			@Override
			public void updateNext(Object[] updatedValues, Object[] matchingValues) {
				//to perform an update on myData, we iterate over each record and
				//compare their values against the ones in the matchingValues parameter.
				for (Map<String, Object> record : getMyData()) {
					if (getMyData().matches(record, fieldsToMatch, matchingValues)) {
						//once a match is found, the record can be updated using the values in the updatedValues parameter.
						getMyData().setValues(record, fieldsToUpdate, updatedValues);
					}
				}
			}
		};
	}

	@Override
	public ExclusionProcess prepareToDelete(final String[] fieldsToMatch) {
		//Before removing data from this entity, ask the data store to persist the original information.
		dataStore.saveMyDataBeforeModifying(this);
		return new ExclusionProcess() {

			@Override
			public void close() {
			}

			@Override
			public void deleteNext(Object[] matchingValues) {
				Iterator<Map<String, Object>> myIterator = getMyData().iterator();
				//to perform an exclusion on myData, we iterate over each record and
				//compare their values against the ones in the matchingValues parameter.
				while (myIterator.hasNext()) {
					Map<String, Object> record = myIterator.next();
					//If a match is found, the record is deleted.
					if (getMyData().matches(record, fieldsToMatch, matchingValues)) {
						myIterator.remove();
					}
				}
			}
		};
	}

	@Override
	public void deleteAll() {
		//Before removing data from this entity, ask the data store to persist the original information.
		dataStore.saveMyDataBeforeModifying(this);
		getMyData().clear();
	}
}
