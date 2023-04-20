package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.exceptions.ParseException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Value(staticConstructor = "of")
public class MdfFormatVersion {
    private static final Pattern VERSION_RE = Pattern.compile("^(\\d)\\.(\\d\\d) {4}$");

    int major;
    int minor;

    public static MdfFormatVersion parse(ByteInput input) throws IOException {
        final var formatId = input.readString(8, StandardCharsets.US_ASCII);
        final var matcher = VERSION_RE.matcher(formatId);
        if (matcher.find()) {
            return new MdfFormatVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        } else {
            throw new ParseException("Unable to parse MDF format version, got " + formatId);
        }
    }

    public int asInt() {
        return major * 100 + minor;
    }

    public static class Meta implements FromBytesInput<MdfFormatVersion> {
        @Override
        public MdfFormatVersion parse(ByteInput input) throws IOException {
            return MdfFormatVersion.parse(input);
        }
    }
}
