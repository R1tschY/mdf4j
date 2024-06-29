/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import java.io.IOException;

/**
 * A predicate on a channel/data group.
 */
@FunctionalInterface
public interface GroupPredicate {

  /**
   * Evaluates predicate on channel/data group pair.
   *
   * @param dataGroup    Data group
   * @param channelGroup Channel group
   * @return {@code true}, iff channel/data group matches predicate.
   * @throws IOException Failed to evaluate predicate
   */
  boolean test(DataGroup dataGroup, ChannelGroup channelGroup) throws IOException;
}
