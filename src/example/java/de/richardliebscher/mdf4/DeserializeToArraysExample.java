/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.extract.de.ObjectDeserialize;
import java.nio.file.Path;
import java.util.List;


public class DeserializeToArraysExample {

  private static class Records {

    public List<Integer> signal1;
    public List<Integer> signal2;

    @Override
    public String toString() {
      return "Records{"
          + "signal1=" + signal1
          + ", signal2=" + signal2
          + '}';
    }
  }

  public static void main(String[] args) throws Exception {
    final var source = Path.of(args[0]);

    // Open file
    try (var mdf4File = Mdf4File.open(source)) {
      final var records = new Records();

      final var reader = mdf4File.newRecordReader(
          0,
          (dg, group, channel) -> {
            switch (channel.getName()) {
              case "signal1":
                return (deserializer, dest) -> dest.signal1.add(
                    (Integer) new ObjectDeserialize().deserialize(deserializer));
              case "signal2":
                return (deserializer, dest) -> dest.signal2.add(
                    (Integer) new ObjectDeserialize().deserialize(deserializer));
              default:
                return null;
            }
          },
          () -> records
      );

      // Write values
      for (int i = 0; i < reader.size(); i++) {
        reader.nextInto(records);
      }

      System.out.println(records);
    }
  }
}