/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;

@Value
public class DataListBlock<T extends Data<T>> implements DataContainer<T> {

  Link<DataListBlock<T>> nextDataList;
  @ToString.Exclude
  List<Link<DataStorage<T>>> data; // DT,SD,RD,DZ
  LengthOrOffsets offsetInfo;

  BitFlags<DataListFlag> flags;

  public static <T extends Data<T>> DataListBlock<T> parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parseExpecting(ID, input, 1, 8);
    final var links = blockHeader.getLinks();
    final Link<DataListBlock<T>> nextDataList = Link.of(links[0]);
    final List<Link<DataStorage<T>>> data = getDataLinks(links);

    final var flags = BitFlags.of(input.readU8(), DataListFlag.class);
    input.skip(3);
    final var count = input.readI32();
    if (count != data.size()) {
      throw new FormatException(
          "Count attribute in DL block is inconsistent: " + count + " vs. " + data.size());
    }

    final LengthOrOffsets offsetInfo;
    if (flags.isSet(DataListFlag.EQUAL_LENGTH)) {
      offsetInfo = new LengthOrOffsets.Length(input.readI64());
    } else {
      final var offsets = new long[count];
      for (int i = 0; i < count; i++) {
        offsets[i] = input.readI64();
      }
      offsetInfo = new LengthOrOffsets.Offsets(offsets);
    }

    //if (flags.test(DataListFlags.TIME_VALUES)) {
    //  for (int i = 0; i < count; i++) {
    //    final var time_values = input.readQword();
    //  }
    //}
    //
    //if (flags.test(DataListFlags.ANGLE_VALUES)) {
    //  for (int i = 0; i < count; i++) {
    //    final var angle_values = input.readQword();
    //  }
    //}
    //
    //if (flags.test(DataListFlags.DISTANCE_VALUES)) {
    //  for (int i = 0; i < count; i++) {
    //    final var distance_values = input.readQword();
    //  }
    //}

    return new DataListBlock<>(nextDataList, data, offsetInfo, flags);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Data<T>> List<Link<DataStorage<T>>> getDataLinks(long[] links) {
    final var data = (Link<DataStorage<T>>[]) new Link<?>[links.length - 1];
    for (int i = 1; i < links.length; i++) {
      data[i - 1] = Link.of(links[i]);
    }
    return List.of(data);
  }

  public static final Type<?> TYPE = new Type<>();
  public static final Type<DataBlock> DT_TYPE = new Type<>();
  public static final Type<SignalDataBlock> SD_TYPE = new Type<>();
  public static final BlockTypeId ID = BlockTypeId.of('D', 'L');

  @SuppressWarnings("unchecked")
  public static <T extends Data<T>> Type<T> type() {
    return (Type<T>) TYPE;
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type<T extends Data<T>> implements DataContainerType<T, DataListBlock<T>> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public DataListBlock<T> parse(ByteInput input) throws IOException {
      return DataListBlock.parse(input);
    }
  }
}

