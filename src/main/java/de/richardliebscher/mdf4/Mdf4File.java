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
import de.richardliebscher.mdf4.exceptions.UnsupportedVersionException;
import de.richardliebscher.mdf4.extract.DetachedRecordReader;
import de.richardliebscher.mdf4.extract.RecordFactory;
import de.richardliebscher.mdf4.extract.RecordReader;
import de.richardliebscher.mdf4.extract.SerializableRecordFactory;
import de.richardliebscher.mdf4.extract.SizedRecordReader;
import de.richardliebscher.mdf4.extract.impl.RecordReaderFactory;
import de.richardliebscher.mdf4.internal.FileContext;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FileInput;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.stream.XMLInputFactory;
import lombok.extern.java.Log;

/**
 * MDF4 file.
 */
@Log
public class Mdf4File {

  /**
   * Supported MDF4 version.
   *
   * <p>Versions above are only partly supported</p>
   */
  public static final MdfFormatVersion TOOL_VERSION = MdfFormatVersion.of(4, 20);

  private final Id id;
  private final Header header;
  private final FileContext ctx;

  Mdf4File(Id id, Header header, ByteInput input) {
    this.id = id;
    this.header = header;
    this.ctx = new FileContext(input, null, XMLInputFactory.newDefaultFactory());
  }

  /**
   * Get HD-Block.
   *
   * @return HD-Block
   */
  public Header getHeader() {
    return header;
  }

  /**
   * Get ID-Block.
   *
   * @return ID-Block
   */
  public Id getId() {
    return id;
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
      throw new UnsupportedVersionException(formatId);
    }

    input.seek(HD_BLOCK_OFFSET);
    final var hdBlock = Header.parse(input);

    //log.info("Opened MDF4: Version=" + formatId + " Program=" + idBlock.getProgramId());
    return new Mdf4File(idBlock, hdBlock, input);
  }

  /**
   * Open MDF4 file.
   *
   * @param input Input file
   * @return Open MDF4 file
   * @throws IOException Failed to read MDF4 header
   */
  public static Mdf4File open(Path input) throws IOException {
    return Mdf4File.open(new FileInput(input));
  }

  /**
   * Create a record reader to read channels in a channel group.
   *
   * @param factory Factory for records
   * @param <R>     Deserialized user-defined record type
   * @return Reader for deserialized records
   * @throws ChannelGroupNotFoundException No channel group selected
   * @throws IOException                   Unable to create record reader
   */
  public <B, R> SizedRecordReader<B, R> newRecordReader(
      RecordFactory<B, R> factory) throws ChannelGroupNotFoundException, IOException {
    return RecordReaderFactory.createFor(ctx, getDataGroups(), factory);
  }

  /**
   * Create iterator for all data groups.
   *
   * @return Newly created iterator
   */
  public LazyIoList<DataGroup> getDataGroups() {
    return () -> new DataGroup.Iterator(getHeader().getFirstDataGroup(), ctx);
  }

  /**
   * Stream channel values from a channel group.
   *
   * @param factory Factory for records
   * @param <R>     Deserialized user-defined record type
   * @return Stream of deserialized records
   * @throws ChannelGroupNotFoundException No channel group selected
   * @throws IOException                   Unable to create record reader
   */
  public <B, R> Stream<Result<R, IOException>> streamRecords(
      SerializableRecordFactory<B, R> factory) throws ChannelGroupNotFoundException, IOException {
    return RecordReaderFactory
        .createParallelFor(ctx, getDataGroups(), factory)
        .stream();
  }

  /**
   * Create detached record readers for distributed reading.
   *
   * <p>Useful for use in distributed compute engines like Apache Spark. The returned record
   * readers are serializable and can be sent to compute nodes to attach them to the file again and
   * start reading records.
   *
   * @param parts   number of parts to split to
   * @param factory Factory for records
   * @param <R>     Deserialized user-defined record type
   * @return Detached
   * @throws ChannelGroupNotFoundException No channel group selected
   * @throws IOException                   Unable to create record reader
   * @see #attachRecordReader
   */
  public <B, R> List<DetachedRecordReader<B, R>> splitRecordReaders(int parts,
      SerializableRecordFactory<B, R> factory)
      throws ChannelGroupNotFoundException, IOException {
    return RecordReaderFactory
        .createParallelFor(ctx, getDataGroups(), factory)
        .splitIntoDetached(parts);
  }

  /**
   * Attach a detached record reader from {@link #splitRecordReaders}.
   *
   * <p>The attached file must have the exact same content as the file on which
   * {@link #splitRecordReaders} was called.
   *
   * @param reader Reader from {@link #splitRecordReaders} call
   * @param <R>    Deserialized user-defined record type
   * @return Reader for deserialized records
   */
  public <B, R> RecordReader<B, R> attachRecordReader(DetachedRecordReader<B, R> reader) {
    return reader.attach(ctx);
  }
}
