package jenkinsci.plugins.influxdb.generators;

public class TimeGenerator {
    private final long currentTime;
    private long nanoOffSet = 0;

    TimeGenerator(long currentTime) {
        this.currentTime = currentTime;
    }

    public long next() {
        nanoOffSet++;
        return currentTime + nanoOffSet;
    }
}