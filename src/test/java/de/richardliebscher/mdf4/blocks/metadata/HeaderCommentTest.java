/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import de.richardliebscher.mdf4.internal.FileContext;
import java.io.IOException;
import javax.xml.stream.XMLInputFactory;
import org.junit.jupiter.api.Test;

class HeaderCommentTest {

  @Test
  void check() throws IOException {
    final var input = "<HDcomment>\n"
        + "\t<TX>Simple MF4 file created by Vector CANape 10.0\n"
        + "This file shows 2 channel groups, each containing a time master channel and some value channels.</TX>\n"
        + "\t<common_properties><e name=\"author\">Otmar Schneider</e>\n"
        + "<e name=\"department\">Vector Informatik GmbH</e>\n"
        + "<e name=\"project\">ASAM COMMON MDF 4.1 example file</e>\n"
        + "<e name=\"subject\">XCPsim simulation</e>\n"
        + "</common_properties>\n"
        + "</HDcomment>";

    final var headerComment = new FileContext(null, null, XMLInputFactory.newDefaultFactory())
        .readMetadata(input, HeaderComment.class)
        .orElseThrow();

    assertThat(headerComment.getCommonProperties())
        .containsOnlyKeys("author", "department", "project", "subject");
  }
}