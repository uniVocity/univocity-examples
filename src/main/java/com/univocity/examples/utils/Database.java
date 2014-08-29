/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.utils;

import java.util.*;

import javax.sql.*;

import org.springframework.jdbc.core.*;

public interface Database {

	public abstract JdbcTemplate getJdbcTemplate();

	public abstract DataSource getDataSource();

	public abstract Set<String> getTableNames();

}
