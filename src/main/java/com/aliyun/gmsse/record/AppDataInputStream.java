package com.aliyun.gmsse.record;

import com.aliyun.gmsse.Record;
import com.aliyun.gmsse.RecordStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class AppDataInputStream extends InputStream
{
    private RecordStream recordStream;
    private ByteBuffer buffer;
    private boolean appDataIsAvailable;

    public AppDataInputStream(RecordStream recordStream)
    {
        this.recordStream = recordStream;
        buffer = ByteBuffer.allocate(4096);
        appDataIsAvailable = false;
    }

    @Override
    public int read() throws IOException {
        byte[] buf = new byte[1];
        int ret = read(buf, 0, 1);
        return ret < 0 ? -1 : buf[0] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int remains = available();
        if (remains > 0) {
            int howmany = Math.min(remains, len);
            buffer.get(b, off, howmany);

            return howmany;
        }

        appDataIsAvailable = false;
        try
        {
            buffer.clear();
            Record record = recordStream.read(true);
            int packetLen = record.fragment.length;

            if (packetLen > buffer.remaining()) {
                buffer = ByteBuffer.allocate(packetLen);
            }

            buffer.put(record.fragment, 0, packetLen);
            buffer.flip();
            appDataIsAvailable = true;

            int howmany = Math.min(len, packetLen);
            buffer.get(b, off, howmany);
            return howmany;
        } catch (Exception e) {
            return -1;
        }
    }

    public int available() throws IOException {
        if ((!appDataIsAvailable)) {
            return 0;
        }

        return buffer.remaining();
    }

//    @Override
//    public int read(byte[] b, int off, int len) throws IOException {
//        Record record = recordStream.read(true);
//        int length = Math.min(record.fragment.length, len);
//        System.arraycopy(record.fragment, 0, b, off, length);
//        return length;
//    }
}
