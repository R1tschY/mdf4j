/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

@XmlSchema(
    elementFormDefault = XmlNsForm.QUALIFIED,
    namespace = "http://www.asam.net/mdf/v4",
    xmlns = {
        @XmlNs(namespaceURI = "http://www.asam.net/mdf/v4", prefix = ""),
        @XmlNs(namespaceURI = "http://www.asam.net/xml", prefix = "ho"),
    }
)
package de.richardliebscher.mdf4.blocks.metadata.properties;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;