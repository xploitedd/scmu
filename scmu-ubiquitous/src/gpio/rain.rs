use std::{thread, time};

use rand::random;
use rppal::gpio::{Gpio, InputPin, OutputPin};

pub struct Rain {
    pub is_raining: bool
}

pub trait RainSensor {
    fn new() -> Self;
    fn read_value(&mut self) -> Option<Rain>;
}

pub struct RainSensorReal {
    read_pin: InputPin,
    vcc_pin: OutputPin
}

impl RainSensor for RainSensorReal {
    fn new() -> Self {
        let gpio = Gpio::new()
            .unwrap();

        let mut read_pin = gpio.get(20)
            .unwrap()
            .into_input();

        let mut vcc_pin = gpio.get(20)
            .unwrap()
            .into_output();

        vcc_pin.set_low();

        Self {
            read_pin,
            vcc_pin
        }
    }

    fn read_value(&mut self) -> Option<Rain> {
        self.vcc_pin.set_high();
        thread::sleep(time::Duration::from_millis(10));
        let is_raining = self.read_pin.is_high();
        self.vcc_pin.set_low();

        Some(Rain {
            is_raining
        })
    }
}

pub struct RainSensorSimulator {}

impl RainSensor for RainSensorSimulator {
    fn new() -> Self {
        Self {}
    }

    fn read_value(&mut self) -> Option<Rain> {
        let val = random::<f32>() * 10.0;
        Some(Rain {
            is_raining: val >= 7.0
        })
    }
}