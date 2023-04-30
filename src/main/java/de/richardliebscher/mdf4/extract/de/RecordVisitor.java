/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

/**
 * Visitor to deserialize record.
 *
 * @param <T> Target type
 */
public interface RecordVisitor<T> {

  /**
   * Visit record.
   *
   * @param recordAccess Interface to access record
   * @return Deserialized record
   * @throws IOException Unable to deserialize
   */
  T visitRecord(RecordAccess recordAccess) throws IOException;
}
