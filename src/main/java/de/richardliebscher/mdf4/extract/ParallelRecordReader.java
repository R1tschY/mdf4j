/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.Result;
import java.io.IOException;
import java.util.List;
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
  Stream<Result<R, IOException>> stream();

  List<DetachedRecordReader<R>> splitIntoDetached(int parts);
}
