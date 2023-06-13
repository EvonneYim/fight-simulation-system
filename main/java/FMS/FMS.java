package FMS;

import Actuators.EngineActuator;
import Actuators.LandingGearActuator;
import Actuators.TailFlapActuator;
import Actuators.WingFlapActuator;
import Actuators.OxygenMaskActuator;

import Sensors.AltitudeSensor;
import Sensors.SpeedSensor;
import Sensors.WeatherSensor;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import Sensors.CabinSensor;
import java.util.concurrent.ExecutionException;

public class FMS {

    public static ScheduledExecutorService fms;

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException, ExecutionException {
        ConnectionFactory cf = new ConnectionFactory();
        Connection con = cf.newConnection();

        fms = Executors.newScheduledThreadPool(1);

        fms.scheduleAtFixedRate(new Controller(con), 0, 5, TimeUnit.SECONDS);
        fms.scheduleAtFixedRate(new WingFlapActuator(con), 0, 1, TimeUnit.SECONDS);
        fms.scheduleAtFixedRate(new EngineActuator(con), 0, 1, TimeUnit.SECONDS);
        fms.scheduleAtFixedRate(new TailFlapActuator(con), 0, 1, TimeUnit.SECONDS);
        fms.scheduleAtFixedRate(new OxygenMaskActuator(con), 0, 1, TimeUnit.SECONDS);
        fms.scheduleAtFixedRate(new LandingGearActuator(con), 0, 1, TimeUnit.SECONDS);

        fms.scheduleAtFixedRate(new CabinSensor(con), 0, 1, TimeUnit.SECONDS);
        fms.scheduleAtFixedRate(new AltitudeSensor(con), 0, 1, TimeUnit.SECONDS);
        fms.scheduleAtFixedRate(new SpeedSensor(con), 0, 2, TimeUnit.SECONDS);
        fms.scheduleAtFixedRate(new WeatherSensor(con), 0, 2, TimeUnit.SECONDS);

//            Future<CabinLoss> futureCabinLoss = fms.schedule(new Logic(new CabinLoss()), 10, TimeUnit.SECONDS);
//            fms.scheduleAtFixedRate(new AltitudeSensor(con, futureCabinLoss), 0, 2, TimeUnit.SECONDS);
//            fms.scheduleAtFixedRate(new CabinSensor(con, futureCabinLoss), 0, 2, TimeUnit.SECONDS);

    }
}
