package Actuators;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TailFlapActuator implements Runnable {

    SecureRandom rand = new SecureRandom();
    //exchange from Controller to Tail Flaps
    String controllerDirectExchangeToTailFlaps = "controllerDirectExchangeToTailFlaps";
    
    //exchange from TailFlaps to Controller
    String tailFlapDirectExchange = "tailFlapDirectExchange";

    //receive message from controller
    String controller_to_tailflaps_speed_key = "controller_to_tailflaps_speed";
    String controller_to_tailflaps_weather_key = "controller_to_tailflaps_weather";
    
    //send message to controller
    String tailflaps_to_controller_speed_key = "tailflaps_to_controller_speed";
    String tailflaps_to_controller_weather_key = "tailflaps_to_controller_weather";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch;
    tailFlap tf = new tailFlap();

    public TailFlapActuator (Connection con) throws IOException, TimeoutException {
        this.con = con;
        
        ch = con.createChannel();       
        ch.exchangeDeclare(controllerDirectExchangeToTailFlaps, "direct");
        
        String qName_speed = ch.queueDeclare().getQueue();
        String qName_weather = ch.queueDeclare().getQueue();

        ch.queueBind(qName_speed, controllerDirectExchangeToTailFlaps, controller_to_tailflaps_speed_key);
        ch.queueBind(qName_weather, controllerDirectExchangeToTailFlaps, controller_to_tailflaps_weather_key);

        receiveMsgFromControllerSpeed(qName_speed);
        receiveMsgFromControllerWeather(qName_weather);
        
        
    }

    @Override
    public void run() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(TailFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromControllerSpeed(String qName) {
        try {
            ch.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("TAIL FLAP: (SPEED) received the following instruction from controller: " + message);

                performActions(message);
                System.out.println("TAIL FLAP: (SPEED) angle had successfully set to " + tf.getAngle());

                switch (tf.getAngle()) {
                    case 10:
                    case 15:
                    case 20: {
                        String altitudeChange = String.valueOf(-rand.nextInt(80)-80);
                        sendMsgToControllerSpeed(altitudeChange);
                        break;
                    }
                    case 30:
                    case 50: {
                        String altitudeChange = String.valueOf(rand.nextInt(50) + 20);
                        sendMsgToControllerSpeed(altitudeChange);
                        break;
                    }
                    default: {
                        String altitudeChange = String.valueOf(rand.nextInt(50) + 40);
                        sendMsgToControllerSpeed(altitudeChange);
                        break;
                    }
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TailFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
                }

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(TailFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void performActions(String msg) {
        tf.setAngle(Integer.parseInt(msg));
    }
    
    public void sendMsgToControllerSpeed(String msg) {

        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(tailFlapDirectExchange, "direct"); //name, type                         
            chan.basicPublish(tailFlapDirectExchange, tailflaps_to_controller_speed_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(TailFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    public void receiveMsgFromControllerWeather(String qName){
        try {
                ch.basicConsume(qName, true, (x, msg) -> {
                    String message = new String(msg.getBody(), "UTF-8");
                    System.out.println("TAIL FLAP: (WEATHER) received the following instruction from controller: " + message);

                    performActions(message);
                    System.out.println("TAIL FLAP: (WEATHER) angle had successfully set to " + tf.getAngle());

                    switch (tf.getAngle()) {
                        
                        case 40:
                            {
                                String altitudeChange = String.valueOf(rand.nextInt(50) + 50);
                                sendMsgToControllerWeather(altitudeChange);
                                break;
                            }    
                        default:
                            {
                                String altitudeChange = String.valueOf(rand.nextInt(50) + 60);
                                sendMsgToControllerWeather(altitudeChange);
                                break;
                            }
                    }

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TailFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }, x -> {
                });
            } catch (IOException ex) {
                Logger.getLogger(TailFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    
    public void sendMsgToControllerWeather(String msg){
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(tailFlapDirectExchange, "direct"); //name, type                         
            chan.basicPublish(tailFlapDirectExchange, tailflaps_to_controller_weather_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(TailFlapActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class tailFlap {

    int angle;

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getAngle() {
        return angle;
    }

}
