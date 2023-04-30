/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import static de.richardliebscher.mdf4.blocks.Consts.HD_BLOCK_OFFSET;

import de.richardliebscher.mdf4.blocks.Header;
import de.richardliebscher.mdf4.blocks.Id;
import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.VersionException;
import de.richardliebscher.mdf4.extract.ChannelSelector;
import de.richardliebscher.mdf4.extract.ExtractPackageGateway;
import de.richardliebscher.mdf4.extract.RecordReader;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.internal.InternalReader;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

/**
 * MDF4 file.
 */
@Log
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Mdf4File {

  /**
   * Supported MDF4 version.
   *
   * <p>Versions above are only partly supported</p>
   */
  public static final MdfFormatVersion TOOL_VERSION = MdfFormatVersion.of(4, 20);

  private final InternalReader inner;

  /**
   * Get HD-Block.
   *
   * @return HD-Block
   */
  public Header getHeader() {
    return inner.getHeader();
  }

  /**
   * Get ID-Block.
   *
   * @return ID-Block
   */
  public Id getId() {
    return inner.getId();
  }

  /**
   * Open MDF4 file.
   *
   * @param input Input file
   * @return Open MDF4 file
   * @throws IOException Failed to read MDF4 header
   */
  public static Mdf4File open(ByteInput input) throws IOException {
    final var idBlock = Id.parse(input);
    if (idBlock.isUnfinalized()) {
      throw new FormatException("MDF file is unfinalized");
    }

    final var formatId = idBlock.getFormatId();
    if (formatId.getMajor() != TOOL_VERSION.getMajor()) {
      throw new VersionException(formatId);
    }

    input.seek(HD_BLOCK_OFFSET);
    final var hdBlock = Header.parse(input);

    log.info("Opened MDF4: Version=" + formatId + " Program=" + idBlock.getProgramId());
    return new Mdf4File(new InternalReader(input, idBlock, hdBlock));
  }

  /**
   * Create a record reader to read channels in a channel group.
   *
   * @param selector      Selector for channel group and channels to read
   * @param recordVisitor Deserializer for records with selected channels
   * @param <R>           Deserialized user-defined record type
   * @return {@link RecordReader}
   * @throws ChannelGroupNotFoundException No channel group selected
   * @throws IOException                   Unable to create record reader
   */
  public <R> RecordReader<R> newRecordReader(ChannelSelector selector,
      RecordVisitor<R> recordVisitor) throws ChannelGroupNotFoundException, IOException {
    return ExtractPackageGateway.newRecordReader(inner, selector, recordVisitor);
  }
}
