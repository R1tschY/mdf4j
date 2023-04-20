package de.richardliebscher.mdf4.extract.de;

import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.Visitor;

import java.io.IOException;

public class ObjectDeserialize implements Deserialize<Object> {
    @Override
    public Object deserialize(Deserializer deserializer) throws IOException {
        return deserializer.deserialize_value(new Visitor<>() {
            @Override
            public Object visitU8(byte value) {
                return value;
            }

            @Override
            public Object visitU16(short value) {
                return value;
            }

            @Override
            public Object visitU32(int value) {
                return value;
            }

            @Override
            public Object visitU64(long value) {
                return value;
            }

            @Override
            public Object visitI8(byte value) {
                return value;
            }

            @Override
            public Object visitI16(short value) {
                return value;
            }

            @Override
            public Object visitI32(int value) {
                return value;
            }

            @Override
            public Object visitI64(long value) {
                return value;
            }

            @Override
            public Object visitF32(float value) {
                return value;
            }

            @Override
            public Object visitF64(double value) {
                return value;
            }

            @Override
            public Object visitInvalid() {
                return Invalid.get();
            }
        });
    }
}
