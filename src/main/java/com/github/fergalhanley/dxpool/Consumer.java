package com.github.fergalhanley.dxpool;

@FunctionalInterface
public interface Consumer<T> {
	void consume(T t);
}
