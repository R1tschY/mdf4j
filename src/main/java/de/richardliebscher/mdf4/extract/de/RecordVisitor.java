package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

public interface RecordVisitor<T> {
    T visitRecord(RecordAccess recordAccess) throws IOException;
}
