package Sensors;

import Actuators.LandingGearActuator;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpeedSensor implements Runnable {

    SecureRandom rand = new SecureRandom();

    //exchange from sensor to controller
    String speedDirectExchange = "speedDirectExchange";

    //exchange from controller to sensor
    String controllerDirectExchangeToEngines = "controllerDirectExchangeToEngines";

    //receive message from controller
    String controller_to_speed_key = "controller_to_speed";
    String controller_to_altitude_key = "controller_to_altitude";

    //send message to controller
    String speed_to_controller_key = "speed_to_controller";
    String speed_to_controller_landing_key = "altitude_to_controller_landing";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch;

    int storedSpeed;

    public SpeedSensor(Connection con) throws IOException, TimeoutException {
        this.con = con;
        if (con != null) {
            
            ch = con.createChannel();

            ch.exchangeDeclare(controllerDirectExchangeToEngines, "direct");
            ch.exchangeDeclare(controllerDirectExchangeToEngines, "direct");

            String qName_altitude = ch.queueDeclare().getQueue();
            String qName_speed = ch.queueDeclare().getQueue();

            //System.out.println(qName1);
            ch.queueBind(qName_altitude, controllerDirectExchangeToEngines, controller_to_altitude_key);
            ch.queueBind(qName_speed, controllerDirectExchangeToEngines, controller_to_speed_key);

            receiveMsgFromControllerAltitude(qName_altitude);
            receiveMsgFromControllerEngine(qName_speed);

        }
    }

    @Override

    public void run() {
        
         if(LandingGearActuator.landingMode == true){
            return;
        }
         
        int speed = getSpeedReading();

        System.out.println("SPEED: detected the following speed: " + speed);
        sendMsgToController(Integer.toString(speed));
        


        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    //@Benchmark
    public int getSpeedReading() {

        int speed = 44;
        int change = rand.nextInt(22);

        if (rand.nextBoolean()) {
            change *= -1;
        }
        speed += change;

        return speed;
    }

    public void decrementSpeed(int storedSpeed) {
        while (storedSpeed > 0) {
            try {
                storedSpeed = Math.max(0, storedSpeed - 1);
                System.out.println("SPEED:...latest speed change is " + storedSpeed);
                sendMsgToControllerLanding(String.valueOf(storedSpeed));

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (IOException ex) {
                Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

//        for (int i = storedSpeed; i >= 0; i--) {
//            try {
//                System.out.println("SPEED:...latest speed change is " + i);
//                sendMsgToControllerLanding(String.valueOf(i));
//
//                Thread.sleep(200);
//            } catch (IOException ex) {
//                Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }

    }

    public void receiveMsgFromControllerAltitude(String qName) {
        if (con != null) {
            try {
                ch.basicConsume(qName, true, (x, msg) -> {
                    String message = new String(msg.getBody(), "UTF-8");

                    if (Integer.parseInt(message) != 0) {
                        storedSpeed = Integer.parseInt(message);
                        System.out.println("SPEED:...latest speed change from altitude impact: " + message);
                    } else {

                        decrementSpeed(storedSpeed);
                    }

                }, x -> {
                });
            } catch (IOException ex) {
                Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void receiveMsgFromControllerEngine(String qName) {
        if (con != null) {
            try {
                ch.basicConsume(qName, true, (x, msg) -> {
                    String message = new String(msg.getBody(), "UTF-8");
                    System.out.println("SPEED:...latest speed change from engine impact: " + message);

                }, x -> {
                });
            } catch (IOException ex) {
                Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void sendMsgToController(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(speedDirectExchange, "direct");  //name,type
            chan.basicPublish(speedDirectExchange, speed_to_controller_key, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToControllerLanding(String msg) throws IOException {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(speedDirectExchange, "direct");  //name,type
            chan.basicPublish(speedDirectExchange, speed_to_controller_landing_key, false, null, msg.getBytes());

        } catch (IOException ex) {
            Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(SpeedSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
