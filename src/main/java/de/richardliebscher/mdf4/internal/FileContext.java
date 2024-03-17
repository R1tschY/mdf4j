/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.blocks.Metadata;
import de.richardliebscher.mdf4.blocks.Metadata.Visitor;
import de.richardliebscher.mdf4.blocks.MetadataBlock;
import de.richardliebscher.mdf4.blocks.TextBlock;
import de.richardliebscher.mdf4.cache.Cache;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.extract.read.Scope;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Getter;

public class FileContext implements Closeable {

  @Getter
  private final ByteInput input;
  @Getter
  private final Cache cache;
  private final XMLInputFactory xmlParserFactory;
  private final Scope fileScope = new Scope();

  public FileContext(ByteInput input, Cache cache, XMLInputFactory xmlParserFactory) {
    this.input = input;
    this.cache = cache;
    this.xmlParserFactory = xmlParserFactory;

    this.fileScope.add(input);
  }

  public XMLStreamReader newXmlParser(String content) {
    try {
      return xmlParserFactory.createXMLStreamReader(new StringReader(content));
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  public Optional<String> readText(Link<Metadata> link, String xmlElement)
      throws IOException {
    final var maybeComment = link.resolve(Metadata.TYPE, input);
    if (maybeComment.isEmpty()) {
      return Optional.empty();
    }
    return maybeComment.get().accept(new Visitor<Optional<String>, IOException>() {
      @Override
      public Optional<String> visit(TextBlock value) {
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

  public Scope newScope() {
    final var scope = new Scope();
    this.fileScope.add(scope);
    return scope;
  }

  @Override
  public void close() throws IOException {
    fileScope.close();
  }
}
