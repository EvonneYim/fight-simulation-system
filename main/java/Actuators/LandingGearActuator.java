package Actuators;

import FMS.CabinLoss;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LandingGearActuator implements Runnable {

    //exchange from Controller to LandingGears
    String controllerDirectExchangeToLandingGear = "controllerDirectExchangeToLandingGear";

    //exchange from OxygenMasks to Controller
    String landingGearDirectExchange = "landingGearDirectExchange";

    //receive message from controller
    String contoller_to_landinggear_key = "controller_to_landinggear";

    //send message to controller
    String landinggear_to_controller_key = "landinggear_to_controller";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch;

    landingGear lg = new landingGear();
    
    
    public static boolean landingMode = false;

    //Future<FMS.CabinLoss> futureCabinLoss;
    public LandingGearActuator(Connection con) throws IOException, TimeoutException {
        this.con = con;

        ch = con.createChannel();

        ch.exchangeDeclare(controllerDirectExchangeToLandingGear, "direct");

        String qName = ch.queueDeclare().getQueue();

        ch.queueBind(qName, controllerDirectExchangeToLandingGear, contoller_to_landinggear_key);

        receiveMsgFromController(qName);

    }

    @Override
    public void run() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(LandingGearActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromController(String qName) {
        try {
            ch.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("LANDING GEAR: received the following instructions from controller: " + message);

                lg.setReleased(true);
                
                if (lg.released == true){
                    System.out.println("LANDING GEAR: (CABIN) landing gears are released");
                    String msgToController = "landing gears are released successfully";
                    sendMsgToController(msgToController);
                    
                    landingMode = true;
                    
                    return;
                }
                
            }, x -> {
            });

        } catch (IOException ex) {
            Logger.getLogger(LandingGearActuator.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(LandingGearActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendMsgToController(String msg){
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(landingGearDirectExchange, "direct"); //name, type                         
            chan.basicPublish(landingGearDirectExchange, landinggear_to_controller_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(LandingGearActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

class landingGear {

    boolean released = false;

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }
}
