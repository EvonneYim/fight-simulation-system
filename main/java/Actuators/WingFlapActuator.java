package Actuators;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WingFlapActuator implements Runnable {

    //Random rand = new Random();
    SecureRandom rand = new SecureRandom();

    //exchange from Controller to WingFlaps
    String controllerDirectExchangeToWingFlaps = "controllerDirectExchangeToWingFlaps";

    //exchange from WingFlaps to Controller
    String wingFlapDirectExchange = "wingFlapDirectExchange";

    //receive message from controller
    String controller_to_wingflaps_altitude_key = "controller_to_wingflaps_altitude";
    String controller_to_wingflaps_weather_key = "controller_to_wingflaps_weather";

    //send message to controller
    String wingflaps_to_controller_altitude_key = "wingflaps_to_controller_altitude";
    String wingflaps_to_controller_weather_key = "wingflaps_to_controller_weather";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch1;
    wingFlap wf = new wingFlap();

    public WingFlapActuator(Connection con) throws IOException, TimeoutException {
        this.con = con;

        con = cf.newConnection();
        ch1 = con.createChannel();

        ch1.exchangeDeclare(controllerDirectExchangeToWingFlaps, "direct");

        String qName_altitude = ch1.queueDeclare().getQueue();
        String qName_weather = ch1.queueDeclare().getQueue();

        ch1.queueBind(qName_altitude, controllerDirectExchangeToWingFlaps, controller_to_wingflaps_altitude_key);
        ch1.queueBind(qName_weather, controllerDirectExchangeToWingFlaps, controller_to_wingflaps_weather_key);

        receiveMsgFromControllerAltitude(qName_altitude);
        receiveMsgFromControllerWeather(qName_weather);

    }

    @Override
    public void run() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(WingFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromControllerAltitude(String qName) {

        try {
            ch1.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("WING FLAP: (ALTITUDE) received the following instruction from controller: " + message);

                performActions(message);
                System.out.println("WING FLAP: (ALTITUDE) angle had successfully set to " + wf.getAngle());

                switch (wf.getAngle()) {
                    case 10:
                    case 15:
                    case 20: {
                        String altitudeChange = String.valueOf(-rand.nextInt(201) - 100);
                        sendMsgToControllerAltitude(altitudeChange);
                        break;
                    }
                    case 30:
                    case 50: {
                        String altitudeChange = String.valueOf(rand.nextInt(100) + 50);
                        sendMsgToControllerAltitude(altitudeChange);
                        break;
                    }
                    case 70: {
                        String altitudeChange = String.valueOf(rand.nextInt(100) + 100);
                        sendMsgToControllerAltitude(altitudeChange);
                        break;
                    }
                    default: {
                         sendMsgToControllerAltitude("0");                       
                         return;
                    }
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(WingFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
                }

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(WingFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void performActions(String msg) {

        wf.setAngle(Integer.parseInt(msg));
    }

    public void sendMsgToControllerAltitude(String msg) {

        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(wingFlapDirectExchange, "direct"); //name, type                         
            chan.basicPublish(wingFlapDirectExchange, wingflaps_to_controller_altitude_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(WingFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void receiveMsgFromControllerWeather(String qName) {
        try {
            ch1.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("WING FLAP: (WEATHER) received the following instruction from controller: " + message);

                performActions(message);
                System.out.println("WING FLAP: (WEATHER) angle had successfully set to " + wf.getAngle());

                switch (wf.getAngle()) {

                    case 80: {
                        String altitudeChange = String.valueOf(rand.nextInt(100) + 150);
                        sendMsgToControllerWeather(altitudeChange);
                        break;
                    }
                    default: {
                        String altitudeChange = String.valueOf(rand.nextInt(100) + 175);
                        sendMsgToControllerWeather(altitudeChange);
                        break;
                    }
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(WingFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
                }

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(WingFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToControllerWeather(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(wingFlapDirectExchange, "direct"); //name, type                         
            chan.basicPublish(wingFlapDirectExchange, wingflaps_to_controller_weather_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(WingFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

class wingFlap {

    int angle;

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getAngle() {
        return angle;
    }
}
