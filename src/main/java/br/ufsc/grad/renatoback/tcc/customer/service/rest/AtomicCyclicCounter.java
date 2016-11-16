package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;

public class AtomicCyclicCounter {
	private final int maxVal;
	private final AtomicInteger counter = new AtomicInteger(0);

	public AtomicCyclicCounter(int maxVal) {
		this.maxVal = maxVal;
	}

	public int cyclicallyIncrementAndGet() {
		return counter.accumulateAndGet(1, new IntBinaryOperator() {
			@Override
			public int applyAsInt(int index, int increment) {
				return ++index >= maxVal ? 0 : index;
			}
		});
	}
}
