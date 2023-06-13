package Sensors;

import Actuators.LandingGearActuator;
import FMS.CabinLoss;
import FMS.Controller;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AltitudeSensor implements Runnable {

    Random rand = new Random();

    //exchange from sensor to controller
    String altitudeDirectExchange = "altitudeDirectExchange";

    //send message to controller
    String altitude_to_controller_key = "altitude_to_controller";
    String altitude_to_controller_landing_key = "altitude_to_controller_landing";

    //exchange from controller to sensors
    String controllerDirectExchangeToWingFlaps = "controllerDirectExchangeToWingFlaps";
    String controllerDirectExchangeToTailFlaps = "controllerDirectExchangeToTailFlaps";

    //receive message from controller
    String controller_to_altitude_key = "controller_to_altitude";
    String controller_to_altitude_weather_key = "controller_to_altitude_weather";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel wingChannel;
    Channel tailChannel;

    String altitude;
   
    int finalAltitude;
    Boolean altitudeLoss = false;
    //Future<FMS.CabinLoss> futureCabinLoss;
    
    public start start = new start();
    public static long storedStart;
    
    public static ArrayList <Long> startTime = new ArrayList<Long>();

    public AltitudeSensor(Connection con) throws IOException, TimeoutException {
        this.con = con;

        wingChannel = con.createChannel();
        tailChannel = con.createChannel();

        wingChannel.exchangeDeclare(controllerDirectExchangeToWingFlaps, "direct");
        tailChannel.exchangeDeclare(controllerDirectExchangeToTailFlaps, "direct");

        String qName_altitude_wing = wingChannel.queueDeclare().getQueue();
        String qName_altitude_tail = tailChannel.queueDeclare().getQueue();
        String qName_weather_wing = wingChannel.queueDeclare().getQueue();
        String qName_weather_tail = tailChannel.queueDeclare().getQueue();

        wingChannel.queueBind(qName_altitude_wing, controllerDirectExchangeToWingFlaps, controller_to_altitude_key);
        tailChannel.queueBind(qName_altitude_tail, controllerDirectExchangeToTailFlaps, controller_to_altitude_key);
        wingChannel.queueBind(qName_weather_wing, controllerDirectExchangeToWingFlaps, controller_to_altitude_weather_key);
        tailChannel.queueBind(qName_weather_tail, controllerDirectExchangeToTailFlaps, controller_to_altitude_weather_key);

        receiveMsgFromControllerWingAltitude(qName_altitude_wing);
        receiveMsgFromControllerTailSpeed(qName_altitude_tail);
        receiveMsgFromControllerWingWeather(qName_weather_wing);
        receiveMsgFromControllerTailWeather(qName_weather_tail);

    }

//    public AltitudeSensor(Connection con, Future<CabinLoss> futureCabinLoss) {
//        this.con = con;
//        this.futureCabinLoss = futureCabinLoss;
//    }

    @Override
    public void run() {      
        
        if(LandingGearActuator.landingMode == true){
            return;
        }
        
        start.setStart(new Date().getTime());
        startTime.add(start.getStart()) ;     
        altitude = String.valueOf(getAltitudeReadng());

        System.out.println("");
        System.out.println("ALTITUDE: detected the following altitude above sea level: " + altitude);
        sendMsgToController(altitude);

        if (Controller.cabinLoss == true) {
            altitudeLoss = Controller.cabinLoss;
        }
        

        
        
    }

    public int getAltitudeReadng() {
        int start = 4000;
        if (altitudeLoss == false) {

            int change = rand.nextInt(300) + 100;
            if (rand.nextBoolean()) {
                change *= -1;
            }
            start += change;
        } else {
            int change = -(rand.nextInt(300) + 500);
            start += change;
        }
        return start;
    }

    public void sendMsgToController(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(altitudeDirectExchange, "direct");  //name,type
            chan.basicPublish(altitudeDirectExchange, altitude_to_controller_key, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(AltitudeSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void decrementAltitude(int finalAltitude) {

        while (finalAltitude > 0) {
            finalAltitude = Math.max(0, finalAltitude - (rand.nextInt(50) + 200));
            System.out.println("ALTITUDE:...the latest altitude above sea level is " + finalAltitude);
            sendMsgToControllerLanding(String.valueOf(finalAltitude));
        }

    }

    public void receiveMsgFromControllerWingAltitude(String qName) {

        try {
            wingChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");

                if (Integer.parseInt(message) != 0) {
                    System.out.println("ALTITUDE:...the latest altitude above sea level is " + message + " due to wing flap adjustments");
                    finalAltitude = Integer.parseInt(message);
                } else {
                    decrementAltitude(finalAltitude);
                }

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

    public void receiveMsgFromControllerTailSpeed(String qName) {
        try {
            wingChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("ALTITUDE:...the latest altitude above sea level is " + message 
                        + " due to tail flap adjustments");
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

    public void receiveMsgFromControllerWingWeather(String qName) {
        try {
            wingChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("ALTITUDE: (WEATHER) ALERT! ...the latest altitude above sea level is " + message + " due to wing flap adjustments");
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

    public void receiveMsgFromControllerTailWeather(String qName) {
        try {
            tailChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("ALTITUDE: (WEATHER) ALERT! ...the latest altitude above sea level is " + message + " due to tail flap adjustments");
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

    public void sendMsgToControllerLanding(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(altitudeDirectExchange, "direct");  //name,type
            chan.basicPublish(altitudeDirectExchange, altitude_to_controller_landing_key, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(AltitudeSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    class start{
        long start;

        public long getStart() {
            return start;
        }

        public void setStart(long start) {
            this.start = start;
        }
        
        
    }
}
