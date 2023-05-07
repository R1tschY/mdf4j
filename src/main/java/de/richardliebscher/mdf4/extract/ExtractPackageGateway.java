/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.extract.de.SerializableRecordVisitor;
import de.richardliebscher.mdf4.internal.FileContext;
import java.io.IOException;
import java.util.Iterator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Allow inner-module access to package-private methods.
 * <p>
 * Needed to exclude public but module-intern methods from Javadoc.
 * </p>
 */
@SuppressWarnings("ClassEscapesDefinedScope")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExtractPackageGateway {

  public static <R> RecordReader<R> newRecordReader(
      FileContext ctx, Iterator<DataGroup> dataGroups, ChannelSelector selector,
      RecordVisitor<R> rowDeserializer) throws ChannelGroupNotFoundException, IOException {
    return RecordReaderFactory.createFor(ctx, dataGroups, selector, rowDeserializer);
  }

  public static <R> ParallelRecordReader<R> newParallelRecordReader(
      FileContext ctx, Iterator<DataGroup> dataGroups, ChannelSelector selector,
      SerializableRecordVisitor<R> rowDeserializer)
      throws ChannelGroupNotFoundException, IOException {
    return RecordReaderFactory.createParallelFor(ctx, dataGroups, selector, rowDeserializer);
  }
}
