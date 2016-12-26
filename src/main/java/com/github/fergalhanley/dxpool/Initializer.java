package com.github.fergalhanley.dxpool;

import java.sql.SQLException;

@FunctionalInterface
public interface Initializer<T> {
	T initialize() throws SQLException, InterruptedException;
}
