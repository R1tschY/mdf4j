/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.internal.FileContext;
import java.io.IOException;
import java.io.Serializable;

/**
 * Serializable record reader fpr transfer to other computers.
 *
 * @param <R> Deserialized record type
 */
public interface DetachedRecordReader<B, R> extends Serializable {

  /**
   * NOT INTENDED FOR PUBLIC USE.
   */
  @SuppressWarnings("ClassEscapesDefinedScope")
  RecordReader<B, R> attach(FileContext ctx) throws IOException;
}
