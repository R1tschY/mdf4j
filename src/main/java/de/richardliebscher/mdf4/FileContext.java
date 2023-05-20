/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.Metadata;
import de.richardliebscher.mdf4.blocks.Text;
import de.richardliebscher.mdf4.blocks.TextBased;
import de.richardliebscher.mdf4.cache.Cache;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class FileContext {

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

  public Optional<String> readName(Link<TextBased> link, String xmlElement) {
    TextBased comment;
    try {
      final var maybeComment = link.resolve(TextBased.META, input);
      if (maybeComment.isEmpty()) {
        return Optional.empty();
      }
      comment = maybeComment.get();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    if (comment instanceof Text) {
      return Optional.of(((Text) comment).getData());
    } else if (comment instanceof Metadata) {
      String data = ((Metadata) comment).getData();
      final var reader = newXmlParser(data);
      try {
        try {
          if (reader.nextTag() != XMLStreamConstants.START_ELEMENT
              && !reader.getLocalName().equals("DGcomment")) {
            throw new UncheckedIOException(new FormatException(
                "Expected DGcomment XML element, but got " + reader.getLocalName()));
          }

          if (reader.nextTag() != XMLStreamConstants.START_ELEMENT
              && !reader.getLocalName().equals("TX")) {
            throw new UncheckedIOException(new FormatException(
                "Expected TX XML element, but got " + reader.getLocalName()));
          }
          return Optional.of(reader.getElementText());
        } finally {
          reader.close();
        }
      } catch (XMLStreamException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new IllegalStateException("Should not be reached");
    }
  }
}
