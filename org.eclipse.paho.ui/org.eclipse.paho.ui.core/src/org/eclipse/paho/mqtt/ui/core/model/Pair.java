/*******************************************************************************
 * Copyright (c) 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Bin Zhang - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.paho.mqtt.ui.core.model;

/**
 * A pair that holds two values
 * 
 * @param <L> The type of the first value held by this pair
 * @param <R> The type of the second value held by this pair
 * 
 * @author Bin Zhang
 */
public final class Pair<L, R> {
	private final L left;
	private final R right;

	/**
	 * @param left
	 * @param right
	 */
	public static <L, R> Pair<L, R> of(L left, R right) {
		return new Pair<L, R>(left, right);
	}

	/**
	 * @param left
	 * @param right
	 */
	Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public L getLeft() {
		return left;
	}

	public R getRight() {
		return right;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getClass().getSimpleName()).append(" [").append("left=").append(left)
				.append(",").append("right=").append(right).append("]").toString();
	}

}
