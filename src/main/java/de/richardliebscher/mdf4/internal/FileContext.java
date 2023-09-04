/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.blocks.MetadataBlock;
import de.richardliebscher.mdf4.blocks.TextBasedBlock;
import de.richardliebscher.mdf4.blocks.TextBasedBlock.Visitor;
import de.richardliebscher.mdf4.blocks.TextBlockBlock;
import de.richardliebscher.mdf4.cache.Cache;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileContext {

  @Getter
  private final ByteInput input;
  @Getter
  private final Cache cache;
  private final XMLInputFactory xmlParserFactory;

  public XMLStreamReader newXmlParser(String content) {
    try {
      return xmlParserFactory.createXMLStreamReader(new StringReader(content));
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  public Optional<String> readText(Link<TextBasedBlock> link, String xmlElement)
      throws IOException {
    final var maybeComment = link.resolve(TextBasedBlock.META, input);
    if (maybeComment.isEmpty()) {
      return Optional.empty();
    }
    return maybeComment.get().accept(new Visitor<Optional<String>, IOException>() {
      @Override
      public Optional<String> visit(TextBlockBlock value) {
        return Optional.of(value.getText());
      }

      @Override
      public Optional<String> visit(MetadataBlock value) throws IOException {
        final var reader = newXmlParser(value.getXml());
        try {
          try {
            if (reader.nextTag() != XMLStreamConstants.START_ELEMENT
                && !reader.getLocalName().equals(xmlElement)) {
              throw new IOException(new FormatException(
                  "Expected " + xmlElement + " XML element, but got " + reader.getLocalName()));
            }

            if (reader.nextTag() != XMLStreamConstants.START_ELEMENT
                && !reader.getLocalName().equals("TX")) {
              throw new IOException(new FormatException(
                  "Expected TX XML element, but got " + reader.getLocalName()));
            }
            return Optional.of(reader.getElementText());
          } finally {
            reader.close();
          }
        } catch (XMLStreamException e) {
          throw new IOException(e);
        }
      }
    });
  }
}
