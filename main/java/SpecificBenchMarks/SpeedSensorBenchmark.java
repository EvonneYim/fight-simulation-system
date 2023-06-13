package SpecificBenchMarks;

import FMS.Controller;
import Sensors.SpeedSensor;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class SpeedSensorBenchmark {

    public Connection mockConnection;

    public int test(int x) {
        return x;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Measurement(iterations = 2, timeUnit = TimeUnit.SECONDS)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 1)
    @Fork(1)
    public void testReceiveMsgFromControllerAltitude(Blackhole hole) throws IOException, TimeoutException {
        int result = 0;
        for (int x = 0; x < 100; ++x) {
            result = test(x);
            SpeedSensor sensor = new SpeedSensor(mockConnection);
            sensor.receiveMsgFromControllerAltitude("amq.gen-ucWYCiqmqKS6ympqoxWlYA");
            hole.consume(result);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Measurement(iterations = 2, timeUnit = TimeUnit.SECONDS)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 1)
    @Fork(1)
    public void testReceiveMsgFromControllerEngine(Blackhole hole) throws IOException, TimeoutException {
        int result = 0;
        for (int x = 0; x < 100; ++x) {
            result = test(x);
            SpeedSensor sensor = new SpeedSensor(mockConnection);
            sensor.receiveMsgFromControllerEngine("amq.gen-w3MpAnsbyx3mMdz5Ogh6HA");
            hole.consume(result);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Measurement(iterations = 2, timeUnit = TimeUnit.SECONDS)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 1)
    @Fork(1)
    public void testReceiveMsgFromEnginesSpeed() throws IOException, TimeoutException {
        Controller controller = new Controller(mockConnection);
        controller.receiveMsgFromEnginesSpeed("amq.gen-s8HkKwXhkWJUXsI0Lq1nMw");

    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Measurement(iterations = 2, timeUnit = TimeUnit.SECONDS)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 1)
    @Fork(1)
    public void testReceiveMsgFromEnginesAltitude() throws IOException, TimeoutException {
        Controller controller = new Controller(mockConnection);
        controller.receiveMsgFromEnginesAltitude("amq.gen-i4CV_sR3BsFGVPI5k6uJKw");
    }

}
