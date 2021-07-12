package io.tomahawkd.cic.packet;

public enum MetaFeature implements PacketFeature {

    // Protocols
    IPV4(Boolean.class), IPV6(Boolean.class), TCP(Boolean.class), UDP(Boolean.class), HTTP(Boolean.class),

    // Src and Dst information
    SRC(byte[].class), DST(byte[].class), SRC_PORT(Integer.class), DST_PORT(Integer.class),
    PAYLOAD_LEN(Integer.class), HEADER_LEN(Integer.class);

    private final Class<?> type;

    MetaFeature(Class<?> type) {
        this.type = type;
    }

    @Override
    public Class<?> getType() {
        return type;
    }
}