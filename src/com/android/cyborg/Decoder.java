/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.cyborg;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class Decoder {
    // Prefixes for simple primitives. These match the JNI definitions.
    private static final byte SIG_BOOLEAN = 'Z';
    private static final byte SIG_BYTE = 'B';
    private static final byte SIG_SHORT = 'S';
    private static final byte SIG_INT = 'I';
    private static final byte SIG_LONG = 'J';
    private static final byte SIG_FLOAT = 'F';
    private static final byte SIG_DOUBLE = 'D';

    // Prefixes for some commonly used objects
    private static final byte SIG_STRING = 'R';

    private static final byte SIG_MAP = 'M'; // a map with an short key
    private static final short SIG_END_MAP = 0;

    private final ByteBuffer mBuf;

    public Decoder(byte[] buf) {
        this(ByteBuffer.wrap(buf));
    }

    public Decoder(ByteBuffer buf) {
        mBuf = buf;
    }

    public boolean hasRemaining() {
        return mBuf.hasRemaining();
    }

    public Object readObject() {
        byte sig = mBuf.get();

        switch (sig) {
            case SIG_BOOLEAN:
                return mBuf.get() == 0 ? Boolean.FALSE : Boolean.TRUE;
            case SIG_BYTE:
                return mBuf.get();
            case SIG_SHORT:
                return mBuf.getShort();
            case SIG_INT:
                return mBuf.getInt();
            case SIG_LONG:
                return mBuf.getLong();
            case SIG_FLOAT:
                return mBuf.getFloat();
            case SIG_DOUBLE:
                return mBuf.getDouble();
            case SIG_STRING:
                return readString();
            case SIG_MAP:
                return readMap();
            default:
                throw new DecoderException(sig, mBuf.position() - 1);
        }
    }

    private String readString() {
        short len = mBuf.getShort();
        byte[] b = new byte[len];
        mBuf.get(b, 0, len);
        return new String(b, Charset.forName("utf-8"));
    }

    private Map<Short, Object> readMap() {
        Map<Short, Object> m = new HashMap<Short, Object>();

        while (true) {
            Object o = readObject();
            if (!(o instanceof Short)) {
                throw new DecoderException("Expected short key, got " + o.getClass());
            }

            Short key = (Short)o;
            if (key == SIG_END_MAP) {
                break;
            }

            m.put(key, readObject());
        }

        return m;
    }

    public static class DecoderException extends RuntimeException {
        public DecoderException(byte seen, int pos) {
            super(String.format("Unexpected byte %c seen at position %d", (char)seen, pos));
        }

        public DecoderException(String msg) {
            super(msg);
        }
    }
}
