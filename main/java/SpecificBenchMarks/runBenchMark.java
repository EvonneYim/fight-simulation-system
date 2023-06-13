package SpecificBenchMarks;


import FMS.FMS;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class runBenchMark {

    public static void main(String[] args) throws IOException, TimeoutException {
        //org.openjdk.jmh.Main.main(new String[]{"-i", "2", "-wi", "1", "-f", "1", "-t", "4", "SpeedSensorBenchmark"});
        //FMS fms = new FMS();

        //org.openjdk.jmh.Main.main(FMS.main(args));
        org.openjdk.jmh.Main.main(getBenchmarkArgs());
        
    }

    public static String[] getBenchmarkArgs() {
        return new String[]{"-i", "2", "-wi", "1", "-f", "1", "-t", "4", "SpeedSensorBenchmark"};
    }

}
