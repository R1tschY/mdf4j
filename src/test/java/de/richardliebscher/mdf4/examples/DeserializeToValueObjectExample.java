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


public class DeserializeToValueObjectExample {

  private static class Record {

    public int signal1;
    public int signal2;

    @Override
    public String toString() {
      return "Record{" +
          "signal1=" + signal1 +
          ", signal2=" + signal2 +
          '}';
    }
  }

  public static void main(String[] args) throws Exception {
    final var source = Path.of(args[0]);

    // Open file
    final var mdf4File = Mdf4File.open(source);

    final var reader = mdf4File.newRecordReader(new RecordFactory<Record, Record>() {
      @Override
      public boolean selectGroup(DataGroup dg, ChannelGroup group) {
        // Select first channel group
        return true;
      }

      @Override
      public DeserializeInto<Record> selectChannel(DataGroup dg, ChannelGroup group,
          Channel channel)
          throws IOException {
        switch (channel.getName()) {
          case "signal1":
            return (deserializer, dest) -> dest.signal1 = (Integer) new ObjectDeserialize().deserialize(
                deserializer);
          case "signal2":
            return (deserializer, dest) -> dest.signal2 = (Integer) new ObjectDeserialize().deserialize(
                deserializer);
          default:
            return null;
        }
      }

      @Override
      public Record createRecordBuilder() {
        return new Record();
      }

      @Override
      public Record finishRecord(Record record) {
        return record;
      }
    });

    // Write values
    for (int i = 0; i < reader.size(); i++) {
      System.out.println(reader.next());
    }
  }
}