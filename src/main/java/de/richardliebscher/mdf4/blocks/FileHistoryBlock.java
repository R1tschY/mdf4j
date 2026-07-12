package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.LazyIoIterator;
import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.TimeStamp;
import de.richardliebscher.mdf4.io.ByteInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public record FileHistoryBlock(
        Link<FileHistoryBlock> next,
        Link<MetadataBlock> comment,
        TimeStamp time
) implements Block {
  public static FileHistoryBlock parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parseExpecting(ID, input, 2, 16);
    final var links = blockHeader.getLinks();
    final var time = TimeStamp.parse(input);
    return new FileHistoryBlock(Link.of(links[0]), Link.of(links[1]), time);
  }

  @Override
  public BlockTypeId typeId() {
    return ID;
  }

  @Override
  public List<? extends Link<?>> links() {
    return List.of();
  }

  @Override
  public List<Map.Entry<String, String>> content() {
    return List.of();
  }

  @Override
  public Link<? extends Metadata> metadataLink() {
    return comment;
  }

  public static class Iterator implements LazyIoIterator<FileHistoryBlock> {
    private final ByteInput input;
    private Link<FileHistoryBlock> next;

    public Iterator(Link<FileHistoryBlock> start, ByteInput input) {
      this.input = input;
      this.next = start;
    }

    @Override
    public boolean hasNext() {
      return !next.isNil();
    }

    @Override
    public FileHistoryBlock next() throws IOException {
      final var next = this.next.resolve(TYPE, input).orElse(null);
      if (next == null) {
        return null;
      }
      this.next = next.next();
      return next;
    }
  }

  public static final Type TYPE = new Type();
  public static final BlockTypeId ID = BlockTypeId.of('F', 'H');

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type implements BlockType<FileHistoryBlock> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public FileHistoryBlock parse(ByteInput input) throws IOException {
      return FileHistoryBlock.parse(input);
    }
  }
}
