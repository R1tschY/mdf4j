/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import java.util.stream.Stream;

/**
 * Reading records in parallel.
 *
 * @param <R> Deserialized user-defined record type
 */
public interface ParallelRecordReader<R> {
  /**
   * Create splittable stream.
   *
   * @return Splittable stream
   */
  Stream<R> stream();
}
