/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.custom;

import java.util.*;

import com.univocity.api.entity.*;
import com.univocity.api.entity.custom.*;

/**
 * A read only entity does not provide data update operations. All it does is expose the available fields in {@link #getFields()} and
 * creating a {@link ReadingProcess} for a selection of fields when uniVocity invokes its {@link #preareToRead(String[])} method.
 * 
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 *
 */
public class MyReadOnlyEntity implements CustomReadableEntity {

	private MyData myData;

	private final String entityName;
	private final String[] fieldNames;

	/**
	 * Creates a new read only custom entity with a given name and a set of fields
	 * @param entityName the name of this read only entity
	 * @param fieldNames the names of all fields available from this entity.
	 */
	public MyReadOnlyEntity(String entityName, String... fieldNames) {
		this.myData = new MyData();
		this.entityName = entityName;
		this.fieldNames = fieldNames;
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public Set<? extends DefaultEntityField> getFields() {
		Set<DefaultEntityField> fields = new HashSet<DefaultEntityField>();

		//"row_id" is the "generated" identifier of this entity.
		DefaultEntityField id = new DefaultEntityField(MyData.ROWID);
		id.setGenerated(true);
		id.setIdentifier(true);
		fields.add(id);

		for (String fieldName : fieldNames) {
			if (!fieldName.equals(MyData.ROWID)) {
				fields.add(new DefaultEntityField(fieldName));
			}
		}

		return fields;
	}

	@Override
	public ReadingProcess preareToRead(final String[] fieldNames) {
		//A reading process is used by uniVocity to extract data from any entity.
		//In the case of our custom entity, we simply iterate over the list of records stored in a "MyData" object 
		return new ReadingProcess() {

			final Iterator<Map<String, Object>> myDataIterator = myData.iterator();

			@Override
			public void close() {
			}

			@Override
			public Object[] readNext() {
				//uniVocity will keep reading rows until this returns null. 
				return myData.toRow(myDataIterator, fieldNames);
			}
		};
	}

	//this is used by the data store so it can serialize the data before a transaction
	MyData getMyData() {
		return myData;
	}

	//this is used by the data store so it can restore the data if a transaction is rolled back.
	void setMyData(MyData myData) {
		this.myData = myData;
	}
}
