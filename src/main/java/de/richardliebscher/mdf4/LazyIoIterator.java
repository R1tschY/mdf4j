/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import java.io.IOException;
import java.util.Iterator;

/**
 * Iterator that throws {@link IOException}s.
 *
 * @param <T> Item type
 */
public interface LazyIoIterator<T> {

  /**
   * Return {@code true} iff iterator has more elements.
   *
   * @return {@code true} iff iterator has more elements
   * @see Iterator#hasNext()
   */
  boolean hasNext();

  /**
   * Returns next element or {@code null} iff no more elements exist.
   *
   * @return Next element or {@code null} iff no more elements exist
   * @throws IOException Failed to load next item
   */
  T next() throws IOException;
}
