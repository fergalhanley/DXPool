package com.github.fergalhanley.dxpool;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DXPool {

	private static final Map<String, DXPoolImpl> pools = new HashMap<>();

	public static DXPoolImpl create(String name) {
		Objects.requireNonNull(name);

		if(pools.get(name) != null) {
			try {
				throw new Exception(String.format("Pool '%s' already exists", name));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		DXPoolImpl pool = new DXPoolImpl(name);
		pools.put(name, pool);
		return pool;
	}

	public static synchronized DXPoolImpl with(String name) {
		DXPoolImpl dxPoolImpl = pools.get(name);
		if(dxPoolImpl == null) {
			try {
				throw new Exception(String.format("Pool %s does not exist.", name));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dxPoolImpl;
	}
}
