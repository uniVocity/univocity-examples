package com.univocity.examples.utils;

import java.util.*;

import javax.sql.*;

import org.springframework.jdbc.core.*;

public interface Database {

	public abstract JdbcTemplate getJdbcTemplate();

	public abstract DataSource getDataSource();

	public abstract Set<String> getTableNames();

}