package com.github.fergalhanley.dxpool;

import java.util.Objects;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

public class DXPoolImpl<T> {

	private final String name;

	// options ->
	/**
	 * The maximum number of objects allowed in the pool. When this threshold is reached, consumers will have to wait
	 * for an pre-existing object to become available.
	 */
	private int maxPoolSize = 150;

	/**
	 * The minimum number of objects to maintain in the pool. If the pool is idle for an extended period the number
	 * object instances will eventually settle down to this number.
	 */
	private int minPoolSize = 5;

	/**
	 * The number of new objects to create when the pool needs to grow.
	 */
	private int poolGrowthIncrement = 5;

	/**
	 * The number of objects that are available that will trigger the creating of new object to be added to the pool.
	 */
	private int growthThreshold = 2;

	/**
	 * How long to wait between checking for an available instance in milliseconds
	 */
	private int waitInterval = 100;

	/**
	 * How long to wait for an instance to become available before throwing an exception
	 */
	private int waitTimeout = 10000;

	/**
	 * How long an consumable object instance should remain idle before being retired in milliseconds
	 */
	private long instanceLifeExpectancy = 60000;

	public DXPoolImpl(String name) {
		this.name = name;
	}

	public DXPoolImpl setMaxPoolSize(int maxPoolSize) {
		requireNotInitialized();
		this.maxPoolSize = maxPoolSize;
		return this;
	}

	public DXPoolImpl setMinPoolSize(int minPoolSize) {
		requireNotInitialized();
		this.minPoolSize = minPoolSize;
		return this;
	}

	public DXPoolImpl setPoolGrowthIncrement(int poolGrowthIncrement) {
		requireNotInitialized();
		this.poolGrowthIncrement = poolGrowthIncrement;
		return this;
	}

	public DXPoolImpl setGrowthThreshold(int growthThreshold) {
		requireNotInitialized();
		this.growthThreshold = growthThreshold;
		return this;
	}

	public DXPoolImpl setInstanceLifeExpectancy(long instanceLifeExpectancy) {
		requireNotInitialized();
		this.instanceLifeExpectancy = instanceLifeExpectancy;
		return this;
	}

	public DXPoolImpl setWaitInterval(int waitInterval) {
		requireNotInitialized();
		this.waitInterval = waitInterval;
		return this;
	}

	public DXPoolImpl setWaitTimeout(int waitTimeout) {
		requireNotInitialized();
		this.waitTimeout = waitTimeout;
		return this;
	}

	private void requireNotInitialized() {
		if (this.initializer != null) {
			try {
				throw new Exception("Must set options before 'initialize'");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// <- end options

	private Initializer<T> initializer;
	private Consumer<T> consumeBefore;
	private Consumer<T> consumeAfter;
	private Consumer<T> consumeDestroy;

	private Stack<DXPoolInstance> pool = new Stack<>();
	// instanceCount is maintained separately since unavailable instances need to be kept track of
	private int instanceCount;

	/**
	 * Sets the instance consumer method that executes before running the main execute consumer.
	 *
	 * @param consumer the functional method to execute
	 * @return this (DXPoolImpl) object for chaining
	 */
	public DXPoolImpl<T> before(Class<T> type, Consumer<T> consumer) {
		Objects.requireNonNull(consumer);
		if (this.consumeBefore != null) {
			try {
				throw new Exception("Cannot set multiple 'before' consumers.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.consumeBefore = consumer;
		return this;
	}

	/**
	 * Sets the instance consumer method that executes after running the main execute consumer.
	 *
	 * @param consumer the functional method to execute
	 * @return this (DXPoolImpl) object for chaining
	 */
	public DXPoolImpl<T> after(Class<T> type, Consumer<T> consumer) {
		Objects.requireNonNull(consumer);
		if (this.consumeAfter != null) {
			try {
				throw new Exception("Cannot set multiple 'after' consumers");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.consumeAfter = consumer;
		return this;
	}

	/**
	 * Sets the instance consumer method that executes just before the pool object instance expires and is returned to the pool.
	 *
	 * @param consumer the functional method to execute
	 * @return this (DXPoolImpl) object for chaining
	 */
	public DXPoolImpl<T> destroy(Class<T> type, Consumer<T> consumer) {
		Objects.requireNonNull(consumer);
		if (this.consumeDestroy != null) {
			try {
				throw new Exception("'destroy' instance consumer already set");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.consumeDestroy = consumer;
		return this;
	}

	/**
	 * Sets the initialization method that will be called to create the object to be pooled.
	 *
	 * @param initializer the functional initializer method to exute that will return the instance of the pool object
	 * @return this (DXPoolImpl) object for chaining
	 */
	@SuppressWarnings("unchecked")
	public DXPoolImpl<T> initialize(Class<T> type, Initializer<T> initializer) {
		Objects.requireNonNull(initializer);
		this.initializer = initializer;
		if (pool.size() <= growthThreshold) {
			new Thread(this::allocateInstances).start();
		}

		Timer cleanPoolTimer = new Timer(true);
		cleanPoolTimer.scheduleAtFixedRate(new PoolCleaner(this), 1000, 1000);
		return this;
	}

	/**
	 * Will execute the instance consumer on the current thread
	 *
	 * @param consumer the functional consumer method to be execute and passed the pooled object instance.
	 */
	@SuppressWarnings("unchecked")
	public void execute(Class<T> type, Consumer<T> consumer) {

		if (pool.size() <= growthThreshold) {
			new Thread(this::allocateInstances).start();
		}

		long startedWaiting = System.currentTimeMillis();
		while (pool.size() == 0) {
			try {
				Thread.sleep(waitInterval);
				if (System.currentTimeMillis() - startedWaiting >= waitTimeout) {

					throw new Exception("Timed out waiting for available object on pool: " + name);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		DXPoolInstance instance = pool.pop();
		if (consumeBefore != null) {
			consumeBefore.consume(instance.getConsumable());
		}
		consumer.consume(instance.getConsumable());
		if (consumeAfter != null) {
			consumeAfter.consume(instance.getConsumable());
		}
		pool.push(instance);
	}

	/**
	 * Allocates new objects to be created and added to the pool as needed.
	 */
	@SuppressWarnings("unchecked")
	private void allocateInstances() {
		if (instanceCount >= maxPoolSize) {
			return;
		}
		for (int i = 0; i < poolGrowthIncrement; i++) {
			DXPoolInstance instance = new DXPoolInstance();
			try {
				instance.setConsumable(initializer.initialize());
				pool.push(instance);
				instanceCount++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private class DXPoolInstance {
		T consumable;
		long created;

		public T getConsumable() {
			return consumable;
		}

		public void setConsumable(T consumable) {
			this.consumable = consumable;
		}

		public long getCreated() {
			return created;
		}

		public void setCreated(long created) {
			this.created = created;
		}
	}

	/**
	 * Executes a loop on an interval to remove items from the pool that have outlived their life expectancy
	 */
	private class PoolCleaner extends TimerTask {

		DXPoolImpl dxPool;

		public PoolCleaner(DXPoolImpl dxPool) {
			this.dxPool = dxPool;
		}

		@Override
		public void run() {
			for (int i = pool.size() - 1; i >= minPoolSize; i--) {
				if (System.currentTimeMillis() - pool.get(i).getCreated() >= instanceLifeExpectancy) {
					if (consumeDestroy != null) {
						consumeDestroy.consume(pool.get(i).getConsumable());
					}
					pool.remove(i);
				}
			}
		}
	}
}
