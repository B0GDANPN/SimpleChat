package org.example;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;

public class Misc {
    public static void close(Closeable... objects) {
        for (Closeable obj : objects) {
            if (obj != null) {
                try {
                    obj.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }
}
