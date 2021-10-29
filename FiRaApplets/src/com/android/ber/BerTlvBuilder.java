package com.android.ber;

import javacard.framework.Util;

public class BerTlvBuilder {

    private BerStack berStack;
    private final short DEFAULT_STACK_SIZE = 20;

    public BerTlvBuilder() {
        berStack = new BerStack(DEFAULT_STACK_SIZE);
    }

    public BerTlvBuilder(short stackSize) {
        berStack = new BerStack(stackSize);
    }

    public short addTlv(byte[] buffer, short bufferOffset, short bufferLength,
                        byte[] tag, short tagOffset, short tagLength,
                        byte[] tlv, short tlvOffset, short tlvLength) {
        short rOffset = bufferOffset;

        /* Return if buffer overflow is going to happen */
        if ((short)(rOffset + tagLength + tlvLength) > bufferLength) return bufferOffset;

        for (short i = 0; i < tagLength ; i++) {
            buffer[rOffset++] = tag[(short)(i + tagOffset)];
        }

        rOffset += fillLength(buffer, tlvLength, rOffset);

        for (short i = 0; i < tlvLength ; i++) {
            buffer[rOffset++] = tlv[(short)(i + tlvOffset)];
        }

        return rOffset;
    }

    public short addTlv(byte[] buffer, byte[] tag, byte[] HexLength, short offset) {
        short rOffset = offset;

        /* Return if buffer overflow is going to happen */
        if ((short) (rOffset + tag.length + HexLength.length) > buffer.length) return offset;

        for (short i = 0; i < tag.length ; i++) {
            buffer[rOffset++] = tag[i];
        }

        rOffset += fillLength(buffer, (short) HexLength.length, rOffset);

        for (short i = 0; i < HexLength.length ; i++) {
            buffer[rOffset++] = HexLength[i];
        }

        return rOffset;
    }

    public short endCOTag(byte[] buffer, byte[] tag, short offset) {
        short rOffset = offset;
        short startOffset = berStack.pop();
        short lengthBytesCnt = getLengthByteCnt((short) (offset - startOffset));

        /* Return if buffer overflow is going to happen */
        if ((short) (rOffset + tag.length) > buffer.length) return offset;

        //System.arraycopy(buffer, startOffset, buffer, startOffset + tag.length + lengthBytesCnt,
        //                                                offset - startOffset);
        Util.arrayCopy(buffer, startOffset, buffer, (short) (startOffset + tag.length + lengthBytesCnt),
                								(short)(offset - startOffset));
        rOffset += (tag.length + lengthBytesCnt);

        for (short i = startOffset, j =0 ; i < (short)(tag.length + startOffset) ; i++) {
            buffer[i] = tag[j++];
        }

                                        /* Actual length */
        fillLength(buffer, (short) (offset - startOffset), (short) (startOffset + tag.length));

        return rOffset;
    }

    public void startCOTag(short offset) {
        berStack.push(offset);
    }

    /* return number of bytes required for length*/
    private short fillLength(byte[] buffer, short length, short offset) {
        short byteCnt = 1;

        if (length < 0x80) {
            buffer[offset] = (byte) length;
        } else if (length <0x100) {
            buffer[offset] = (byte) 0x81;
            buffer[(short) (offset+1)] = (byte) length;
            byteCnt = 2;
        } else {
        	buffer[offset]   = (byte) 0x82;
        	buffer[(short)(offset + 1)] = (byte) (length / 0x100);
        	buffer[(short)(offset + 2)] = (byte) (length % 0x100);
        	byteCnt = 3;
        }
//        } else if (length < 0x10000) {
//            buffer[offset]   = (byte) 0x82;
//            buffer[(short)(offset + 1)] = (byte) (length / 0x100);
//            buffer[(short)(offset + 2)] = (byte) (length % 0x100);
//            byteCnt = 3;
//        } else if (length < 0x1000000) {
//            buffer[offset]   = (byte) 0x83;
//            buffer[(short)(offset + 1)] = (byte) (length / 0x10000);
//            buffer[(short)(offset + 2)] = (byte) (length / 0x100);
//            buffer[(short)(offset + 3)] = (byte) (length % 0x100);
//            byteCnt = 4;
//        } else {
//            // throw new IllegalStateException("length ["+length+"] out of range (0x1000000)");
//        }

        return byteCnt;
    }

    private short getLengthByteCnt(short length) {
        if (length < 0x80) 
        	return 1;
        else if (length <0x100)
        	return 2;
        else 
        	return 3;
//        else if (length < 0x10000)
//        	return 3;
//        else 
//        	return 4;
        /* TODO: exception*/
    }
}