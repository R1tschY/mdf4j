/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.Channel;
import java.util.List;

/**
 * A record reader with a known number of records.
 *
 * @param <R> Deserialized record type
 */
public interface SizedRecordReader<B, R> extends RecordReader<B, R> {

  /**
   * Get list of channels this instance is reading from.
   *
   * @return List of channels
   */
  List<Channel> getChannels();

  /**
   * Get number of records.
   *
   * @return number of records
   */
  long size();

  /**
   * Get number of remaining records.
   *
   * @return number of remaining records
   */
  long remaining();

}
