/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 * Contributors:
 *    Dustin Thomson - initial API and implementation and/or initial documentation
 */

package org.eclipse.paho.client.mqttv3.internal;

/**
 * A high-resolution timer source.
 *
 * Implementations must use clocks that are guaranteed to be monotonic and continue to run
 * even if the CPU enters a low-power state.
 */
public interface HighResolutionTimer {

    /**
     * Returns the current value of a high-resolution time source, in nanoseconds.
     *
     * This method can only be used to measure elapsed time and may return negative values.
     *
     * @return the current value of a high-resolution time source, in nanoseconds.
     */
    public long nanoTime();
}
