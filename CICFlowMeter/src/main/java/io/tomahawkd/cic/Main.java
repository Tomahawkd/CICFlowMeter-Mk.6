package io.tomahawkd.cic;

import io.tomahawkd.cic.config.CommandlineDelegate;
import io.tomahawkd.cic.data.PacketInfo;
import io.tomahawkd.cic.flow.FlowFeatureTag;
import io.tomahawkd.cic.flow.FlowGenerator;
import io.tomahawkd.cic.util.PacketReader;
import io.tomahawkd.cic.util.Utils;
import io.tomahawkd.config.ConfigManager;
import io.tomahawkd.config.commandline.CommandlineConfig;
import io.tomahawkd.config.commandline.CommandlineConfigSource;
import io.tomahawkd.config.sources.SourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jnetpcap.PcapClosedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

    public static final Logger logger = LogManager.getLogger(Main.class);
    private static final String DividingLine = "-------------------------------------------------------------------------------";

    public static void main(String[] args) {
        SourceManager sourceManager = SourceManager.get();
        ConfigManager configManager = ConfigManager.get();

        sourceManager.getSource(CommandlineConfigSource.class).setData(args);
        configManager.parse();

        CommandlineDelegate delegate = configManager.getDelegateByType(CommandlineDelegate.class);
        assert delegate != null;
        if (delegate.isHelp()) {
            System.out.println(Objects.requireNonNull(configManager.getConfig(CommandlineConfig.class)).usage());
            return;
        }
        long flowTimeout = delegate.getFlowTimeout();
        long activityTimeout = delegate.getActivityTimeout();
        List<Path> pcapPath = delegate.getPcapPath();
        Path outPath = delegate.getOutputPath();

        pcapPath.forEach(p -> {
            logger.info("Start Processing {}", p.getFileName().toString());
            readPcapFile(p, outPath, flowTimeout, activityTimeout);
        });
    }

    private static void readPcapFile(Path inputFile, Path outPath, long flowTimeout, long activityTimeout) {
        if (inputFile == null || outPath == null) {
            return;
        }

        String fileName = inputFile.getFileName().toString();
        Path saveFileFullPath = outPath.resolve(fileName + Utils.FLOW_SUFFIX);
        if (Files.exists(saveFileFullPath)) {
            try {
                Files.delete(saveFileFullPath);
            } catch (IOException e) {
                logger.warn("Save file {} can not be deleted.", saveFileFullPath.toString(), e);
            }
        }

        try {
            Files.createFile(saveFileFullPath);
        } catch (IOException e) {
            logger.fatal("Failed to create file");
            throw new RuntimeException(e);
        }
        System.out.printf("Working on... %s%n", fileName);

        // setting up
        FlowGenerator flowGen = new FlowGenerator(flowTimeout, activityTimeout);

        // This is hard-coded
        if (inputFile.getFileName().toString().contains("Wednesday-WorkingHours")) {
            // 172.16.0.1 -> 192.168.10.50:80
            flowGen.setFlowLabelSupplier(f -> {
                if (f.getSrc().equals("172.16.0.1") && f.getDst().equals("192.168.10.50") && f.getDstPort() == 80) {
                    return "SLOWDOS";
                } else return "NORMAL";
            });
        } else if (inputFile.getFileName().toString().contains("Friday-WorkingHours")) {
            flowGen.setFlowLabelSupplier(f -> {
                if (f.getSrc().equals("172.16.0.1") && f.getDst().equals("192.168.10.50") && f.getDstPort() == 80) {
                    return "DOS";
                } else return "NORMAL";
            });
        } else {
            flowGen.setFlowLabelSupplier(f -> "NONE");
        }

        // counter
        AtomicLong flowCount = new AtomicLong(0);
        flowGen.addFlowListener(flow -> flowCount.incrementAndGet());
        // data export
        flowGen.addFlowListener(flow ->
                Utils.insertToFile(FlowFeatureTag.getHeader(), flow.exportData(), saveFileFullPath));

        PacketReader packetReader = new PacketReader(inputFile.toString());
        long nTotal = 0;
        long nValid = 0;
        while (true) {
            try {
                PacketInfo basicPacket = packetReader.nextPacket();
                nTotal++;
                if (basicPacket != null) {
                    flowGen.addPacket(basicPacket);
                    nValid++;
                }

                System.out.printf("%s -> %d packets, %d flows \r", fileName, nTotal, flowCount.get());
            } catch (PcapClosedException e) {
                break;
            }
        }

        flowGen.dumpLabeledCurrentFlow(saveFileFullPath);
        long lines = Utils.countLines(saveFileFullPath);

        System.out.printf("%s is done. total %d flows %n", fileName, lines);
        System.out.printf("Packet stats: Total=%d,Valid=%d,Discarded=%d%n", nTotal, nValid, nTotal - nValid);
        System.out.println(DividingLine);
    }
}
