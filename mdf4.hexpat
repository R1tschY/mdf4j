// imhex format pattern

#pragma author "Richard Liebscher"
#pragma description "ASAM MDF4 (Measurement Data Format) Pattern"
#pragma file_extension mf4
#pragma file_extension mdf
#pragma endian little

#include <std/mem.pat>
#include <std/sys.pat>
#include <type/magic.pat>

bitfield UnfinalizedFlags {
    dirty_cgca_cycle_counters : 1;
    dirty_sr_cycle_counters : 1;
    dirty_last_dt_length : 1;
    dirty_last_rd_length : 1;
    dirty_last_dl : 1;
    dirty_vlsd_byte_lengths : 1;
    dirty_vlsd_offset : 1;
    reserved : 9;
};

struct IdBlock {
    try {
        type::Magic<"MDF     "> mdf;
    } catch {
        type::Magic<"UnFinMF "> mdf;
    }

    char version_str[8] [[comment("Format version as string, e.g. '4.10    '")]];
    char program_id[8] [[comment("Program identifier that created the file")]];
    u16 reserved_byte_order [[comment("3.x: Byte order (0 = Little endian)")]];
    u16 reserved_floating_point_format [[comment("3.x: Floating point format (0 = IEEE 754)")]];
    u16 version_number [[comment("Version number as integer, e.g. 410")]];
    u16 code_page_number [[comment("Code page number (0 if not used)")]];
    u8 reserved[28] [[comment("Reserved / fill bytes")]];
    UnfinalizedFlags unfinalized_flags [[comment("Standard unfinalized flags")]];
    u16 custom_unfinalized_flags [[comment("Custom unfinalized flags")]];
};

IdBlock ID @ 0x00;

struct BlockHeader {
    char id[4] [[comment("Block type identifier, e.g. '##HD'")]];
    u32 reserved [[comment("Reserved / padding")]];
    u64 length [[comment("Block length in bytes")]];
    u64 link_count [[comment("Number of links")]];
};

enum TimeClass : u8 {
    LocalPC = 0,
    ExternalTimeSource = 10,
    ExternalAbsoluteSynchronizedTime = 16
};

bitfield TimeFlags {
    local_time : 1;
    time_offset_valid : 1;
    reserved : 6;
};

struct TimeStamp {
    u64 time [[comment("Absolute time in nanoseconds since 1970-01-01 00:00:00 UTC")]];
    s16 tz_offset_min [[comment("Time zone offset in minutes")]];
    s16 dst_offset_min [[comment("Daylight saving time offset in minutes")]];
    TimeFlags flags [[comment("Time flags")]];
};

bitfield HeaderFlags {
    start_angle_valid : 1;
    start_distance_valid : 1;
    reserved : 6;
};

struct FileHistoryBlock {
    BlockHeader header;
    u64 next [[comment("Link to next file history block (##FH)")]];
    if (next != 0) {
        FileHistoryBlock next_ @ next;
    }

    u64 comment [[comment("Link to comment / metadata block (##MD or ##TX)")]];
    if (header.link_count > 2) {
        u64 extra_links[header.link_count - 2] [[comment("Extra links")]];
    }
    TimeStamp time [[comment("Time stamp of file modification / creation")]];
    if (header.length > 24 + header.link_count * 8 + 13) {
        u8 reserved[header.length - 24 - header.link_count * 8 - 13] [[comment("Reserved / flags")]];
    }
};

struct HeaderBlock {
    BlockHeader header;
    u64 first_data_group [[comment("Link to first data group block (##DG)")]];
    u64 first_file_history [[comment("Link to first file history block (##FH)")]];
    if (first_file_history != 0) {
        FileHistoryBlock first_file_history_ @ first_file_history;
    }
    u64 first_channel_hierarchy [[comment("Link to first channel hierarchy block (##CH)")]];
    u64 first_attachment [[comment("Link to first attachment block (##AT)")]];
    u64 first_event_block [[comment("Link to first event block (##EV)")]];
    u64 comment [[comment("Link to comment / metadata block (##MD or ##TX)")]];
    if (header.link_count > 6) {
        u64 extra_links[header.link_count - 6] [[comment("Extra links")]];
    }
    TimeStamp start_time [[comment("Time stamp at start of measurement")]];
    TimeClass time_class [[comment("Time source and classification")]];
    HeaderFlags flags [[comment("Header flags")]];
    u8 reserved [[comment("Reserved")]];
    double start_angle_rad [[comment("Start angle in radians at start of measurement")]];
    double start_distance_m [[comment("Start distance in meters at start of measurement")]];
};

HeaderBlock HD @ 0x40;