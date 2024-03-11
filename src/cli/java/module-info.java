/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

/**
 * CLI.
 */
module mdf4j.cli {
  requires java.logging;
  requires mdf4j;
  requires static lombok;
  requires parquet.column;
  requires parquet.hadoop;
  requires hadoop.common;

  exports de.richardliebscher.mdf4.cli;
}