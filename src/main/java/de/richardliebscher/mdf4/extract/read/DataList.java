/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.blocks.BlockType;
import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataContainer;
import de.richardliebscher.mdf4.blocks.DataListBlock;
import de.richardliebscher.mdf4.blocks.DataStorage;
import de.richardliebscher.mdf4.blocks.HeaderListBlock;
import de.richardliebscher.mdf4.blocks.Offsets;
import de.richardliebscher.mdf4.blocks.Offsets.EqualLength;
import de.richardliebscher.mdf4.blocks.Offsets.Values;
import de.richardliebscher.mdf4.blocks.Offsets.Visitor;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;
import lombok.Getter;

@Getter
public class DataList<T extends Data<T>> implements Serializable {

  private final Links<DataStorage<T>> dataBlocks;
  private final Offsets offsets;

  public DataList(Links<DataStorage<T>> dataBlocks, Offsets offsets) {
    if (dataBlocks.size() != offsets.size()) {
      throw new IllegalArgumentException("dataBlocks and offsets must have equal size");
    }

    this.dataBlocks = dataBlocks;
    this.offsets = offsets;
  }

  public static <T extends Data<T>> DataList<T> from(
      Link<DataContainer<T>> dataRoot,
      BlockType<DataContainer<T>> containerBlockType,
      ByteInput input) throws IOException {
    final var rootBlock = dataRoot.resolve(containerBlockType, input).orElse(null);

    if (rootBlock == null) {
      return empty();
    } else if (rootBlock instanceof DataStorage) {
      return single((Link<DataStorage<T>>) (Link<?>) dataRoot);
    } else if (rootBlock instanceof DataListBlock) {
      return fromList((DataListBlock<T>) rootBlock, input);
    } else if (rootBlock instanceof HeaderListBlock) {
      final var headerList = (HeaderListBlock<T>) rootBlock;
      final var dataList = headerList.getFirstDataList().resolve(DataListBlock.type(), input)
          .orElse(null);
      if (dataList != null) {
        return fromList(dataList, input);
      } else {
        return empty();
      }
    } else {
      throw new IllegalStateException("Should not happen!");
    }
  }

  private static <T extends Data<T>> DataList<T> empty() throws FormatException {
    return new DataList<>(new Links<>(List.of()), new Values(new long[0]));
  }

  private static <T extends Data<T>> DataList<T> single(Link<DataStorage<T>> dataStorage)
      throws FormatException {
    return new DataList<>(new Links<>(List.of(dataStorage)), new Values(new long[]{0L}));
  }

  private static <T extends Data<T>> DataList<T> fromList(
      DataListBlock<T> dataList, ByteInput input) throws IOException {
    var resultLinks = new ArrayList<Link<DataStorage<T>>>();
    var resultOffsets = (Offsets) null;

    do {
      resultOffsets = mergeOffsets(resultOffsets, dataList.getOffsets());
      resultLinks.addAll(dataList.getData());

      dataList = dataList.getNextDataList().resolve(DataListBlock.type(), input).orElse(null);
    } while (dataList != null);

    return new DataList<>(new Links<>(resultLinks), resultOffsets);
  }

  private static Offsets mergeOffsets(Offsets a, Offsets b) throws IOException {
    if (a == null) {
      return b;
    }

    return a.accept(new Visitor<Offsets, IOException>() {
      @Override
      public Offsets visitLength(int number1, long length1) throws IOException {
        return b.accept(new Visitor<Offsets, IOException>() {
          @Override
          public Offsets visitLength(int number2, long length2) throws IOException {
            if (length1 != length2) {
              throw new FormatException("Lengths of equal length data list differ");
            }
            return new EqualLength(number1 + number2, length2);
          }

          @Override
          public Offsets visitOffsets(long[] offsets) throws IOException {
            throw new FormatException("Mix of equal length and non-equal length data lists");
          }
        });
      }

      @Override
      public Offsets visitOffsets(long[] offsets1) throws IOException {
        return b.accept(new Visitor<Offsets, IOException>() {
          @Override
          public Offsets visitLength(int number, long length) throws IOException {
            throw new FormatException("Mix of equal length and non-equal length data lists");
          }

          @Override
          public Offsets visitOffsets(long[] offsets2) throws IOException {
            return new Values(LongStream.concat(
                Arrays.stream(offsets1),
                Arrays.stream(offsets2)).toArray());
          }
        });
      }
    });
  }
}
