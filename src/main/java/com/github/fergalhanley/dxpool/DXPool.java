package com.github.fergalhanley.dxpool;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DXPool {

	private static final Map<String, DXPoolImpl> pools = new HashMap<>();

	public static DXPoolImpl create(String name) throws Exception {
		Objects.requireNonNull(name);

		if(pools.get(name) != null) {
			throw new Exception(String.format("Pool '%s' already exists", name));
		}
		DXPoolImpl pool = new DXPoolImpl(name);
		pools.put(name, pool);
		return pool;
	}

	public static synchronized DXPoolImpl with(String name) throws Exception {
		DXPoolImpl dxPoolImpl = pools.get(name);
		if(dxPoolImpl == null) {
			throw new Exception(String.format("Pool %s does not exist.", name));
		}
		return dxPoolImpl;
	}
}
