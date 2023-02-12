/**
 *  Copyright 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * 
 * @author com.perezhai
 * @time 2015/10/10
 * */
package com.perez.arsceditor.ResDecoder.IO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class LEDataInputStream {
    /**  */
    private DataInputStream dis;
    /**  */
    private InputStream is;

    private boolean mIsLittleEndian = true;

    /**
     * 
     */
    protected byte[] work;

    public long size;

    public LEDataInputStream(byte[] data) throws IOException {
        this(new ByteArrayInputStream(data));
    }

    /**
     * 
     *
     * @throws IOException
     */
    public LEDataInputStream(InputStream in) throws IOException {
        
        this.is = in;
        this.dis = new DataInputStream(is);
        work = new byte[8];
        size = dis.available();
    }

    /**
     * 
     */
    public int available() throws IOException {
        return is.available();
    }

    /**
     * 
     */
    public void close() throws IOException {
        dis.close();
        is.close();
    }

    public void mark(int readlimit) throws IOException {
        is.mark(readlimit);
    }

    /**
     * ，buffer
     *
     * @throws IOException
     */
    public int read(byte[] buffer, int start, int end) throws IOException {
        return dis.read(buffer, start, end);
    }

    /**
     * 
     *
     * @throws IOException
     */
    public byte readByte() throws IOException {
        return dis.readByte();
    }

    /**
     * ，，
     *
     * @throws IOException
     */
    public void readFully(byte ba[]) throws IOException {
        dis.readFully(ba, 0, ba.length);
    }

    /**
     * ，offset，len，
     *
     * @throws IOException
     */
    public void readFully(byte ba[], int off, int len) throws IOException {
        dis.readFully(ba, off, len);
    }

    /**
     * 32int
     *
     * @throws IOException
     */
    public int readInt() throws IOException {
        if(mIsLittleEndian) {
            dis.readFully(work, 0, 4);
            return (work[3]) << 24 | (work[2] & 0xff) << 16 | (work[1] & 0xff) << 8 | (work[0] & 0xff);
        } else
            return dis.readInt();
    }

    /**
     * 32int，，
     *
     * @throws IOException
     */
    public int[] readIntArray(int length) throws IOException {
        int[] array = new int[length];
        for(int i = 0; i < length; i++)
            array[i] = readInt();
        return array;
    }

    /***
     * 64
     */
    public final long readLong() throws IOException {
        if(mIsLittleEndian) {
            dis.readFully(work, 0, 8);
            return (long)(work[7]) << 56 | (long)(work[6] & 0xff) << 48 | (long)(work[5] & 0xff) << 40
                   | (long)(work[4] & 0xff) << 32 | (long)(work[3] & 0xff) << 24 | (long)(work[2] & 0xff) << 16
                   | (long)(work[1] & 0xff) << 8 | work[0] & 0xff;
        } else
            return dis.readLong();
    }

    /**
     * 
     */
    public String readNulEndedString(int length, boolean fixed) throws IOException {
        StringBuilder string = new StringBuilder(16);
        while(length-- != 0) {
            short ch = readShort();
            if(ch == 0)
                break;
            string.append((char) ch);
        }
        if(fixed)
            skipBytes(length * 2);
        return string.toString();
    }

    /**
     * 16short
     *
     * @throws IOException
     */
    public short readShort() throws IOException {
        if(mIsLittleEndian) {
            dis.readFully(work, 0, 2);
            return (short)((work[1] & 0xff) << 8 | (work[0] & 0xff));
        } else
            return dis.readShort();
    }

    /***
     * 16Short
     */
    public int readUnsignedShort() throws IOException {
        return dis.readUnsignedShort();
    }

    public void reset() throws IOException {
        is.reset();
    }

    /**
     * 
     *
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws IOException
     * @throws NoSuchFieldException
     */
    public void seek(long position) throws IOException {
        if(is instanceof ByteArrayInputStream) {
            Class<ByteArrayInputStream> clazz = ByteArrayInputStream.class;
            Field field;
            try {
                field = clazz.getDeclaredField("pos");
                field.setAccessible(true);
                field.setInt(is, (int) position);
            } catch(NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
                throw new IOException("Unsupported");
            }
        } else
            throw new IOException("Unsupported");
    }

    public void setIsLittleEndian(boolean isLittleEndian) {
        mIsLittleEndian = isLittleEndian;
    }

    /**
     * 1
     *
     * @throws IOException
     */
    public void skipByte() throws IOException {
        skipBytes(1);
    }

    /**
     * n
     *
     * @throws IOException
     */
    public void skipBytes(int n) throws IOException {
        dis.skipBytes(n);
    }

    public void skipCheckByte(byte expected) throws IOException {
        byte got = readByte();
        if(got != expected)
            throw new IOException(String.format("CheckByte Expected: 0x%08x, got: 0x%08x", expected, got));
    }

    public int skipCheckChunkTypeInt(int expected, int possible) throws IOException {
        int got = readInt();
        if(got == possible)
            skipCheckChunkTypeInt(expected, -1);
        else if(got != expected)
            throw new IOException(String.format("CheckChunkTypeInt Expected: 0x%08x, got: 0x%08x", expected, got));
        return got;
    }

    public void skipCheckInt(int expected) throws IOException {
        int got = readInt();
        if(got != expected)
            throw new IOException(String.format("CheckInt Expected: 0x%08x, got: 0x%08x", expected, got));
    }

    public void skipCheckShort(short expected) throws IOException {
        short got = readShort();
        if(got != expected)
            throw new IOException(String.format("CheckShort Expected: 0x%08x, got: 0x%08x", expected, got));
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while(-1 != (n = input.read(buffer)))
            output.write(buffer, 0, n);
        return output.toByteArray();
    }

    /**
     * 32int
     *
     * @throws IOException
     */
    public void skipInt() throws IOException {
        skipBytes(4);
    }

    /**
     * 16short
     *
     * @throws IOException
     */
    public void skipShort() throws IOException {
        skipBytes(2);
    }

}
