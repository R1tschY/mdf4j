/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.BitFlags;
import de.richardliebscher.mdf4.blocks.TimeFlag;
import de.richardliebscher.mdf4.extract.de.Unsigned;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.Getter;

/**
 * Time stamp in nanoseconds since midnight Jan 1st, 1970 (UTC time or local time).
 */
public final class TimeStamp {

  /**
   * Absolute time in nanoseconds since midnight Jan 1st, 1970.
   */
  private final @Unsigned long time;
  /**
   * Time zone offset in minutes.
   */
  private final int timeZoneOffsetMin;
  /**
   * Daylight saving time (DST) offset in minutes.
   */
  private final int dstOffsetMin;

  @Getter
  private final BitFlags<TimeFlag> timeFlags;

  /**
   * Construct from raw values.
   *
   * @param time         Absolute time in nanoseconds since midnight Jan 1st, 1970.
   * @param timeZoneOffsetMin Time zone offset in minutes
   * @param dstOffsetMin      DST offset in minutes
   * @param timeFlags         Time flags
   */
  public TimeStamp(long time, int timeZoneOffsetMin, int dstOffsetMin,
      BitFlags<TimeFlag> timeFlags) {
    this.time = time;
    this.timeZoneOffsetMin = timeZoneOffsetMin;
    this.dstOffsetMin = dstOffsetMin;
    this.timeFlags = timeFlags;
  }

  /**
   * Convert to {@link Instant} if possible.
   *
   * @return Instant, when time is in UTC.
   */
  public Optional<Instant> toInstant() {
    if (timeFlags.isSet(TimeFlag.LOCAL_TIME)) {
      return Optional.empty();
    }

    return Optional.of(Instant.ofEpochSecond(0, time));
  }

  private ZoneOffset getZoneOffset_() {
    if (timeFlags.isSet(TimeFlag.TIME_OFFSET_VALID)) {
      return ZoneOffset.ofTotalSeconds(
          (int) TimeUnit.MINUTES.toSeconds(timeZoneOffsetMin + dstOffsetMin));
    } else {
      return ZoneOffset.UTC;
    }
  }

  /**
   * Convert to {@link OffsetDateTime} if possible.
   *
   * @return Date time, when time is in UTC.
   */
  public Optional<OffsetDateTime> toDateTime() {
    if (timeFlags.isSet(TimeFlag.LOCAL_TIME)) {
      return Optional.empty();
    }

    final var instant = Instant.ofEpochSecond(0, time);
    return Optional.of(instant.atOffset(getZoneOffset_()));
  }

  /**
   * Convert to local date time.
   *
   * @return Local date time.
   */
  public LocalDateTime toLocalDateTime() {
    final var instant = Instant.ofEpochSecond(0, time);
    if (timeFlags.isSet(TimeFlag.LOCAL_TIME)) {
      return LocalDateTime.ofEpochSecond(
          instant.getEpochSecond(), instant.getNano(),
          ZoneOffset.ofTotalSeconds(0));
    }
    return LocalDateTime.ofInstant(instant, getZoneOffset_());
  }

  /**
   * Get time zone offset.
   *
   * @return Local date time.
   */
  public Optional<ZoneOffset> getZoneOffset() {
    if (timeFlags.isSet(TimeFlag.LOCAL_TIME)) {
      return Optional.empty();
    } else {
      return Optional.of(getZoneOffset_());
    }
  }

  /**
   * Return whether time stamp is missing a time zone.
   *
   * @return time stamp is local time
   */
  public boolean isLocalTime() {
    return timeFlags.isSet(TimeFlag.LOCAL_TIME);
  }

  /**
   * Return whether time stamp is in daylight saving time.
   *
   * @return time stamp is in daylight saving time
   */
  public boolean isDaylightSavingTime() {
    return timeFlags.isSet(TimeFlag.TIME_OFFSET_VALID) && dstOffsetMin != 0;
  }
}
