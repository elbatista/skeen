package util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ArgsParser {
    private Option log, id, numMsgs, clientCount, duration, tpcc, numPartitions, 
    locality, homewarehouse, region, algorithm, tree, gc, payload, tt, localMsgs, reconfigClient;
    private Options options;
    private CommandLineParser parser;
    private CommandLine line;
    private String command;

    private ArgsParser() {
        id = Option.builder("i").desc("server id").argName("id").hasArg().numberOfArgs(1).type(Short.class).build();
        algorithm = Option.builder("a").desc("algorithm").argName("algorithm").hasArg().numberOfArgs(1).type(Short.class).build();
        clientCount = Option.builder("c").desc("total number of clients").argName("clients").hasArg().numberOfArgs(1).type(Short.class).build();
        numMsgs = Option.builder("m").desc("number of messages each client send (defaults to -1)").argName("messages").hasArg().numberOfArgs(1).type(Integer.class).build();
        duration = Option.builder("d").desc("time to execute in seconds (defaults to 120)").argName("seconds").hasArg().numberOfArgs(1).type(Short.class).build();
        tpcc = Option.builder("t").desc("tpcc workload").argName("tpcc").type(Boolean.class).build();
        log = Option.builder("log").desc("log").argName("log").type(Boolean.class).build();
        payload = Option.builder("payload").desc("payload").argName("payload").type(Boolean.class).build();
        numPartitions = Option.builder("np").desc("max number of partitions per request (defaults to 10)").argName("#partitions").hasArg().numberOfArgs(1).type(Short.class).build();
        tt = Option.builder("tt").desc("tt").argName("tt").type(Boolean.class).build();
        localMsgs = Option.builder("localMsgs").desc("localMsgs").argName("localMsgs").type(Boolean.class).build();
        locality = Option.builder("l").desc("with locality").argName("locality").hasArg().numberOfArgs(1).type(Integer.class).build();
        homewarehouse = Option.builder("w").desc("home warehouse").argName("homewarehouse").hasArg().numberOfArgs(1).type(Integer.class).build();
        region = Option.builder("r").desc("region").argName("region").hasArg().numberOfArgs(1).type(String.class).build();
        tree = Option.builder("tree").desc("tree").argName("tree").hasArg().numberOfArgs(1).type(Short.class).build();
        gc = Option.builder("gc").desc("gc client").argName("gc").hasArg().numberOfArgs(1).type(Integer.class).build();
        reconfigClient = Option.builder("rc").desc("reconfig client").argName("rc").hasArg().numberOfArgs(1).type(Integer.class).build();
        options = new Options();
        parser = new DefaultParser();
        line = null;
    }

    public static ArgsParser getClientParser(String[] args) {
        ArgsParser parser = new ArgsParser();
        parser.command = "Client";
        parser.id.setRequired(true);
        parser.clientCount.setRequired(true);
        // parser.algorithm.setRequired(true);
        parser.options.addOption(parser.id);
        parser.options.addOption(parser.algorithm);
        parser.options.addOption(parser.clientCount);
        parser.options.addOption(parser.duration);
        parser.options.addOption(parser.numMsgs);
        parser.options.addOption(parser.tpcc);
        parser.options.addOption(parser.numPartitions);
        parser.options.addOption(parser.locality);
        parser.options.addOption(parser.homewarehouse);
        parser.options.addOption(parser.region);
        parser.options.addOption(parser.tree);
        parser.options.addOption(parser.log);
        parser.options.addOption(parser.gc);
        parser.options.addOption(parser.payload);
        parser.options.addOption(parser.tt);
        parser.options.addOption(parser.localMsgs);
        parser.options.addOption(parser.reconfigClient);
        parser.parse(args);
        return parser;
    }

    public static ArgsParser getServerParser(String[] args) {
        ArgsParser parser = new ArgsParser();
        parser.command = "Server";
        parser.id.setRequired(true);
        parser.clientCount.setRequired(true);
        // parser.algorithm.setRequired(true);
        parser.options.addOption(parser.id);
        parser.options.addOption(parser.algorithm);
        parser.options.addOption(parser.clientCount);
        parser.options.addOption(parser.duration);
        parser.options.addOption(parser.log);
        parser.options.addOption(parser.tree);
        parser.options.addOption(parser.payload);
        parser.parse(args);
        return parser;
    }

    public short getId() {
        return Short.valueOf(line.getOptionValue("i"));
    }

    public short getAlgorithm() {
        return Short.valueOf(line.getOptionValue("a"));
    }

    public int getNumMessages() {
        String v = line.getOptionValue("m");
        return v == null ? -1 : Integer.parseInt(v);
    }

    public short getClientCount() {
        String v = line.getOptionValue("c");
        return v == null ? 1 : Short.valueOf(v);
    }

    public short getDuration() {
        String v = line.getOptionValue("d");
        return v == null ? 120 : Short.valueOf(v);
    }

    public boolean isTpcc() {
        return line.hasOption("t");
    }

    public int getReconfigClient() {
        String v = line.getOptionValue("rc");
        return v == null ? -1 : Integer.valueOf(v);
    }

    public int getGC() {
        String v = line.getOptionValue("gc");
        return v == null ? -1 : Integer.valueOf(v);
    }

    public boolean getLog() {
        return line.hasOption("log");
    }

    public boolean shouldSendPayload() {
        return line.hasOption("payload");
    }

    public boolean thinkTime() {
        return line.hasOption("tt");
    }

        public boolean includeLocalMsgs() {
        return line.hasOption("localMsgs");
    }

    public short getNumPartitions() {
        String v = line.getOptionValue("np");
        return v == null ? 0 : Short.valueOf(v);
    }

    public int getLocality(){
        String v = line.getOptionValue("l");
        return v == null ? 0 : Integer.valueOf(v);
    }

    public int getHomeWarehouse(){
        String v = line.getOptionValue("w");
        return v == null ? 0 : Integer.valueOf(v);
    }

    public String getRegion(){
        String v = line.getOptionValue("r");
        return v == null ? "" : v;
    }

    public int getTree() {
        String v = line.getOptionValue("tree");
        return v == null ? 1 : Integer.valueOf(v);
    }

    public int getDAGTop() {
        String v = line.getOptionValue("tree");
        return v == null ? 1 : Integer.valueOf(v);
    }

    private void parse(String args[]) {
        try {
            line = parser.parse(options, args, false);
        }
        catch (ParseException exp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(command, options);
            System.err.println("Fail to parse command line options!");
            System.err.println(exp.getMessage());
            System.exit(-1);
        }
    }
}
