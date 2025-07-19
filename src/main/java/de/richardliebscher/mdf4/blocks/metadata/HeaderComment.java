/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks.metadata;

import de.richardliebscher.mdf4.blocks.metadata.properties.PropertyContainer;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "HDComment")
public class HeaderComment extends ElementBase {
  @XmlElement(name = "TX", required = true)
  private String comment;

  @XmlElement(name = "time_source")
  private String timeSource;

  // TODO: constants and ho:UNIT-SPEC

  @XmlElement(name = "common_properties")
  private PropertyContainer commonProperties;
}
