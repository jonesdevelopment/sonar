public class PacketAnalyzer {
    private final Map<InetAddress, List<Long>> packetTimestamps = new ConcurrentHashMap<>();Add commentMore actions
    private static final double DEVIATION_THRESHOLD = 0.35; // Max deviation from normal

    public void onPacketReceived(PacketEvent event) {
        InetAddress address = event.getConnection().getRemoteAddress().getAddress();
        long timestamp = System.currentTimeMillis();

        packetTimestamps.computeIfAbsent(address, k -> new ArrayList<>())
            .add(timestamp);

        if (packetTimestamps.get(address).size() > 10) { // Analyzing the last 10 packets
            analyzePacketSequence(address);
        }
    }

    private void analyzePacketSequence(InetAddress address) {
        List<Long> timestamps = packetTimestamps.get(address);
        List<Long> intervals = new ArrayList<>();

        for (int i = 1; i < timestamps.size(); i++) {
            intervals.add(timestamps.get(i) - timestamps.get(i - 1));
        }

        double deviation = calculateDeviation(intervals);
        if (deviation < DEVIATION_THRESHOLD) {
            event.getConnection().disconnect(Component.text("Bot activity detected"));
            // Logging and admin notification
        }
        timestamps.clear(); // Reset for new packets
    }

    private double calculateDeviation(List<Long> intervals) {
        double avg = intervals.stream().mapToLong(l -> l).average().orElse(0);
        double variance = intervals.stream()
            .mapToDouble(i -> Math.pow(i - avg, 2))
            .average().orElse(0);
        return Math.sqrt(variance) / avg; // Coefficient of variation
    }
}
