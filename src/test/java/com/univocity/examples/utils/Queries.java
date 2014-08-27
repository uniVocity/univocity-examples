package com.univocity.examples.utils;

import java.util.*;

public interface Queries {

	public abstract Set<String> getQueryNames();

	public abstract String getQuery(String name);

}