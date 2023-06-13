package FMS;

import Sensors.AltitudeSensor;
import static Sensors.AltitudeSensor.startTime;
import Sensors.SpeedSensor;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Controller implements Runnable {

    //exchange from sensors to contoller
    String altitudeDirectExchange = "altitudeDirectExchange";
    String speedDirectExchange = "speedDirectExchange";
    String weatherDirectExchange = "weatherDirectExchange";
    String cabinDirectExchange = "cabinDirectExchange";

    //exchange from controller to actuators
    String controllerDirectExchangeToWingFlaps = "controllerDirectExchangeToWingFlaps";
    String controllerDirectExchangeToEngines = "controllerDirectExchangeToEngines";
    String controllerDirectExchangeToTailFlaps = "controllerDirectExchangeToTailFlaps";
    String controllerDirectExchangeToOxygenMask = "controllerDirectExchangeToOxygenMask";
    String controllerDirectExchangeToLandingGear = "controllerDirectExchangeToLandingGear";

    String controllerDirectExchangeToCabin = "controllerDirectExchangeToCabin";

    //exchange from actuators to controller
    String wingFlapDirectExchange = "wingFlapDirectExchange";
    String engineDirectExchange = "engineDirectExchange";
    String tailFlapDirectExchange = "tailFlapDirectExchange";
    String oxygenMaskDirectExchange = "oxygenMaskDirectExchange";
    String landingGearDirectExchange = "landingGearDirectExchange";

    //receives message from sensors
    String altitude_to_controller_key = "altitude_to_controller";
    String speed_to_controller_key = "speed_to_controller";
    String weather_to_controller_key = "weather_to_controller";
    String cabin_to_controller_key = "cabin_to_controller";
    String altitude_to_controller_landing_key = "altitude_to_controller_landing";
    String speed_to_controller_landing_key = "altitude_to_controller_landing";
    String cabin_to_controller_landing_key = "cabin_to_controller_landing";

    //send messages to actuators
    String controller_to_wingflaps_altitude_key = "controller_to_wingflaps_altitude";
    String controller_to_engines_altitude_key = "controller_to_engines_altitude";
    String controller_to_engines_speed_key = "controller_to_engines_speed";
    String controller_to_tailflaps_speed_key = "controller_to_tailflaps_speed";
    String controller_to_wingflaps_weather_key = "controller_to_wingflaps_weather";
    String controller_to_tailflaps_weather_key = "controller_to_tailflaps_weather";
    String controller_to_oxygenmask_key = "controller_to_oxygenmask";
    String controller_to_landinggear_key = "controller_to_landinggear";

    //receives messages from actuators
    String wingflaps_to_controller_altitude_key = "wingflaps_to_controller_altitude";
    String engines_to_controller_altitude_key = "engines_to_controller_altitude";
    String engines_to_controller_speed_key = "engines_to_controller_speed";
    String tailflaps_to_controller_speed_key = "tailflaps_to_controller_speed";
    String wingflaps_to_controller_weather_key = "wingflaps_to_controller_weather";
    String tailflaps_to_controller_weather_key = "tailflaps_to_controller_weather";
    String oxygenmask_to_controller_pressure_key = "oxygenmask_to_controller_pressure";
    String oxygenmask_to_controller_landing_key = "oxygenmask_to_controller_landing";
    String landinggear_to_controller_key = "landinggear_to_controller";

    //send messages to sensors
    String controller_to_altitude_key = "controller_to_altitude";
    String controller_to_speed_key = "controller_to_speed";
    String controller_to_altitude_weather_key = "controller_to_altitude_weather";
    String controller_to_cabin_key = "controller_to_cabin";

    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel altitudeChannel;
    Channel wingChannel;
    Channel engineChannel;
    Channel speedChannel;
    Channel tailChannel;
    Channel weatherChannel;
    Channel cabinChannel;
    Channel maskChannel;
    Channel landingChannel;

    String wingFlapdegree;
    String tailFlapdegree;
    String speed;

    String initialAltitudeValue = "";
    String storedAltitudeValue;

    String storedSpeedValue = "";
    String altitudeChange = "";     ////
    String storedPressureValue;

    long end;
    long start;


    public static Boolean cabinLoss = false;

    public Controller(Connection con) throws IOException, TimeoutException {
        this.con = con;

        cf.setChannelRpcTimeout(300000);

        con = cf.newConnection();
        altitudeChannel = con.createChannel();
        wingChannel = con.createChannel();
        engineChannel = con.createChannel();
        speedChannel = con.createChannel();
        tailChannel = con.createChannel();
        weatherChannel = con.createChannel();
        cabinChannel = con.createChannel();
        maskChannel = con.createChannel();
        landingChannel = con.createChannel();

        altitudeChannel.exchangeDeclare(altitudeDirectExchange, "direct");
        wingChannel.exchangeDeclare(wingFlapDirectExchange, "direct");
        engineChannel.exchangeDeclare(engineDirectExchange, "direct");
        speedChannel.exchangeDeclare(speedDirectExchange, "direct");
        tailChannel.exchangeDeclare(tailFlapDirectExchange, "direct");
        weatherChannel.exchangeDeclare(weatherDirectExchange, "direct");
        cabinChannel.exchangeDeclare(cabinDirectExchange, "direct");
        maskChannel.exchangeDeclare(oxygenMaskDirectExchange, "direct");
        landingChannel.exchangeDeclare(landingGearDirectExchange, "direct");

        String qName_altitude = altitudeChannel.queueDeclare().getQueue();
        String qName_wingAltitude = wingChannel.queueDeclare().getQueue();
        String qName_engineAltitude = engineChannel.queueDeclare().getQueue();
        String qName_engineSpeed = engineChannel.queueDeclare().getQueue();
        String qName_speed = speedChannel.queueDeclare().getQueue();
        String qName_tailSpeed = tailChannel.queueDeclare().getQueue();
        String qName_weather = weatherChannel.queueDeclare().getQueue();
        String qName_wingWeather = wingChannel.queueDeclare().getQueue();
        String qName_tailWeather = tailChannel.queueDeclare().getQueue();
        String qName_cabin = cabinChannel.queueDeclare().getQueue();
        String qName_maskPressure = maskChannel.queueDeclare().getQueue();
        String qName_maskLanding = maskChannel.queueDeclare().getQueue();
        String qName_landinggear = landingChannel.queueDeclare().getQueue();
        String qName_altitudeToZero = altitudeChannel.queueDeclare().getQueue();
        String qName_speedToZero = engineChannel.queueDeclare().getQueue();
        String qName_cabinToNormal = cabinChannel.queueDeclare().getQueue();

        altitudeChannel.queueBind(qName_altitude, altitudeDirectExchange, altitude_to_controller_key);
        wingChannel.queueBind(qName_wingAltitude, wingFlapDirectExchange, wingflaps_to_controller_altitude_key);
        engineChannel.queueBind(qName_engineSpeed, engineDirectExchange, engines_to_controller_speed_key);
        engineChannel.queueBind(qName_engineAltitude, engineDirectExchange, engines_to_controller_altitude_key);
        speedChannel.queueBind(qName_speed, speedDirectExchange, speed_to_controller_key);
        tailChannel.queueBind(qName_tailSpeed, tailFlapDirectExchange, tailflaps_to_controller_speed_key);
        weatherChannel.queueBind(qName_weather, weatherDirectExchange, weather_to_controller_key);
        wingChannel.queueBind(qName_wingWeather, wingFlapDirectExchange, wingflaps_to_controller_weather_key);
        tailChannel.queueBind(qName_tailWeather, tailFlapDirectExchange, tailflaps_to_controller_weather_key);
        cabinChannel.queueBind(qName_cabin, cabinDirectExchange, cabin_to_controller_key);
        maskChannel.queueBind(qName_maskPressure, oxygenMaskDirectExchange, oxygenmask_to_controller_pressure_key);
        maskChannel.queueBind(qName_maskLanding, oxygenMaskDirectExchange, oxygenmask_to_controller_landing_key);
        landingChannel.queueBind(qName_landinggear, landingGearDirectExchange, landinggear_to_controller_key);
        altitudeChannel.queueBind(qName_altitudeToZero, altitudeDirectExchange, altitude_to_controller_landing_key);
        speedChannel.queueBind(qName_speedToZero, speedDirectExchange, speed_to_controller_landing_key);
        cabinChannel.queueBind(qName_cabinToNormal, cabinDirectExchange, cabin_to_controller_landing_key);

        receiveMsgFromSpeed(qName_speed);
        receiveMsgFromEnginesSpeed(qName_engineSpeed);
        receiveMsgFromAltitude(qName_altitude);
        receiveMsgFromWingFlapAltitude(qName_wingAltitude);
        receiveMsgFromTailFlapSpeed(qName_tailSpeed);
        receiveMsgFromEnginesAltitude(qName_engineAltitude);
        receiveMsgFromWeather(qName_weather);
        receiveMsgFromWingFlapWeather(qName_wingWeather);
        receiveMsgFromTailFlapWeather(qName_tailWeather);
        receiveMsgFromCabin(qName_cabin);
        receiveMsgFromOxygenMaskPressure(qName_maskPressure);
        receiveMsgFromOxygenMaskLanding(qName_maskLanding);
        receiveMsgFromLandingGear(qName_landinggear);

        receiveMsgFromAltitudeLanding(qName_altitudeToZero);
        receiveMsgFromCabinLanding(qName_cabinToNormal);
        receiveMsgFromSpeedLanding(qName_speedToZero);

    }

    public void run() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromAltitude(String qName) {
        try {
            altitudeChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: received key from altitude sensor: " + message);
                initialAltitudeValue = message;

                if (Integer.parseInt(message) >= 3600 && Integer.parseInt(message) <= 3700) {

                    wingFlapdegree = "30";
                    System.out.println("CONTROLLER: (ALTITUDE) set wing flaps degree to " + wingFlapdegree);
                    sendMsgToWingFlapAltitude(wingFlapdegree);

                    speed = "40";
                    System.out.println("CONTROLLER: (ALTITUDE) set engine speed to " + speed);
                    sendMsgToEngineAltitude(speed);

                } else if (Integer.parseInt(message) >= 3701 && Integer.parseInt(message) <= 3900) {

                    wingFlapdegree = "50";
                    System.out.println("CONTROLLER: (ALTITUDE) set wing flaps degree to " + wingFlapdegree);
                    sendMsgToWingFlapAltitude(wingFlapdegree);

                    speed = "60";
                    System.out.println("CONTROLLER: (ALTITUDE) set engine speed to " + speed);
                    sendMsgToEngineAltitude(speed);

                } else if (Integer.parseInt(message) >= 3901 && Integer.parseInt(message) <= 4000) {

                    wingFlapdegree = "70";
                    System.out.println("CONTROLLER: (ALTITUDE) set wing flaps degree to " + wingFlapdegree);
                    sendMsgToWingFlapAltitude(wingFlapdegree);

                    speed = "80";
                    System.out.println("CONTROLLER: (ALTITUDE) set engine speed to " + speed);
                    sendMsgToEngineAltitude(speed);

                } else if (Integer.parseInt(message) >= 4001 && Integer.parseInt(message) <= 4400) {

                    wingFlapdegree = "20";
                    System.out.println("CONTROLLER: (ALTITUDE) set wing flaps degree to " + wingFlapdegree);
                    sendMsgToWingFlapAltitude(wingFlapdegree);

                    speed = "30";
                    System.out.println("CONTROLLER: (ALTITUDE) set engine speed to " + speed);
                    sendMsgToEngineAltitude(speed);

                } else if (Integer.parseInt(message) >= 4401) {

                    wingFlapdegree = "15";
                    System.out.println("CONTROLLER: (ALTITUDE) set wing flaps degree to " + wingFlapdegree);
                    sendMsgToWingFlapAltitude(wingFlapdegree);

                    speed = "25";
                    System.out.println("CONTROLLER: (ALTITUDE) set engine speed to " + speed);
                    sendMsgToEngineAltitude(speed);

                } else {

                    ///////
                    wingFlapdegree = "10";
                    System.out.println("CONTROLLER: (ALTITUDE) set wing flaps degree to " + wingFlapdegree);
                    sendMsgToWingFlapAltitude(wingFlapdegree);

                    speed = "20";
                    System.out.println("CONTROLLER: (ALTITUDE) set engine speed to " + speed);
                    sendMsgToEngineAltitude(speed);
                }
            }, x -> {
            }
            );
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToWingFlapAltitude(String msg) {

        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToWingFlaps, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToWingFlaps, controller_to_wingflaps_altitude_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void sendMsgToTailFlapSpeed(String msg) {

        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToTailFlaps, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToTailFlaps, controller_to_tailflaps_speed_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromWingFlapAltitude(String qName) {
        try {
            wingChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");

                if (Integer.parseInt(message) != 0) {
                    System.out.println("CONTROLLER: (ALTITUDE) altitude had changed due to wing flap adjustments");
                    altitudeChange = message;

//                    System.out.println("");
//                    System.out.println("stored altitude value: " + initialAltitudeValue);
//                    System.out.println("message:" + message);
//                    System.out.println("");
                    int finalAltitude = Integer.parseInt(message) + Integer.parseInt(initialAltitudeValue);
                    sendMsgToAltitudeFromWingFlapAltitude(String.valueOf(finalAltitude));

                } else {
                    System.out.println("CONTROLLER: (ALTITUDE) altitude is decrementing");
                    sendMsgToAltitudeFromWingFlapAltitude(message);
                }

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }   //storedAltitudeValue

    public void sendMsgToAltitudeFromWingFlapAltitude(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToWingFlaps, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToWingFlaps, controller_to_altitude_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToEngineAltitude(String msg) {

        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToEngines, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToEngines, controller_to_engines_altitude_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void receiveMsgFromEnginesSpeed(String qName) {
        if (con != null) {
            try {

                engineChannel.basicConsume(qName, true, (x, msg) -> {
                    String message = new String(msg.getBody(), "UTF-8");

                    System.out.println("CONTROLLER: (SPEED) received the following speed change from engines: " + message);

                    sendMsgToSpeedEngine(message);

                }, x -> {
                });
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//storedAltitudeValue = String.valueOf(Integer.parseInt(storedAltitudeValue) + Integer.parseInt(altitudeChange));

    public void receiveMsgFromEnginesAltitude(String qName) {
        if (con != null) {
            try {

                engineChannel.basicConsume(qName, true, (x, msg) -> {
                    String message = new String(msg.getBody(), "UTF-8");
                    System.out.println("CONTROLLER: (ALTITUDE) received the following speed change: " + message);

                    sendMsgToSpeedAltitude(message);

                }, x -> {
                });
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    } //storedAltitudeValue

    public void sendMsgToSpeedEngine(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToEngines, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToEngines, controller_to_speed_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToSpeedAltitude(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToEngines, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToEngines, controller_to_altitude_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromSpeed(String qName) {

        try {
            speedChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: received key from speed sensor: " + message);

                storedSpeedValue = message;

                if (Integer.parseInt(message) <= 20) {

                    speed = "20";
                    System.out.println("CONTROLLER: (SPEED) maintain engine speed or above: " + speed);
                    sendMsgToEngineSpeed(speed);

                    tailFlapdegree = "25";
                    System.out.println("CONTROLLER: (SPEED) set tail flaps degree to " + tailFlapdegree);
                    sendMsgToTailFlapSpeed(tailFlapdegree);

                } else if (Integer.parseInt(message) >= 21 && Integer.parseInt(message) <= 30) {

                    speed = "30";
                    System.out.println("CONTROLLER: (SPEED) maintain engine speed or above: " + speed);
                    sendMsgToEngineSpeed(speed);

                    tailFlapdegree = "35";
                    System.out.println("CONTROLLER: (SPEED) set tail flaps degree to " + tailFlapdegree);
                    sendMsgToTailFlapSpeed(tailFlapdegree);

                } else if (Integer.parseInt(message) >= 31 && Integer.parseInt(message) <= 40) {

                    speed = "40";
                    System.out.println("CONTROLLER: (SPEED) maintain engine speed or above: " + speed);
                    sendMsgToEngineSpeed(speed);

                    tailFlapdegree = "45";
                    System.out.println("CONTROLLER: (SPEED) set tail flaps degree to " + tailFlapdegree);
                    sendMsgToTailFlapSpeed(tailFlapdegree);

                } else if (Integer.parseInt(message) >= 41 && Integer.parseInt(message) <= 50) {

                    speed = "50";
                    System.out.println("CONTROLLER: (SPEED) maintain engine speed or above: " + speed);
                    sendMsgToEngineSpeed(speed);

                    tailFlapdegree = "55";
                    System.out.println("CONTROLLER: (SPEED) set tail flaps degree to " + tailFlapdegree);
                    sendMsgToTailFlapSpeed(tailFlapdegree);

                } else if (Integer.parseInt(message) >= 51 && Integer.parseInt(message) <= 60) {

                    speed = "60";
                    System.out.println("CONTROLLER: (SPEED) maintain engine speed or below: " + speed);
                    sendMsgToEngineSpeed(speed);

                    tailFlapdegree = "60";
                    System.out.println("CONTROLLER: (SPEED) set tail flaps degree to " + tailFlapdegree);
                    sendMsgToTailFlapSpeed(tailFlapdegree);

                } else if (Integer.parseInt(message) >= 61 && Integer.parseInt(message) <= 65) {

                    speed = "70";
                    System.out.println("CONTROLLER: (SPEED) maintain engine speed or below: " + speed);
                    sendMsgToEngineSpeed(speed);

                    tailFlapdegree = "65";
                    System.out.println("CONTROLLER: (SPEED) set tail flaps degree to " + tailFlapdegree);
                    sendMsgToTailFlapSpeed(tailFlapdegree);

                } else {

                    speed = "55";
                    System.out.println("CONTROLLER: (SPEED) too fast, maintain engine speed at " + speed);
                    sendMsgToEngineSpeed(speed);

                    tailFlapdegree = "20";
                    System.out.println("CONTROLLER: (SPEED) set tail flaps degree to " + tailFlapdegree);
                    sendMsgToTailFlapSpeed(tailFlapdegree);

                }

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToEngineSpeed(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToEngines, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToEngines, controller_to_engines_speed_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromTailFlapSpeed(String qName) {
        try {
            tailChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: (SPEED) altitude had changed due to tail flap adjustments");

                altitudeChange = message;

                int finalAltitude = Integer.parseInt(message) + Integer.parseInt(initialAltitudeValue);

                sendMsgToAltitudeFromTailFlap(String.valueOf(finalAltitude));

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }   //storedAltitudeValue

    public void sendMsgToAltitudeFromTailFlap(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToTailFlaps, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToTailFlaps, controller_to_altitude_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromWeather(String qName) {
        try {
            weatherChannel.basicConsume(qName, true, (String x, Delivery msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: received key from weather sensor: " + message);

                if (message.equals("cloudy")) {

                    System.out.println("CONTROLLER: (CLOUDY WEATHER) maintain current wing flap and tail flap degree");

                } else if (message.equals("stormy")) {

                    wingFlapdegree = "80";
                    System.out.println("CONTROLLER: (STORMY WEATHER) set wing flaps degree to " + wingFlapdegree);
                    sendMsgToWingFlapWeather(wingFlapdegree);

                    tailFlapdegree = "40";
                    System.out.println("CONTROLLER: (STORMY WEATHER) set tail flaps degree to " + tailFlapdegree);
                    sendMsgToTailFlapWeather(tailFlapdegree);

                } else {

                    wingFlapdegree = "100";
                    System.out.println("CONTROLLER: (RAINY WEATHER) set wing flaps degree to " + wingFlapdegree);
                    sendMsgToWingFlapWeather(wingFlapdegree);

                    String tailFlapdegree = "50";
                    System.out.println("CONTROLLER: (RAINY WEATHER) set tail flaps degree to " + tailFlapdegree);
                    sendMsgToTailFlapWeather(tailFlapdegree);

                    cabinLoss = true;
                }

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToWingFlapWeather(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToWingFlaps, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToWingFlaps, controller_to_wingflaps_weather_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromWingFlapWeather(String qName) {
        try {
            wingChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: (WEATHER) ALERT! altitude had changed due to wing flap adjustments");

                altitudeChange = message;

                int finalAltitude = Integer.parseInt(message) + Integer.parseInt(initialAltitudeValue);

                sendMsgToAltitudeFromWingFlapWeather(String.valueOf(finalAltitude));

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToAltitudeFromWingFlapWeather(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToWingFlaps, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToWingFlaps, controller_to_altitude_weather_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToTailFlapWeather(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToTailFlaps, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToTailFlaps, controller_to_tailflaps_weather_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromTailFlapWeather(String qName) {
        try {
            tailChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");

                System.out.println("CONTROLLER: (WEATHER) ALERT! altitude had changed due to tail flap adjustments");

                int finalAltitude = Integer.parseInt(message) + Integer.parseInt(initialAltitudeValue);

                sendMsgToAltitudeFromTailFlapWeather(String.valueOf(finalAltitude));

            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToAltitudeFromTailFlapWeather(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToTailFlaps, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToTailFlaps, controller_to_altitude_weather_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromCabin(String qName) {
        try {
            cabinChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: received key from cabin sensor: " + message);
                storedPressureValue = message;

                if (Integer.parseInt(message) <= 5200) {

                    System.out.println("CONTROLLER: WARNING! received significant pressure drop!!!");
                    String emergency = "Release oxygen mask!!!";
                    System.out.println("CONTROLLER: " + emergency);
                    sendMsgToOxygenMask(emergency);

                } else {

                    System.out.println("CONTROLLER: The cabin pressure is in normal state");
                }
            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToOxygenMask(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToOxygenMask, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToOxygenMask, controller_to_oxygenmask_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromOxygenMaskPressure(String qName) {
        try {
            maskChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: (CABIN): " + message);

            }, x -> {
            });

        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromOxygenMaskLanding(String qName) {
        try {
            maskChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: (CABIN): " + message);

                String msgToLandingGears = "release landing gears!";
                System.out.println("CONTROLLER: " + msgToLandingGears);
                sendMsgToLandingGears(msgToLandingGears);
            }, x -> {
            });

        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void sendMsgToLandingGears(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToLandingGear, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToLandingGear, controller_to_landinggear_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromLandingGear(String qName) {
        try {
            landingChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: (CABIN): " + message);

                wingFlapdegree = "5";
                System.out.println("CONTROLLER: (CABIN) set wing flaps degree to " + wingFlapdegree + " for landing");
                sendMsgToWingFlapLanding(wingFlapdegree);

                speed = "0";
                System.out.println("CONTROLLER: (CABIN) slowly decrement the engine speed to " + speed + " for landing");
                sendMsgToEngineLanding(speed);

            }, x -> {
            });

        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToWingFlapLanding(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToWingFlaps, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToWingFlaps, controller_to_wingflaps_altitude_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMsgToEngineLanding(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToEngines, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToEngines, controller_to_engines_altitude_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromAltitudeLanding(String qName) {
        try {
            altitudeChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: received key from altitude sensor: " + message);

                if (Integer.parseInt(message) == 0) {

                    System.out.println("The plane had reached the land");

                    System.out.println("Cabin pressure should be returning back to normal");
                    notifyLanding(storedPressureValue);
                }
            }, x -> {
            }
            );
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void notifyLanding(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(controllerDirectExchangeToCabin, "direct"); //name, type                         
            chan.basicPublish(controllerDirectExchangeToCabin, controller_to_cabin_key, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromCabinLanding(String qName) {
        try {
            cabinChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: received key from cabin sensor: " + message);

                if (Integer.parseInt(message) >= 6000) {
                    System.out.println("The cabin pressure is back to normal");

                }
            }, x -> {
            }
            );
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveMsgFromSpeedLanding(String qName) {
        try {
            speedChannel.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), "UTF-8");
                System.out.println("CONTROLLER: received key from speed sensor: " + message);

                if (Integer.parseInt(message) == 0) {

                    end = new Date().getTime();

                    System.out.println("The plane had stopped");
                    System.out.println("");
                    calculateTime(end);

                    System.exit(0);
                }
            }, x -> {
            }
            );
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void calculateTime(long end) {
        start = AltitudeSensor.startTime.get(0);
        long time = end - start;

        Date startdate = new Date(start);
        Date enddate = new Date(end);

//        start = startTimeEngineAltitude.get(0);
//        long time1 = SpeedSensor.endAltitude;
        System.out.println("Start: " + start + " " + startdate);
        System.out.println("End: " + end + " " + enddate);
        System.out.println("Total time used to run the simulation in long:" + time);
        //System.out.println("");

    }

    class start {

        long start;

        public long getStart() {
            return start;
        }

        public void setStart(long start) {
            this.start = start;
        }

    }
}
