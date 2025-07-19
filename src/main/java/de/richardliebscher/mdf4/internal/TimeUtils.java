/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import java.time.OffsetDateTime;
import java.time.OffsetTime;

public final class TimeUtils {
  public static OffsetDateTime toDate(java.util.Calendar calendar) {
    return OffsetDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
  }

  public static OffsetDateTime toDateTime(java.util.Calendar calendar) {
    return OffsetDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
  }

  public static OffsetTime toTime(java.util.Calendar calendar) {
    return OffsetTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
  }
}
