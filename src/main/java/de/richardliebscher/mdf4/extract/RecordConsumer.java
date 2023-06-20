/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import java.io.IOException;

/**
 * Consumer of records.
 *
 * @param <R> Record type
 * @see RecordReader#forEachRemaining
 */
public interface RecordConsumer<R> {

  /**
   * Consume record.
   *
   * @param record Record
   * @throws IOException Any exception
   */
  void consume(R record) throws IOException;
}
