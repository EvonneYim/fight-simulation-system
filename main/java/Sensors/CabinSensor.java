package Sensors;

import Actuators.LandingGearActuator;
import FMS.CabinLoss;
import FMS.Controller;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CabinSensor implements Runnable {

    Random rand = new Random();

    //exchange from sensor to controller
    String cabinDirectExchange = "cabinDirectExchange";

    //exchange from controller to sensor
    String controllerDirectExchangeToCabin = "controllerDirectExchangeToCabin";

    //send message to controller
    String cabin_to_controller_key = "cabin_to_controller";
    String cabin_to_controller_landing_key = "cabin_to_controller_landing";

    //receive message from controller
    String controller_to_cabin_key = "controller_to_cabin";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch;

    int initialPressure;
    Boolean pressureLoss = false;
    //Future<FMS.CabinLoss> futureCabinLoss;

    public CabinSensor(Connection con) throws IOException, TimeoutException {
        this.con = con;

        ch = con.createChannel();

        ch.exchangeDeclare(controllerDirectExchangeToCabin, "direct");

        String qName = ch.queueDeclare().getQueue();

        ch.queueBind(qName, controllerDirectExchangeToCabin, controller_to_cabin_key);

        receiveMsgFromController(qName);

    }

//    public CabinSensor(Connection con, Future<CabinLoss> futureCabinLoss) {
//        this.con = con;
//        this.futureCabinLoss = futureCabinLoss;
//    }

    @Override
    public void run() {
        
         if(LandingGearActuator.landingMode == true){
            return;
        }
         
        if (Controller.cabinLoss == true) {
            pressureLoss = Controller.cabinLoss;
        }
        
        int pressure = getPressureReading();
        initialPressure = pressure;

        System.out.println("CABIN: detected the following pressure reading: " + pressure);
        sendMsgToController(Integer.toString(pressure));
        

    }

    public int getPressureReading() {
        int start = 6000;
        if (pressureLoss == false) {
            int change = rand.nextInt(200) + 300;

            if (rand.nextBoolean()) {
                change *= -1;
            }
            start += change;
        } else {
            int change = -(rand.nextInt(200) + 800);
            start += change;         
        }
        return start;
    }

    public void sendMsgToController(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(cabinDirectExchange, "direct");  //name,type
            chan.basicPublish(cabinDirectExchange, cabin_to_controller_key, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(CabinSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void incrementPressure(int storedPressure) {
        
        while (storedPressure < 6000) {
            storedPressure += (rand.nextInt(100) + 300);
            
            System.out.println("CABIN:...the latest cabin pressure is " + storedPressure);
            sendMsgToControllerLanding(String.valueOf(storedPressure));
            
            if (storedPressure > 6000) {
                return;
            }        
        }
        }
        //assume when in landing mode, the cabin pressure goes back to normal
    
    public void sendMsgToControllerLanding(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(cabinDirectExchange, "direct");  //name,type
            chan.basicPublish(cabinDirectExchange, cabin_to_controller_landing_key, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(CabinSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromController(String qName) {
        try {
            ch.basicConsume(qName, true, (x, msg) -> {
                String landing = new String(msg.getBody(), "UTF-8");
                incrementPressure(Integer.parseInt(landing));

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(AltitudeSensor.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(AltitudeSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
