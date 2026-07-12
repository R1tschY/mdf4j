package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.IntegrationTest;
import de.richardliebscher.mdf4.Mdf4File;
import de.richardliebscher.mdf4.Result;
import de.richardliebscher.mdf4.TimeStamp;
import de.richardliebscher.mdf4.blocks.metadata.FileHistoryComment;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FileHistoryBlockTest {
  @Test
  void checkAsamMdfFile() throws Exception {
    // ARRANGE
    try (var input = IntegrationTest.openMdf("/primitives.mf4"); var mdf = Mdf4File.open(input)) {

      // ACT
      final var results = mdf.getHeader().iterFileHistory(input).stream().toList();

      // ASSERT
      assertThat(results)
              .singleElement()
              .isInstanceOf(Result.Ok.class)
              .returns(OffsetDateTime.of(2023, 4, 29, 11, 16, 27, 680089088, ZoneOffset.ofHours(2)), x -> x.unwrap().time().toDateTime().orElseThrow())
              .extracting(result -> {
                try {
                  return mdf.blockReader().readMetadata(result.unwrap().comment(), FileHistoryComment.class).orElseThrow();
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              })
              .returns(List.of("created"), x -> x.comment().content())
              .returns("asammdf", x -> x.toolId().value())
              .returns("asammdf", x -> x.toolVendor().value())
              .returns("7.3.12", x -> x.toolVersion().value());
    }
  }

  @Test
  void checkPeaFile() throws Exception {
    // ARRANGE
    try (var input = IntegrationTest.openMdf("/KonvektionKalt1-20140123-143636.mf4"); var mdf = Mdf4File.open(input)) {

      // ACT
      final var results = mdf.getHeader().iterFileHistory(input).stream().toList();

      // ASSERT
      assertThat(results)
              .singleElement()
              .isInstanceOf(Result.Ok.class)
              .returns(OffsetDateTime.of(2014, 1, 23, 14, 36, 13, 263156000, ZoneOffset.ofHours(1)), x -> x.unwrap().time().toDateTime().orElseThrow())
              .extracting(result -> {
                try {
                  return mdf.blockReader().readMetadata(result.unwrap().comment(), FileHistoryComment.class).orElseThrow();
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              })
              .returns(List.of("created"), x -> x.comment().content())
              .returns("PEA32", x -> x.toolId().value())
              .returns("Porsche AG", x -> x.toolVendor().value())
              .returns("2.4.7", x -> x.toolVersion().value());
    }
  }
}
