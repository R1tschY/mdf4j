/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.BitFlags;
import de.richardliebscher.mdf4.blocks.TimeFlag;
import de.richardliebscher.mdf4.blocks.WriteData;
import de.richardliebscher.mdf4.extract.de.Unsigned;
import de.richardliebscher.mdf4.io.ReadWrite;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.Getter;

/**
 * Time stamp in nanoseconds since midnight Jan 1st, 1970 (UTC time or local time).
 */
public final class TimeStamp implements WriteData {

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
   * Construct empty timestamp.
   *
   * <p>Pointing at midnight Jan 1st, 1970 UTC.
   */
  public static TimeStamp empty() {
    return new TimeStamp(0, 0, 0, BitFlags.empty(TimeFlag.class));
  }

  /**
   * Construct from raw values.
   *
   * @param time              Absolute time in nanoseconds since midnight Jan 1st, 1970.
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
   * Get current timestamp.
   *
   * @return Now as timestamp
   */
  public static TimeStamp now() {
    final var now = Instant.now();
    final var ns = TimeUnit.SECONDS.toNanos(now.getEpochSecond()) + now.getNano();
    final var tz = ZoneId.systemDefault();
    final var rules = tz.getRules();
    final var stdOffset = rules.getStandardOffset(now).getTotalSeconds();
    final var offset = rules.getOffset(now).getTotalSeconds();
    return new TimeStamp(
        ns,
        (int) TimeUnit.SECONDS.toMinutes(stdOffset),
        (int) TimeUnit.SECONDS.toMinutes(offset - stdOffset),
        BitFlags.of(TimeFlag.class, TimeFlag.TIME_OFFSET_VALID));
  }

  /**
   * Get amount of nanoseconds since midnight Jan 1st, 1970 in local time or UTC, depending on
   * {@link #isLocalTime}.
   *
   * @return amount of nanoseconds since midnight Jan 1st, 1970
   */
  public long getNanoseconds() {
    return time;
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

  /**
   * Get time zone offset only resulting from daylight saving.
   *
   * @return Daylight saving offset or {@code Optional.empty()} if local time.
   */
  public Optional<ZoneOffset> getDaylightSavingOffset() {
    if (timeFlags.isSet(TimeFlag.LOCAL_TIME)) {
      return Optional.empty();
    } else {
      return Optional.of(ZoneOffset.ofTotalSeconds((int) TimeUnit.MINUTES.toSeconds(dstOffsetMin)));
    }
  }

  @Override
  public void write(ReadWrite input) throws IOException {
    input.write(time);
    input.write((short) timeZoneOffsetMin);
    input.write((short) dstOffsetMin);
    input.write(timeFlags.asByte());
  }
}
