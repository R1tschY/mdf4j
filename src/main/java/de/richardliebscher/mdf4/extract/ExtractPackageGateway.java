/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.internal.InternalReader;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Allow inner-module access to package-private methods.
 * <p>
 * Needed to exclude public but module-intern methods from Javadoc.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExtractPackageGateway {

  public static <R> RecordReader<R> newRecordReader(InternalReader reader, ChannelSelector selector,
      RecordVisitor<R> rowDeserializer) throws ChannelGroupNotFoundException, IOException {
    return RecordReader.createFor(reader, selector, rowDeserializer);
  }
}
