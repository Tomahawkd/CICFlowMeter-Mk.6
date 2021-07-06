package io.tomahawkd.cic.flow.features;

import io.tomahawkd.cic.data.PacketInfo;
import io.tomahawkd.cic.util.DateFormatter;
import io.tomahawkd.cic.util.FlowLabelSupplier;
import org.jnetpcap.packet.format.FormatUtils;

import java.util.Arrays;

@Feature(name = "FlowBasicFeature", tags = {
        FlowFeatureTag.fid,
        FlowFeatureTag.src_ip,
        FlowFeatureTag.src_port,
        FlowFeatureTag.dst_ip,
        FlowFeatureTag.dst_port,
        FlowFeatureTag.tstp,
        FlowFeatureTag.fl_dur
}, manual = true)
public class FlowBasicFeature extends AbstractFlowFeature {

    private final String flowId;
    private final byte[] src;
    private final byte[] dst;
    private final int srcPort;
    private final int dstPort;

    private final FlowLabelSupplier supplier;

    private long flowStartTime = -1L;
    private long flowLastSeen = 0L;

    // settings
    private final long flowActivityTimeOut;

    public FlowBasicFeature(String flowId,
                            byte[] src, byte[] dst,
                            int srcPort, int dstPort,
                            FlowLabelSupplier supplier,
                            long flowActivityTimeOut) {
        super(null);

        this.flowId = flowId;
        this.src = Arrays.copyOf(src, src.length);
        this.dst = Arrays.copyOf(dst, dst.length);
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.supplier = supplier;
        this.flowActivityTimeOut = flowActivityTimeOut;
    }

    @Override
    public void addPacket(PacketInfo info, boolean fwd) {

    }

    @Override
    public void postAddPacket(PacketInfo info) {
        // first packet
        if (flowStartTime == -1L) {
            flowStartTime = info.getTimestamp();
        }
        flowLastSeen = info.getTimestamp();
    }

    @Override
    public String exportData() {
        return getFlowId() + "," +
                getSrc() + "," +
                getSrcPort() + "," +
                getDst() + "," +
                getDstPort() + "," +
                getTimestamp() + "," +
                getDuration() + ",";
    }

    public String getFlowId() {
        return flowId;
    }

    public byte[] src() {
        return src;
    }

    public byte[] dst() {
        return dst;
    }

    public String getSrc() {
        return FormatUtils.ip(src);
    }

    public String getDst() {
        return FormatUtils.ip(dst);
    }

    public int getSrcPort() {
        return srcPort;
    }

    public int getDstPort() {
        return dstPort;
    }

    public long getFlowStartTime() {
        return flowStartTime;
    }

    public String getTimestamp() {
        return DateFormatter.convertMilliseconds2String(flowStartTime / 1000L, "dd/MM/yyyy hh:mm:ss a");
    }

    public long getFlowLastSeen() {
        return flowLastSeen;
    }

    public long getFlowActivityTimeOut() {
        return flowActivityTimeOut;
    }

    public long getDuration() {
        return flowLastSeen - flowStartTime;
    }

    public FlowLabelSupplier getSupplier() {
        return supplier;
    }

    public void reset() {
        flowStartTime = -1L;
        flowLastSeen = 0L;
    }
}