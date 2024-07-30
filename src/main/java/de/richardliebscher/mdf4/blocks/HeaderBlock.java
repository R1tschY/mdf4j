/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.LazyIoList;
import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.TimeStamp;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Header/HD-Block.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class HeaderBlock {

  private final Link<DataGroupBlock> firstDataGroup;
  private final long firstFileHistory;
  private final long firstChannelHierarchy;
  private final long firstAttachment;
  private final long firstEventBlock;
  private final Link<Metadata> comment;

  private final TimeStamp startTime;

  @Getter
  private final Value<TimeClass> timeClass;

  @Getter
  private final BitFlags<HeaderFlag> headerFlags;

  private final double startAngleRad;

  private final double startDistanceM;

  public LazyIoList<DataGroupBlock> getDataGroups(ByteInput input) {
    return () -> new DataGroupBlock.Iterator(firstDataGroup, input);
  }

  public Optional<Metadata> readComment(ByteInput input) throws IOException {
    return comment.resolve(Metadata.TYPE, input);
  }

  public TimeStamp getStartTime() {
    return startTime;
  }

  public Optional<Double> getStartAngle() {
    return headerFlags.isSet(HeaderFlag.START_ANGLE_VALID) ? Optional.of(startAngleRad)
        : Optional.empty();
  }

  public Optional<Double> getStartDistance() {
    return headerFlags.isSet(HeaderFlag.START_DISTANCE_VALID) ? Optional.of(startDistanceM)
        : Optional.empty();
  }

  public static HeaderBlock parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parseExpecting(ID, input, 6, 24);
    final var startTime = input.readI64();
    final var tzOffsetMin = input.readI16();
    final var dstOffsetMin = input.readI16();
    final var timeFlags = input.readU8();
    final var timeClass = input.readU8();
    final var flags = input.readU8();
    input.skip(1);
    final var startAngleRad = input.readF64();
    final var startDistanceM = input.readF64();

    final var links = blockHeader.getLinks();
    return new HeaderBlock(
        Link.of(links[0]),
        links[1],
        links[2],
        links[3],
        links[4],
        Link.of(links[5]),
        new TimeStamp(startTime, tzOffsetMin, dstOffsetMin, BitFlags.of(timeFlags, TimeFlag.class)),
        Value.of(timeClass, TimeClass.class),
        BitFlags.of(flags, HeaderFlag.class),
        startAngleRad,
        startDistanceM
    );
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private Link<DataGroupBlock> firstDataGroup = Link.nil();
    private long firstFileHistory = 0;
    private long firstChannelHierarchy = 0;
    private long firstAttachment = 0;
    private long firstEventBlock = 0;
    private Link<Metadata> comment = Link.nil();

    private TimeStamp startTime = TimeStamp.empty();

    @Getter
    private Value<TimeClass> timeClass = Value.empty(TimeClass.class);

    @Getter
    private BitFlags<HeaderFlag> headerFlags = BitFlags.empty(HeaderFlag.class);

    private double startAngleRad;

    private double startDistanceM;

    public Builder firstDataGroup(@NonNull Link<DataGroupBlock> firstDataGroup) {
      this.firstDataGroup = firstDataGroup;
      return this;
    }

    public Builder comment(@NonNull Link<Metadata> comment) {
      this.comment = comment;
      return this;
    }

    public Builder startTime(@NonNull TimeStamp startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder timeClass(TimeClass timeClass) {
      this.timeClass = timeClass == null ? Value.empty(TimeClass.class) : Value.of(timeClass);
      return this;
    }

    public Builder startAngleRad(double startAngleRad) {
      this.headerFlags = headerFlags.add(HeaderFlag.START_ANGLE_VALID);
      this.startAngleRad = startAngleRad;
      return this;
    }

    public Builder startDistanceM(double startDistanceM) {
      this.headerFlags = headerFlags.add(HeaderFlag.START_DISTANCE_VALID);
      this.startDistanceM = startDistanceM;
      return this;
    }

    public HeaderBlock build() {
      return new HeaderBlock(
          firstDataGroup, firstFileHistory, firstChannelHierarchy, firstAttachment, firstEventBlock,
          comment, startTime, timeClass, headerFlags, startAngleRad, startDistanceM);
    }
  }

  public static final Type TYPE = new Type();
  public static final BlockTypeId ID = BlockTypeId.of('H', 'D');

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type implements BlockType<HeaderBlock> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public HeaderBlock parse(ByteInput input) throws IOException {
      return HeaderBlock.parse(input);
    }
  }
}
