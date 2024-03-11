/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.cli.parquet;

import java.io.IOException;
import org.apache.parquet.io.api.RecordConsumer;

public interface Record {

  void writeInto(RecordConsumer recordConsumer) throws IOException;
}
