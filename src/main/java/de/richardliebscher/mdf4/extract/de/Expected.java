/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

/**
 * Provide an explanation of what a {@link Visitor} is expecting.
 */
public interface Expected {

  /**
   * Build an explanation what is being expected.
   *
   * @return explanation
   */
  String expecting();
}
