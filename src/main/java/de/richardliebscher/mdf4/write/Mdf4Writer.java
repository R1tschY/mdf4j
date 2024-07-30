/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.write;

import static de.richardliebscher.mdf4.Mdf4File.TOOL_VERSION;
import static de.richardliebscher.mdf4.blocks.Consts.FILE_MAGIC;
import static de.richardliebscher.mdf4.blocks.Consts.UNFINISHED_FILE_MAGIC;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.MdfFormatVersion;
import de.richardliebscher.mdf4.blocks.BlockTypeId;
import de.richardliebscher.mdf4.blocks.HeaderBlock;
import de.richardliebscher.mdf4.extract.read.Links;
import de.richardliebscher.mdf4.io.ByteBufferInput;
import de.richardliebscher.mdf4.io.FileInput;
import de.richardliebscher.mdf4.io.ReadWrite;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * MDF4 Writer.
 *
 * <pre>{@code
 *  try (final var writer = Mdf4Writer.builder().create("file.mf4")) {
 *    writer.writeHeader(HeaderBlock.builder().startTime(TimeStamp.now()).build());
 *    writer.finalizeFile();
 *  }
 * }</pre>
 */
public class Mdf4Writer implements Closeable {

  private final ReadWrite input;

  private Mdf4Writer(ReadWrite input) {
    this.input = input;
  }

  /**
   * Create builder.
   *
   * @return {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void close() throws IOException {
    input.close();
  }

  private static <T extends Closeable> T useClosable(T closeable, UseClosable<T> fn)
      throws IOException {
    boolean success = false;
    try {
      fn.use(closeable);
      success = true;
      return closeable;
    } finally {
      if (!success) {
        closeable.close();
      }
    }
  }

  private interface UseClosable<T extends Closeable> {

    void use(T closeable) throws IOException;
  }

  /**
   * Finalize MDF file.
   *
   * @throws IOException Failed to write
   */
  public void finalizeFile() throws IOException {
    input.seek(0);
    input.write(FILE_MAGIC.getBytes(StandardCharsets.ISO_8859_1));
  }

  /**
   * Builder.
   */
  public static class Builder {

    private final MdfFormatVersion version = TOOL_VERSION;

    /**
     * Create new MDF4 file.
     *
     * @param path Path to existing file
     * @return Writer
     * @throws IOException Failed to create and write file
     */
    public Mdf4Writer create(Path path) throws IOException {
      final ReadWrite input = new FileInput(path, StandardOpenOption.READ,
          StandardOpenOption.WRITE, StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
      return create(input);
    }

    private Mdf4Writer create(ReadWrite input) throws IOException {
      return useClosable(new Mdf4Writer(input), writer -> {
        // ID
        writer.writeIdBlock(version);
      });
    }

    /**
     * Create new in-memory MDF4 file.
     *
     * @param byteBuffer In-memory buffer
     * @return Writer
     */
    public Mdf4Writer createForMemory(ByteBuffer byteBuffer) {
      try {
        return create(new ByteBufferInput(byteBuffer));
      } catch (IOException e) {
        throw new RuntimeException("Should not happen", e);
      }
    }
  }

  private void writeIdBlock(MdfFormatVersion version) throws IOException {
    input.write(UNFINISHED_FILE_MAGIC, StandardCharsets.ISO_8859_1);
    input.write(
        version.getMajor() + "." + version.getMinor() + "    ",
        StandardCharsets.ISO_8859_1);
    input.write("mdf4j\0\0\0", StandardCharsets.ISO_8859_1);
    input.write((short) 0); // defaultByteOrder
    input.write((short) 0); // defaultFloatingPointFormat
    input.write((short) version.asInt());
    input.write((short) 0); // codePageNumber
    input.writePadding(28); // fill bytes
    input.write((short) 0); // unfinalizedFlags
    input.write((short) 0); // customUnfinalizedFlags
  }

  /**
   * Write block header.
   *
   * @param typeId     Block type ID
   * @param links      Links
   * @param dataLength Data section length
   * @throws IOException Failed to write
   */
  public void writeBlockHeader(BlockTypeId typeId, Links<?> links, long dataLength)
      throws IOException {
    input.write(typeId.asInt());
    input.writePadding(4);
    input.write(dataLength + 24L + links.size() * 8L);
    input.write((long) links.size());
    for (Link<?> link : links) {
      input.write(link.asLong());
    }
  }

  /**
   * Write header block.
   *
   * @param headerBlock Header block to write
   * @throws IOException Failed to write
   */
  public void writeHeader(HeaderBlock headerBlock) throws IOException {
    writeBlockHeader(
        HeaderBlock.ID,
        new Links<>(new long[]{
            headerBlock.getFirstDataGroup().asLong(),
            headerBlock.getFirstFileHistory(),
            headerBlock.getFirstChannelHierarchy(),
            headerBlock.getFirstAttachment(),
            headerBlock.getFirstEventBlock(),
            headerBlock.getComment().asLong(),
        }),
        32);

    input.write(headerBlock.getStartTime());
    input.write(headerBlock.getTimeClass().asByte());
    input.write(headerBlock.getHeaderFlags().asByte());
    input.writePadding(1);
    input.write(headerBlock.getStartAngleRad());
    input.write(headerBlock.getStartDistanceM());
  }
}
