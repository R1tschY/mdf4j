/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.examples;

import de.richardliebscher.mdf4.Channel;
import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.Mdf4File;
import de.richardliebscher.mdf4.extract.RecordFactory;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.ObjectDeserialize;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;


public class DeserializeToArraysExample {

  private static class Records {

    public List<Integer> signal1;
    public List<Integer> signal2;

    @Override
    public String toString() {
      return "Records{" +
          "signal1=" + signal1 +
          ", signal2=" + signal2 +
          '}';
    }
  }

  public static void main(String[] args) throws Exception {
    final var source = Path.of(args[0]);

    // Open file
    try (var mdf4File = Mdf4File.open(source)) {

      final var records = new Records();

      final var reader = mdf4File.newRecordReader(new RecordFactory<Records, Void>() {
        @Override
        public boolean selectGroup(DataGroup dg, ChannelGroup group) {
          // Select first channel group
          return true;
        }

        @Override
        public DeserializeInto<Records> selectChannel(DataGroup dg, ChannelGroup group,
            Channel channel)
            throws IOException {
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
        }

        @Override
        public Records createRecordBuilder() {
          return records;
        }

        @Override
        public Void finishRecord(Records record) {
          return null;
        }
      });

      // Write values
      for (int i = 0; i < reader.size(); i++) {
        reader.nextInto(records);
      }

      System.out.println(records);
    }
  }
}