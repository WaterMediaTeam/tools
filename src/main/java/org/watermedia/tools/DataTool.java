package org.watermedia.tools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DataTool {
    public static int readBytesAsInt(final ByteBuffer buffer, final int length, final ByteOrder order) {
        int value = 0;
        for (int i = 0; i < length; i++) {
            value |= (buffer.get() & 0xFF) << toLong$pos((length * 8) - 8, i * 8, order);
        }
        return value;
    }

    public static long readBytesAsLong(final ByteBuffer buffer, final int length, final ByteOrder order) {
        long value = 0;
        for (int i = 0; i < length; i++) {
            value |= (long) (buffer.get() & 0xFF) << toLong$pos((length * 8) - 8, i * 8, order);
        }
        return value;
    }

    public static long sumArray(final long... array) {
        int i = 0;
        long result = 0;
        while (i < array.length) {
            result += array[i++];
        }
        return result;
    }

    public static short toShort(final byte one, final byte two, final ByteOrder order) {
        return (short) ((one & 0xFF) << toLong$pos(8, 0, order) | (two & 0xFF) << toLong$pos(8, 8, order));
    }

    public static int toInt(final byte one, final byte two, final byte three, final byte four, final ByteOrder order) {
        return (one & 0xFF) << toLong$pos(24, 0, order) | (two & 0xFF) << toLong$pos(24, 8, order) | (three & 0xFF) << toLong$pos(24, 16, order) | (four & 0xFF) << toLong$pos(24, 24, order);
    }

    public static long toLong(final byte b1, final byte b2, final byte b3, final byte b4, final byte b5, final byte b6, final byte b7, final byte b8, final ByteOrder order) {
        return ((long)(b1 & 0xFF) << toLong$pos(56, 0, order)) |
                ((long)(b2 & 0xFF) << toLong$pos(56, 8, order)) |
                ((long)(b3 & 0xFF) << toLong$pos(56, 16, order)) |
                ((long)(b4 & 0xFF) << toLong$pos(56, 24, order)) |
                ((long)(b5 & 0xFF) << toLong$pos(56, 32, order)) |
                ((long)(b6 & 0xFF) << toLong$pos(56, 40, order)) |
                ((long)(b7 & 0xFF) << toLong$pos(56, 48, order))  |
                ((long)(b8 & 0xFF) << toLong$pos(56, 56, order));
    }

    private static int toLong$pos(final int top, final int pos, final ByteOrder order) {
        return order == ByteOrder.BIG_ENDIAN ? top - pos : pos;
    }

    public static void rgbaToBrga(final ByteBuffer buffer, final int pixel, final byte a) {
        final int r = (pixel >> 16) & 0xFF;
        final int g = (pixel >> 8) & 0xFF;
        final int b = pixel & 0xFF;
        buffer.put((byte) b);
        buffer.put((byte) g);
        buffer.put((byte) r);
        buffer.put(a);
    }
}
