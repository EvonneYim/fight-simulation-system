package Actuators;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EngineActuator implements Runnable {

    //exchange from Controller to Engines
    String controllerDirectExchangeToEngines = "controllerDirectExchangeToEngines";

    //exchange from Engines to Controller
    String engineDirectExchange = "engineDirectExchange";

    //receives message from controller
    String controller_to_engines_altitude_key = "controller_to_engines_altitude";
    String controller_to_engines_speed_key = "controller_to_engines_speed";

    //send message to controller
    String engines_to_controller_altitude_key = "engines_to_controller_altitude";
    String engines_to_controller_speed_key = "engines_to_controller_speed";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;

    Channel ch;

    engine eng = new engine();

    public EngineActuator(Connection con) throws IOException, TimeoutException {
        this.con = con;

        ch = con.createChannel();

        ch.exchangeDeclare(controllerDirectExchangeToEngines, "direct");

        String qName_speed = ch.queueDeclare().getQueue();
        String qName_altitude = ch.queueDeclare().getQueue();

        ch.queueBind(qName_altitude, controllerDirectExchangeToEngines, controller_to_engines_altitude_key);
        ch.queueBind(qName_speed, controllerDirectExchangeToEngines, controller_to_engines_speed_key);

        receiveMsgFromControllerSpeed(qName_speed);
        receiveMsgFromControllerAltitude(qName_altitude);

    }

    @Override
    public void run() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromControllerSpeed(String qName) {
        try {
            ch.basicConsume(qName, true, (x, msg) -> {
                String speed_message = new String(msg.getBody(), "UTF-8");
                System.out.println("ENGINES: (SPEED) received the following instruction from controller: " + speed_message);

                performActions(speed_message);

                System.out.println("ENGINES: (SPEED) speed had successfully set to " + eng.getSpeed());
                sendMsgToControllerSpeed(String.valueOf(eng.getSpeed()));

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
                }

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromControllerAltitude(String qName) {

        try {
            ch.basicConsume(qName, true, (String x, Delivery msg) -> {
                String altitude_message = new String(msg.getBody(), "UTF-8");

                System.out.println("ENGINES: (ALTITUDE) received the following instruction from controller: " + altitude_message);


                if (Integer.parseInt(altitude_message) != 0) {
                    performActions(altitude_message);
                    System.out.println("ENGINES: (ALTITUDE) speed had successfully set to " + eng.getSpeed());
                    sendMsgToControllerAltitude(String.valueOf(eng.getSpeed()));

                } else {

                    //System.out.println("aim to decrement altitude to 0000 from engineee");
                    System.out.println("ENGINES: (ALTITUDE) slowly decreasing speed to 0...");
                    performActions("0");
                    sendMsgToControllerAltitude(String.valueOf(eng.getSpeed()));
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
                }

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void sendMsgToControllerAltitude(String msg) {

        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(engineDirectExchange, "direct"); //name, type                         
            chan.basicPublish(engineDirectExchange, engines_to_controller_altitude_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void sendMsgToControllerSpeed(String msg) {

        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(engineDirectExchange, "direct"); //name, type                         
            chan.basicPublish(engineDirectExchange, engines_to_controller_speed_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void performActions(String msg) {
        eng.setSpeed(Integer.parseInt(msg));
    }

}

class engine {

    int speed;

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

}
