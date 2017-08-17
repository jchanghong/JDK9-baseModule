/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.security;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.Pattern;
import sun.security.util.*;


public final class PKCS12Attribute implements KeyStore.Entry.Attribute {

    private static final Pattern COLON_SEPARATED_HEX_PAIRS =
        Pattern.compile("^[0-9a-fA-F]{2}(:[0-9a-fA-F]{2})+$");
    private String name;
    private String value;
    private byte[] encoded;
    private int hashValue = -1;


    public PKCS12Attribute(String name, String value) {
        if (name == null || value == null) {
            throw new NullPointerException();
        }
        // Validate name
        ObjectIdentifier type;
        try {
            type = new ObjectIdentifier(name);
        } catch (IOException e) {
            throw new IllegalArgumentException("Incorrect format: name", e);
        }
        this.name = name;

        // Validate value
        int length = value.length();
        String[] values;
        if (value.charAt(0) == '[' && value.charAt(length - 1) == ']') {
            values = value.substring(1, length - 1).split(", ");
        } else {
            values = new String[]{ value };
        }
        this.value = value;

        try {
            this.encoded = encode(type, values);
        } catch (IOException e) {
            throw new IllegalArgumentException("Incorrect format: value", e);
        }
    }


    public PKCS12Attribute(byte[] encoded) {
        if (encoded == null) {
            throw new NullPointerException();
        }
        this.encoded = encoded.clone();

        try {
            parse(encoded);
        } catch (IOException e) {
            throw new IllegalArgumentException("Incorrect format: encoded", e);
        }
    }


    @Override
    public String getName() {
        return name;
    }


    @Override
    public String getValue() {
        return value;
    }


    public byte[] getEncoded() {
        return encoded.clone();
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PKCS12Attribute)) {
            return false;
        }
        return Arrays.equals(encoded, ((PKCS12Attribute) obj).getEncoded());
    }


    @Override
    public int hashCode() {
        if (hashValue == -1) {
            Arrays.hashCode(encoded);
        }
        return hashValue;
    }


    @Override
    public String toString() {
        return (name + "=" + value);
    }

    private byte[] encode(ObjectIdentifier type, String[] values)
            throws IOException {
        DerOutputStream attribute = new DerOutputStream();
        attribute.putOID(type);
        DerOutputStream attrContent = new DerOutputStream();
        for (String value : values) {
            if (COLON_SEPARATED_HEX_PAIRS.matcher(value).matches()) {
                byte[] bytes =
                    new BigInteger(value.replace(":", ""), 16).toByteArray();
                if (bytes[0] == 0) {
                    bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
                }
                attrContent.putOctetString(bytes);
            } else {
                attrContent.putUTF8String(value);
            }
        }
        attribute.write(DerValue.tag_Set, attrContent);
        DerOutputStream attributeValue = new DerOutputStream();
        attributeValue.write(DerValue.tag_Sequence, attribute);

        return attributeValue.toByteArray();
    }

    private void parse(byte[] encoded) throws IOException {
        DerInputStream attributeValue = new DerInputStream(encoded);
        DerValue[] attrSeq = attributeValue.getSequence(2);
        ObjectIdentifier type = attrSeq[0].getOID();
        DerInputStream attrContent =
            new DerInputStream(attrSeq[1].toByteArray());
        DerValue[] attrValueSet = attrContent.getSet(1);
        String[] values = new String[attrValueSet.length];
        String printableString;
        for (int i = 0; i < attrValueSet.length; i++) {
            if (attrValueSet[i].tag == DerValue.tag_OctetString) {
                values[i] = Debug.toString(attrValueSet[i].getOctetString());
            } else if ((printableString = attrValueSet[i].getAsString())
                != null) {
                values[i] = printableString;
            } else if (attrValueSet[i].tag == DerValue.tag_ObjectId) {
                values[i] = attrValueSet[i].getOID().toString();
            } else if (attrValueSet[i].tag == DerValue.tag_GeneralizedTime) {
                values[i] = attrValueSet[i].getGeneralizedTime().toString();
            } else if (attrValueSet[i].tag == DerValue.tag_UtcTime) {
                values[i] = attrValueSet[i].getUTCTime().toString();
            } else if (attrValueSet[i].tag == DerValue.tag_Integer) {
                values[i] = attrValueSet[i].getBigInteger().toString();
            } else if (attrValueSet[i].tag == DerValue.tag_Boolean) {
                values[i] = String.valueOf(attrValueSet[i].getBoolean());
            } else {
                values[i] = Debug.toString(attrValueSet[i].getDataBytes());
            }
        }

        this.name = type.toString();
        this.value = values.length == 1 ? values[0] : Arrays.toString(values);
    }
}
