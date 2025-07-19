/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import static de.richardliebscher.mdf4.blocks.metadata.XmlConstants.MDF4_NAMESPACE;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.blocks.Metadata;
import de.richardliebscher.mdf4.blocks.Metadata.Visitor;
import de.richardliebscher.mdf4.blocks.MetadataBlock;
import de.richardliebscher.mdf4.blocks.TextBlock;
import de.richardliebscher.mdf4.cache.Cache;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.extract.read.Scope;
import de.richardliebscher.mdf4.io.ByteInput;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.StreamReaderDelegate;
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
    xmlParserFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    xmlParserFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    xmlParserFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    xmlParserFactory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
      throw new XMLStreamException("External entities not supported");
    });

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

  public <T> Optional<T> readMetadata(Link<MetadataBlock> link, Class<T> cls) throws IOException {
    final var maybeComment = link.resolve(MetadataBlock.TYPE, input);
    if (maybeComment.isEmpty()) {
      return Optional.empty();
    }

    return readMetadata(maybeComment.get().getXml(), cls);
  }

  public <T> Optional<T> readMetadata(String comment, Class<T> cls)
      throws IOException {
    try {
      final var unmarshaller = JAXBContext.newInstance(cls).createUnmarshaller();
      final var source = newXmlParser(comment);
      return Optional.of(unmarshaller.unmarshal(new MdfXmlStreamReader(source), cls).getValue());
    } catch (JAXBException e) {
      throw new IOException(e);
    }
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

  private static class MdfXmlStreamReader extends StreamReaderDelegate {

    public MdfXmlStreamReader(XMLStreamReader reader) {
      super(reader);
    }

    @Override
    public String getNamespaceURI(String prefix) {
      final var namespaceUri = super.getNamespaceURI(prefix);
      if (prefix.isEmpty() && namespaceUri.isEmpty()) {
        return MDF4_NAMESPACE;
      }
      return namespaceUri;
    }

    @Override
    public String getNamespaceURI() {
      final var namespaceUri = super.getNamespaceURI();
      final var eventType = super.getEventType();
      if (namespaceUri == null
          && (eventType == XMLEvent.START_ELEMENT || eventType == XMLEvent.END_ELEMENT)) {
        final var prefix = super.getPrefix();
        if (prefix == null || prefix.isEmpty()) {
          return MDF4_NAMESPACE;
        }
      }
      return namespaceUri;
    }

    @Override
    public NamespaceContext getNamespaceContext() {
      final var context = super.getNamespaceContext();
      return new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
          final var namespaceUri = context.getNamespaceURI(prefix);
          if (prefix.isEmpty() && namespaceUri.isEmpty()) {
            return MDF4_NAMESPACE;
          }
          return namespaceUri;
        }

        @Override
        public String getPrefix(String namespaceUri) {
          final var prefix = context.getPrefix(namespaceUri);
          if (prefix == null
              && namespaceUri.equals(MDF4_NAMESPACE)
              && context.getNamespaceURI(XMLConstants.NULL_NS_URI).isEmpty()) {
            return XMLConstants.DEFAULT_NS_PREFIX;
          }
          return prefix;
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceUri) {
          final var prefixes = context.getPrefixes(namespaceUri);
          if (namespaceUri.equals(MDF4_NAMESPACE)
              && context.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).isEmpty()) {
            return Stream.concat(
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(prefixes, 0), false),
                Stream.of(XMLConstants.DEFAULT_NS_PREFIX)).iterator();
          }
          return prefixes;
        }
      };
    }
  }
}
