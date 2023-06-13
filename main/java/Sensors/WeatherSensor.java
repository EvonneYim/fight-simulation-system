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

public class WeatherSensor implements Runnable {

    SecureRandom rand = new SecureRandom();

    //exchange from sensor to controller
    String weatherDirectExchange = "weatherDirectExchange";

    //send message to controller
    String weather_to_controller_key = "weather_to_controller";

    //exchange from controller to sensors
    String controllerDirectExchangeToWingFlaps = "controllerDirectExchangeToWingFlaps";
    String controllerDirectExchangeToTailFlaps = "controllerDirectExchangeToTailFlaps";

    //receive message from controller
    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel ch;

    String[] weatherTypes = {"cloudy", "stormy", "rainy"};
    int currentWeatherIndex = 0;
    int weatherCount = 0;

    public WeatherSensor(Connection con) throws IOException, TimeoutException {
        this.con = con;
        ch = con.createChannel();
        ch.exchangeDeclare(controllerDirectExchangeToWingFlaps, "direct");

        String qName = ch.queueDeclare().getQueue();

    }

    @Override
    public void run() {
        
        if (LandingGearActuator.landingMode == true) {
            return;
        }
        String weather = getWeatherReading();

        System.out.println("WEATHER: detected the following weather: " + weather);
        sendMsgToController(weather);

        try {
            Thread.sleep(700);
        } catch (InterruptedException ex) {
            Logger.getLogger(WeatherSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getWeatherReading() {

        String currentWeather = weatherTypes[currentWeatherIndex];
        weatherCount++;
        if (weatherCount == 3) {
            currentWeatherIndex = (currentWeatherIndex + 1) % weatherTypes.length;
            weatherCount = 0;
        }
        return currentWeather;
    }

    public void sendMsgToController(String msg) {
        try ( Connection con = cf.newConnection()) {
            Channel chan = con.createChannel();
            chan.exchangeDeclare(weatherDirectExchange, "direct");  //name,type
            chan.basicPublish(weatherDirectExchange, weather_to_controller_key, false, null, msg.getBytes());

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(WeatherSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
