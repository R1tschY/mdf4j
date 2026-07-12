/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import static java.util.Objects.requireNonNull;

import de.richardliebscher.mdf4.LazyIoList;
import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.TimeStamp;
import de.richardliebscher.mdf4.extract.read.Links;
import de.richardliebscher.mdf4.internal.Pair;
import de.richardliebscher.mdf4.io.ByteInput;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Header/HD-Block.
 */
public final class HeaderBlock implements Block {

  private final Link<DataGroupBlock> firstDataGroup;
  private final Link<FileHistoryBlock> firstFileHistory;
  private final long firstChannelHierarchy;
  private final long firstAttachment;
  private final long firstEventBlock;
  private final Link<Metadata> comment;

  private final TimeStamp startTime;

  private final Value<TimeClass> timeClass;

  private final BitFlags<HeaderFlag> headerFlags;

  private final double startAngleRad;

  private final double startDistanceM;

  private HeaderBlock(HeaderBlock.Builder builder) {
    this.firstDataGroup = builder.firstDataGroup;
    this.firstFileHistory = builder.firstFileHistory;
    this.firstChannelHierarchy = builder.firstChannelHierarchy;
    this.firstAttachment = builder.firstAttachment;
    this.firstEventBlock = builder.firstEventBlock;
    this.comment = builder.comment;
    this.startTime = builder.startTime;
    this.timeClass = builder.timeClass;
    this.headerFlags = builder.headerFlags;
    this.startAngleRad = builder.startAngleRad;
    this.startDistanceM = builder.startDistanceM;
  }

  private HeaderBlock(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parseExpecting(ID, input, 6, 24);
    final var startTime = TimeStamp.parse(input);
    final var timeClass = input.readU8();
    final var flags = input.readU8();
    input.skip(1);
    final var startAngleRad = input.readF64();
    final var startDistanceM = input.readF64();

    final var links = blockHeader.getLinks();
    this.firstDataGroup = Link.of(links[0]);
    this.firstFileHistory = Link.of(links[1]);
    this.firstChannelHierarchy = links[2];
    this.firstAttachment = links[3];
    this.firstEventBlock = links[4];
    this.comment = Link.of(links[5]);
    this.startTime = startTime;
    this.timeClass = Value.of(timeClass, TimeClass.class);
    this.headerFlags = BitFlags.of(flags, HeaderFlag.class);
    this.startAngleRad = startAngleRad;
    this.startDistanceM = startDistanceM;
  }

  public LazyIoList<DataGroupBlock> getDataGroups(ByteInput input) {
    return () -> new DataGroupBlock.Iterator(firstDataGroup, input);
  }

  public LazyIoList<FileHistoryBlock> iterFileHistory(ByteInput input) {
    return () -> new FileHistoryBlock.Iterator(firstFileHistory, input);
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
    return new HeaderBlock(input);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Link<DataGroupBlock> getFirstDataGroup() {
    return this.firstDataGroup;
  }

  public Link<FileHistoryBlock> getFirstFileHistory() {
    return this.firstFileHistory;
  }

  public long getFirstChannelHierarchy() {
    return this.firstChannelHierarchy;
  }

  public long getFirstAttachment() {
    return this.firstAttachment;
  }

  public long getFirstEventBlock() {
    return this.firstEventBlock;
  }

  public Link<Metadata> getComment() {
    return this.comment;
  }

  public double getStartAngleRad() {
    return this.startAngleRad;
  }

  public double getStartDistanceM() {
    return this.startDistanceM;
  }

  public Value<TimeClass> getTimeClass() {
    return this.timeClass;
  }

  public BitFlags<HeaderFlag> getHeaderFlags() {
    return this.headerFlags;
  }

  @Override
  public BlockTypeId typeId() {
    return ID;
  }

  @Override
  public List<Link<?>> links() {
    return List.of(
            firstDataGroup,
            firstFileHistory,
            Link.of(firstChannelHierarchy),
            Link.of(firstAttachment),
            Link.of(firstEventBlock),
            comment
    );
  }

  @Override
  public List<Map.Entry<String, String>> content() {
    return List.of(
            Pair.of("startTime", startTime.toString()),
            Pair.of("timeClass", timeClass.toString()),
            Pair.of("flags", headerFlags.toString()),
            Pair.of("startAngleRad", String.valueOf(startAngleRad)),
            Pair.of("startDistanceM", String.valueOf(startDistanceM))
    );
  }

  @Override
  public Link<Metadata> metadataLink() {
    return comment;
  }

  public static final class Builder {

    private Link<DataGroupBlock> firstDataGroup = Link.nil();
    private Link<FileHistoryBlock> firstFileHistory = Link.nil();
    private long firstChannelHierarchy = 0;
    private long firstAttachment = 0;
    private long firstEventBlock = 0;
    private Link<Metadata> comment = Link.nil();

    private TimeStamp startTime = TimeStamp.empty();

    private Value<TimeClass> timeClass = Value.empty(TimeClass.class);

    private BitFlags<HeaderFlag> headerFlags = BitFlags.empty(HeaderFlag.class);

    private double startAngleRad;

    private double startDistanceM;

    public Builder firstDataGroup(Link<DataGroupBlock> firstDataGroup) {
      this.firstDataGroup = requireNonNull(firstDataGroup);
      return this;
    }

    public Builder firstFileHistory(Link<FileHistoryBlock> firstFileHistory) {
      this.firstFileHistory = requireNonNull(firstFileHistory);
      return this;
    }

    public Builder comment(Link<Metadata> comment) {
      this.comment = requireNonNull(comment);
      return this;
    }

    public Builder startTime(TimeStamp startTime) {
      this.startTime = requireNonNull(startTime);
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
              this);
    }
  }

  public static final Type TYPE = new Type();
  public static final BlockTypeId ID = BlockTypeId.of('H', 'D');

  public static class Type implements BlockType<HeaderBlock> {
    private Type() {
    }

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
