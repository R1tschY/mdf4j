package de.richardliebscher.mdf4;

import static org.assertj.core.api.Assertions.assertThat;

import de.richardliebscher.mdf4.blocks.HeaderBlock;
import de.richardliebscher.mdf4.io.ByteBufferInput;
import de.richardliebscher.mdf4.write.Mdf4Writer;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Mdf4FileTest {

  @TempDir
  Path tmpDir;

  @Test
  void openEmpty() throws Exception {
    // ARRANGE
    final var now = TimeStamp.now();

    final var buf = ByteBuffer.allocate(1024);
    try (var writer = new Mdf4Writer.Builder().createForMemory(buf)) {
      writer.writeHeader(new HeaderBlock.Builder()
          .startTime(now)
          .build());
      writer.finalizeFile();
    }
    buf.rewind();

    // ACT
    try (final var file = Mdf4File.open(new ByteBufferInput(buf))) {

      // ASSERT
      assertThat(file.getDataGroups().stream().count()).isEqualTo(0);
      assertThat(file.getStartTime().toInstant()).isEqualTo(now.toInstant());
    }
  }

  @Test
  void openEmptyFile() throws Exception {
    // ARRANGE
    final var now = TimeStamp.now();

    final var f = tmpDir.resolve("file.mf4");
    try (var writer = new Mdf4Writer.Builder().create(f)) {
      writer.writeHeader(new HeaderBlock.Builder()
          .startTime(now)
          .build());
      writer.finalizeFile();
    }

    // ACT
    try (final var file = Mdf4File.open(f)) {

      // ASSERT
      assertThat(file.getDataGroups().stream().count()).isEqualTo(0);
      assertThat(file.getStartTime().toInstant()).isEqualTo(now.toInstant());
    }
  }
}
