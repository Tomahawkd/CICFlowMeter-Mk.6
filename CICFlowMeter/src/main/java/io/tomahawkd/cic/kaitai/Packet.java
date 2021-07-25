package io.tomahawkd.cic.kaitai;

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStream;
import io.kaitai.struct.KaitaiStruct;

/**
 * @see <a href="https://wiki.wireshark.org/Development/LibpcapFileFormat#Record_.28Packet.29_Header">Source</a>
 */
public class Packet extends KaitaiStruct {

    /**
     * The date and time when this packet was captured. This value is in seconds since January 1, 1970 00:00:00 GMT.
     */
    private long tsSec;

    /**
     * the microseconds when this packet was captured, as an offset to ts_sec.
     */
    private long tsUsec;

    /**
     * the number of bytes of packet data actually captured and saved in the file.
     * This value should never become larger than orig_len or the snaplen value of the global header.
     */
    private long inclLen;

    /**
     * the length in bytes of the packet as it appeared on the network when it was captured.
     * If incl_len and orig_len differ, the actually saved packet size was limited by snaplen.
     */
    private long origLen;

    /**
     * Parsed Packet body/content
     */
    private Object body;
    private byte[] _raw_body;

    private final Pcap _root;
    private final Pcap _parent;

    public Packet(KaitaiStream _io, Pcap _parent, Pcap _root) {
        super(_io);
        this._parent = _parent;
        this._root = _root;
        _read();
    }

    private void _read() {
        this.tsSec = this._io.readU4le();
        this.tsUsec = this._io.readU4le();
        this.inclLen = this._io.readU4le();
        this.origLen = this._io.readU4le();
        {
            Pcap.Linktype on = _root.hdr().network();
            if (on != null) {
                if (_root.hdr().network() == Pcap.Linktype.ETHERNET) {
                    this._raw_body = this._io.readBytes(inclLen());
                    KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                    this.body = new EthernetFrame(_io__raw_body);
                } else {
                    this.body = this._io.readBytes(inclLen());
                }
            } else {
                this.body = this._io.readBytes(inclLen());
            }
        }
    }

    public boolean isEthernetPacket() {
        return _root.hdr().network() == Pcap.Linktype.ETHERNET;
    }

    public EthernetFrame getEthernetPacket() {
        if (isEthernetPacket()) return (EthernetFrame) this.body;
        else return null;
    }

    /**
     * @see Packet#tsSec
     */
    public long tsSec() {
        return tsSec;
    }

    /**
     * @see Packet#tsUsec
     */
    public long tsUsec() {
        return tsUsec;
    }

    /**
     * Number of bytes of packet data actually captured and saved in the file.
     */
    public long inclLen() {
        return inclLen;
    }

    /**
     * Length of the packet as it appeared on the network when it was captured.
     */
    public long origLen() {
        return origLen;
    }

    /**
     * @see <a href="https://wiki.wireshark.org/Development/LibpcapFileFormat#Packet_Data">Source</a>
     */
    public Object body() {
        return body;
    }

    public Pcap _parent() {
        return _parent;
    }

    public byte[] _raw_body() {
        return _raw_body;
    }
}