package artemis_helm;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinDirection;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.trigger.GpioCallbackTrigger;
import com.pi4j.io.gpio.trigger.GpioPulseStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSetStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSyncStateTrigger;
import com.pi4j.io.gpio.event.GpioPinListener;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.gpio.event.PinEventType;
import java.lang.Math;

public class Main {
    static Logger logger = Logger.getLogger(Main.class);

    // Set up pins
    final static GpioController gpio = GpioFactory.getInstance();
    final static GpioPinDigitalOutput SPI_CLK  = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "SPI_CLK"); 
    final static GpioPinDigitalInput  SPI_MISO = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, "SPI_MISO"); 
    final static GpioPinDigitalOutput SPI_MOSI = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "SPI_MOSI"); 
    final static GpioPinDigitalOutput SPI_CS   = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "SPI_CS"); 

    static int[] last_read = {10, 10, 10}; // Stores the last value read for each potentiometer
    static int tolerance = 5;              // Jitter filter amount

    public static void main(String[] args) throws InterruptedException {
        BasicConfigurator.configure();
        logger.info("Starting...");          

        // Configure Shutdown behavior
        SPI_CLK.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        SPI_MISO.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        SPI_MOSI.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        SPI_CS.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);

        while(true) {
            for(int i=0; i<last_read.length; i++) {
                int pot_val = readADC(i);
                int pot_adjust = Math.abs(pot_val - last_read[i]);

                if (pot_adjust > tolerance && pot_val > tolerance) {
                    int val_percent = (int)Math.round((double)pot_val / 10.24);
                    logger.info("Potentiomenter " + i + "'s Value: " + val_percent + "%");
                    last_read[i] = pot_val;
                }
            }
            Thread.sleep(500);
        }
    }

    private static int readADC(int adcnum) {
        if (adcnum > 7 || adcnum < 0) {
            return -1;
        }
        SPI_CS.high();

        SPI_CLK.low();
        SPI_CS.low();

        int commandout = adcnum;
        commandout |= 0x18; // start bit + single ended bit
        commandout <<= 3;    // we only need to send 5 bits
        for(int i=0; i<5; i++) {
            if ((commandout & 0x80) != 0) {
                SPI_MOSI.high();
            } else {
                SPI_MOSI.low();
            }
            commandout <<= 1;
            SPI_CLK.high();
            SPI_CLK.low();
        }

        int adcount = 0;

        // read in 1 empty bit, 1 null bit, and 10 ADC bits
        for(int i=0; i<12; i++) {
            SPI_CLK.high();
            SPI_CLK.low();
            adcount <<= 1;
            if (SPI_MISO.isHigh()) {
                adcount |= 0x1;
            }
        }

        SPI_CS.high();

        adcount >>= 1; // The first bit is null, so drop it
        return adcount;
    }
}
