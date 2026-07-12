package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;

import java.io.IOException;
import java.util.Optional;

/**
 * Low-level block read access.
 */
public interface BlockReader {
  <T> Optional<T> readBlock(Link<T> link, BlockType<T> type) throws IOException;

  <T> Optional<T> readBlockNonCache(Link<T> link, BlockType<T> type) throws IOException;

  Optional<String> readText(Link<Metadata> link, String xmlElement) throws IOException;

  <T> Optional<T> readMetadata(Link<MetadataBlock> link, Class<T> cls) throws IOException;
}
