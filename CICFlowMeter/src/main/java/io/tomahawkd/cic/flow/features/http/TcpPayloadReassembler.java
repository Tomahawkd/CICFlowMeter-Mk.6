package io.tomahawkd.cic.flow.features.http;

import io.tomahawkd.cic.packet.HttpPacketDelegate;
import io.tomahawkd.cic.packet.MetaFeature;
import io.tomahawkd.cic.packet.PacketInfo;
import io.tomahawkd.cic.packet.UnknownAppLayerPacketDelegate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class TcpPayloadReassembler {

    private static final Logger logger = LogManager.getLogger(TcpPayloadReassembler.class);

    private final Map<Long, String> segmentMap_fwd = new HashMap<>();
    private final Map<Long, String> segmentMap_bwd = new HashMap<>();

    public void addIncompletePacket(PacketInfo info, boolean fwd) {
        Map<Long, String> map = fwd ? segmentMap_fwd : segmentMap_bwd;

        long expectNextSeq = info.seq() + info.getPayloadBytes();
        String incompleteString = info.getFeature(HttpPacketDelegate.Feature.INCOM_SEGMENT, String.class);
        if (incompleteString == null || incompleteString.isEmpty()) {
            logger.warn("The first HTTP header segment is empty.");
        }

        map.put(expectNextSeq, incompleteString);
    }

    public boolean canCompleteIncompletePacket(PacketInfo info, boolean fwd) {
        Map<Long, String> map = fwd ? segmentMap_fwd : segmentMap_bwd;
        String incompleteString = map.get(info.seq());
        if (incompleteString == null) return false;

        if (Optional.ofNullable(info.getFeature(MetaFeature.HTTP, Boolean.class)).orElse(false)) {
            logger.warn("This is the first packet of HTTP data");
            return false;
        }

        String readableString = info.getFeature(UnknownAppLayerPacketDelegate.Feature.PAYLOAD, String.class);
        if (readableString == null || readableString.isEmpty()) {
            logger.warn("The HTTP header segment (not first) is empty.");
        }
        incompleteString += readableString;

        // terminate by CRLF * 2, that is, the header ends
        if (Optional.ofNullable(
                        info.getFeature(UnknownAppLayerPacketDelegate.Feature.CRLF, Boolean.class)).orElse(false)) {
            return HttpPacketDelegate.parseFeatures(info, incompleteString, true);
        }

        long expectNextSeq = info.seq() + info.getPayloadBytes();
        map.remove(info.seq());
        map.put(expectNextSeq, incompleteString);
        return false;
    }

    public boolean isEmpty(boolean fwd) {
        return fwd ? segmentMap_fwd.isEmpty() : segmentMap_bwd.isEmpty();
    }

    public boolean isEmpty() {
        return segmentMap_fwd.isEmpty() && segmentMap_bwd.isEmpty();
    }

    public void cleanIncompletePackets(Consumer<PacketInfo> function) {
        // These packets are not generated by the real reader
        // so that they have no underlying features such as TCP and IP
        // Therefore, the subclass of AbstractHttpFeature MUST ONLY use HTTP features

        for (String incompHeader : segmentMap_fwd.values()) {
            PacketInfo info = new PacketInfo(-1);
            if (HttpPacketDelegate.parseFeatures(info, incompHeader, true)) {
                function.accept(info);
            }
        }

        for (String incompHeader : segmentMap_bwd.values()) {
            PacketInfo info = new PacketInfo(-1);
            if (HttpPacketDelegate.parseFeatures(info, incompHeader, true)) {
                function.accept(info);
            }
        }
    }
}
