package Actuators;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OxygenMaskActuator implements Runnable {

    //exchange from Controller to OxygenMasks
    String controllerDirectExchangeToOxygenMask = "controllerDirectExchangeToOxygenMask";

    //exchange from OxygenMasks to Controller
    String oxygenMaskDirectExchange = "oxygenMaskDirectExchange";

    //receive message from controller
    String contoller_to_oxygenmask_key = "controller_to_oxygenmask";

    //send message to controller
    String oxygenmask_to_controller_pressure_key = "oxygenmask_to_controller_pressure";
    String oxygenmask_to_controller_landing_key = "oxygenmask_to_controller_landing";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch;
    oxygenMask om = new oxygenMask();
    
    int triggerCount = 0;

    public OxygenMaskActuator(Connection con) throws IOException, TimeoutException {
        this.con = con;

        con = cf.newConnection();
        ch = con.createChannel();

        ch.exchangeDeclare(controllerDirectExchangeToOxygenMask, "direct");

        String qName = ch.queueDeclare().getQueue();

        ch.queueBind(qName, controllerDirectExchangeToOxygenMask, contoller_to_oxygenmask_key);

        receiveMsgFromController(qName);
    }

    @Override
    public void run() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(OxygenMaskActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void receiveMsgFromController(String qName){
        try {
            ch.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("OXYGEN MASK: received key from controller: " + message);
                
                triggerCount++;
                                
                om.setReleased(true);

                if (om.released == true && triggerCount ==1) {
                   
                    System.out.println("OXYGEN MASK: (CABIN) oxygen masks are released");
                    String msgToController = "oxygen masks are released successfully";
                    sendMsgToControllerPressure(msgToController);
                } else if (om.released == true && triggerCount >1 && triggerCount <4){
                    
                    System.out.println("OXYGEN MASK: oxygen masks are already realesed!!!");
                } else if (om.released == true && triggerCount==4){
                   
                    String msgToController = "oxygen masks already realesed!!! Suggest emergency landing";
                    System.out.println("OXYGEN MASK: " + msgToController);
                    sendMsgToControllerLanding(msgToController);
                    return;     //the whole if statement will not appear once returned (trigger count >4 onwards)
                }
                
            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(OxygenMaskActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(OxygenMaskActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendMsgToControllerPressure(String msg){
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(oxygenMaskDirectExchange, "direct"); //name, type                         
            chan.basicPublish(oxygenMaskDirectExchange, oxygenmask_to_controller_pressure_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(OxygenMaskActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendMsgToControllerLanding(String msg){
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(oxygenMaskDirectExchange, "direct"); //name, type                         
            chan.basicPublish(oxygenMaskDirectExchange, oxygenmask_to_controller_landing_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(OxygenMaskActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

class oxygenMask {

    boolean released = false;

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

}
