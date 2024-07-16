/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.HeaderBlock;
import de.richardliebscher.mdf4.blocks.IdBlock;
import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.UnsupportedVersionException;
import de.richardliebscher.mdf4.extract.ChannelDeFactory;
import de.richardliebscher.mdf4.extract.DetachedRecordReader;
import de.richardliebscher.mdf4.extract.GroupPredicate;
import de.richardliebscher.mdf4.extract.RecordFactory;
import de.richardliebscher.mdf4.extract.RecordReader;
import de.richardliebscher.mdf4.extract.SerializableRecordFactory;
import de.richardliebscher.mdf4.extract.SizedRecordReader;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.impl.RecordReaderFactory;
import de.richardliebscher.mdf4.internal.FileContext;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FileInput;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.xml.stream.XMLInputFactory;
import lombok.NonNull;
import lombok.extern.java.Log;

/**
 * MDF4 file.
 */
@Log
public class Mdf4File implements Closeable {

  /**
   * Supported MDF4 version.
   *
   * <p>Versions above are only partly supported</p>
   */
  public static final MdfFormatVersion TOOL_VERSION = MdfFormatVersion.of(4, 20);

  private final IdBlock idBlock;
  private final HeaderBlock headerBlock;
  private final FileContext ctx;

  Mdf4File(IdBlock idBlock, HeaderBlock headerBlock, ByteInput input) {
    this.idBlock = idBlock;
    this.headerBlock = headerBlock;
    this.ctx = new FileContext(input, null, XMLInputFactory.newDefaultFactory());
  }

  /**
   * Get HD-Block.
   *
   * @return HD-Block
   */
  public HeaderBlock getHeader() {
    return headerBlock;
  }

  /**
   * Get ID-Block.
   *
   * @return ID-Block
   */
  public IdBlock getId() {
    return idBlock;
  }

  /**
   * Open MDF4 file.
   *
   * @param input Input file
   * @return Open MDF4 file
   * @throws IOException Failed to read MDF4 header
   */
  public static Mdf4File open(ByteInput input) throws IOException {
    try {
      final var idBlock = IdBlock.parse(input);
      if (idBlock.isUnfinalized()) {
        throw new FormatException("MDF file is unfinalized");
      }

      final var formatId = idBlock.getFormatId();
      if (formatId.getMajor() != TOOL_VERSION.getMajor()) {
        throw new UnsupportedVersionException(formatId);
      }

      final var hdBlock = HeaderBlock.parse(input);

      //log.info("Opened MDF4: Version=" + formatId + " Program=" + idBlock.getProgramId());
      return new Mdf4File(idBlock, hdBlock, input);
    } catch (Throwable throwable) {
      input.close();
      throw throwable;
    }
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
   * Read measurement file comment.
   *
   * @return measurement file comment, iff it exists
   * @throws IOException Failed to read comment
   */
  public Optional<String> readComment() throws IOException {
    return ctx.readText(headerBlock.getComment(), "HDcomment");
  }

  /**
   * Get measurement start time.
   *
   * @return measurement start time
   */
  public TimeStamp getStartTime() {
    return headerBlock.getStartTime();
  }

  /**
   * Get start angle.
   *
   * @return start distance, iff it exists
   */
  public Optional<Double> getStartAngle() {
    return headerBlock.getStartAngle();
  }

  /**
   * Get start distance.
   *
   * @return start distance, iff it exists
   */
  public Optional<Double> getStartDistance() {
    return headerBlock.getStartDistance();
  }

  /**
   * Create a record reader to read channels in a channel group.
   *
   * @param factory Factory for records
   * @param <B>     Builder for user-defined record type
   * @param <R>     Deserialized user-defined record type
   * @return Reader for deserialized records
   * @throws ChannelGroupNotFoundException No channel group selected
   * @throws IOException                   Unable to create record reader
   * @deprecated Please use {@link #newRecordReader(GroupPredicate, ChannelDeFactory, Supplier)}
   */
  @Deprecated(since = "0.3", forRemoval = true)
  public <B, R> SizedRecordReader<B, R> newRecordReader(
      RecordFactory<B, R> factory) throws ChannelGroupNotFoundException, IOException {
    return RecordReaderFactory.createFor(ctx, getDataGroups(), factory);
  }

  /**
   * Create a record reader to read channels in a channel group.
   *
   * @param predicate     Predicate to select channel group to read
   * @param deFactory     Factory for create channel deserializations
   * @param recordFactory Factory to create records in which deserialization writes
   * @param <R>           Deserialized user-defined record type
   * @return Reader for deserialized records
   * @throws ChannelGroupNotFoundException No channel group selected
   * @throws IOException                   Unable to create record reader
   */
  public <R> SizedRecordReader<R, R> newRecordReader(
      GroupPredicate predicate,
      @NonNull ChannelDeFactory<R> deFactory,
      @NonNull Supplier<R> recordFactory)
      throws ChannelGroupNotFoundException, IOException {
    return RecordReaderFactory.createFor(ctx, getDataGroups(), new RecordFactory<>() {
      @Override
      public boolean selectGroup(DataGroup dataGroup, ChannelGroup group) throws IOException {
        return predicate.test(dataGroup, group);
      }

      @Override
      public DeserializeInto<R> selectChannel(DataGroup dataGroup, ChannelGroup group,
          Channel channel) throws IOException {
        return deFactory.createDeserialization(dataGroup, group, channel);
      }

      @Override
      public R createRecordBuilder() {
        return recordFactory.get();
      }

      @Override
      public R finishRecord(R unfinishedRecord) {
        return unfinishedRecord;
      }
    });
  }

  /**
   * Create a record reader to read channels in a channel group.
   *
   * @param channelGroupName Name of channel group to read
   * @param deFactory        Factory for create channel deserializations
   * @param recordFactory    Factory to create records in which deserialization writes
   * @param <R>              Deserialized user-defined record type
   * @return Reader for deserialized records
   * @throws ChannelGroupNotFoundException No channel group selected
   * @throws IOException                   Unable to create record reader
   */
  public <R> SizedRecordReader<R, R> newRecordReader(
      @NonNull String channelGroupName, ChannelDeFactory<R> deFactory, Supplier<R> recordFactory)
      throws ChannelGroupNotFoundException, IOException {
    return newRecordReader(
        (dg, cg) -> channelGroupName.equals(cg.getName().orElse(null)),
        deFactory, recordFactory);
  }

  /**
   * Create a record reader to read channels in a channel group.
   *
   * @param channelGroupIndex Index of channel group to read
   * @param deFactory         Factory for create channel deserializations
   * @param recordFactory     Factory to create records in which deserialization writes
   * @param <R>               Deserialized user-defined record type
   * @return Reader for deserialized records
   * @throws ChannelGroupNotFoundException No channel group selected
   * @throws IOException                   Unable to create record reader
   */
  public <R> SizedRecordReader<R, R> newRecordReader(
      int channelGroupIndex, ChannelDeFactory<R> deFactory, Supplier<R> recordFactory)
      throws ChannelGroupNotFoundException, IOException {
    return newRecordReader(
        new GroupPredicate() {
          private int index = 0;

          @Override
          public boolean test(DataGroup dataGroup, ChannelGroup channelGroup) {
            return channelGroupIndex == ++index;
          }
        },
        deFactory, recordFactory);
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
  public <B, R> RecordReader<B, R> attachRecordReader(DetachedRecordReader<B, R> reader)
      throws IOException {
    return reader.attach(ctx);
  }

  @Override
  public void close() throws IOException {
    ctx.close();
  }
}
