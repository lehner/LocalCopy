/*
 * Copyright (C) 2009 Christoph Lehner
 * 
 * All programs in this directory and subdirectories are published under the GNU
 * General Public License as described below.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Further information about the GNU GPL is available at:
 * http://www.gnu.org/copyleft/gpl.ja.html
 *
 */
package localcopy;

public class Base64 {

    private static final char[] map64 = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '+', '/'};

    private static int inverseMap64(byte c) {

	if (c >= 'A' && c <= 'Z') {
	    return c-'A';
	} else if (c >= 'a' && c <= 'z') {
	    return c-'a' + 26;
	} else if (c >= '0' && c <= '9') {
	    return c-'0' + 52;
	} else if (c == '+') {
	    return 62;
	} else if (c == '/') {
	    return 63;
	}

	return 64;
    }

    private static int toUnsignedByte(byte c) {
	return (c<0)?c+256:c;
    }

    private static byte toSignedByte(int c) {
	return (c>127)?((byte)(c-256)):((byte)c);
    }

    public static byte[] decode(String s) {
        return decode(s.getBytes());
    }

    public static byte[] decode(byte[] in) {
	if (in.length == 0)
	    return new byte[0];
	
	if (in.length % 4 != 0)
	    return null;

	int nFourByteChunks = in.length / 4;
	int inPos = 0;
	int outPos = 0;
	int nStripBytes = 0;

	if (in[in.length-1] == '=') {
	    nStripBytes++;
	    if (in[in.length-2] == '=') {
		nStripBytes++;
	    }
	}

	byte[] out = new byte[nFourByteChunks * 3 - nStripBytes];

	while (inPos < in.length) {
	    if (in[inPos+3] == '=') {
		if (in[inPos+2] == '=') {
		    int fourBytes = inverseMap64(in[inPos]) * 64 * 64 * 64 +
			inverseMap64(in[inPos+1]) * 64 * 64;
		    fourBytes /= 256 * 256;
		    out[outPos] = toSignedByte(fourBytes % 256);
		    outPos += 1;
		} else {
		    int fourBytes = inverseMap64(in[inPos]) * 64 * 64 * 64 +
			inverseMap64(in[inPos+1]) * 64 * 64 + 
			inverseMap64(in[inPos+2]) * 64;
		    fourBytes /= 256;
		    out[outPos+1] = toSignedByte(fourBytes % 256); fourBytes /= 256;
		    out[outPos] = toSignedByte(fourBytes % 256);
		    outPos += 2;
		}
	    } else {
		int fourBytes = inverseMap64(in[inPos]) * 64 * 64 * 64 +
		    inverseMap64(in[inPos+1]) * 64 * 64 + 
		    inverseMap64(in[inPos+2]) * 64 + 
		    inverseMap64(in[inPos+3]);
		out[outPos+2] = toSignedByte(fourBytes % 256); fourBytes /= 256;
		out[outPos+1] = toSignedByte(fourBytes % 256); fourBytes /= 256;
		out[outPos] = toSignedByte(fourBytes % 256);
		outPos += 3;
	    }
	    inPos += 4;
	}

	return out;
    }

    public static String encode(String s) {
        return encode(s.getBytes());
    }

    public static String encode(byte[] in) {

	if (in.length == 0)
	    return new String("");

	int nThreeByteChunks = ((in.length - 1) / 3) + 1;
	char[] out = new char[nThreeByteChunks * 4];

        int threeBytes;
        int bits6;

        int outPos = 0;
        int inPos = 0;

        while ((inPos + 3) <= in.length) {
            
            threeBytes = (toUnsignedByte(in[inPos]) * 256 * 256 + toUnsignedByte(in[inPos+1]) * 256 + toUnsignedByte(in[inPos+2]));
	    
	    out[outPos+3] = map64[threeBytes % 64]; threeBytes /= 64;
	    out[outPos+2] = map64[threeBytes % 64]; threeBytes /= 64;
	    out[outPos+1] = map64[threeBytes % 64]; threeBytes /= 64;
	    out[outPos] = map64[threeBytes % 64];

	    inPos += 3;
	    outPos += 4;

        }

        if (inPos == in.length - 2) {

            threeBytes = (toUnsignedByte(in[inPos]) * 256 * 256 + toUnsignedByte(in[inPos+1]) * 256);
	    
	    out[outPos+3] = '='; threeBytes /= 64;
	    out[outPos+2] = map64[threeBytes % 64]; threeBytes /= 64;
	    out[outPos+1] = map64[threeBytes % 64]; threeBytes /= 64;
	    out[outPos] = map64[threeBytes % 64];

        } else if (inPos == in.length - 1) {

            threeBytes = (toUnsignedByte(in[inPos]) * 256 * 256);
	    
	    out[outPos+3] = '='; threeBytes /= 64;
	    out[outPos+2] = '='; threeBytes /= 64;
	    out[outPos+1] = map64[threeBytes % 64]; threeBytes /= 64;
	    out[outPos] = map64[threeBytes % 64];

        }
        return new String(out);
    }
}
