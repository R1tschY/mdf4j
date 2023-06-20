package de.richardliebscher.mdf4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaSerde {
  public static byte[] ser(Object obj) {
    final var res = new ByteArrayOutputStream();
    try (var oos = new ObjectOutputStream(res)) {
      oos.writeObject(obj);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return res.toByteArray();
  }

  public static Object de(byte[] bytes) {
    try (var ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
