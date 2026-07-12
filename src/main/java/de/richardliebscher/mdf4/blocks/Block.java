package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;

import java.util.List;
import java.util.Map;

public interface Block {
    BlockTypeId typeId();
    List<? extends Link<?>> links();
    List<Map.Entry<String, String>> content();
    Link<? extends Metadata> metadataLink();
}
