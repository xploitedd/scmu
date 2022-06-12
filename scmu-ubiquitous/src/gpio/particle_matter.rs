use core::time;
use std::thread;

use rand::random;
use rppal::uart::Uart;

pub struct ParticleMatter {
    pub pm_25_level: u16,
    pub pm_10_level: u16
}

pub trait ParticleMatterSensor {
    fn new() -> Self;
    fn read_value(&mut self) -> Option<ParticleMatter>;
}

pub struct ParticleMatterSensorReal {
    uart: Uart
}

// implemented according to https://sensirion.com/media/documents/8600FF88/616542B5/Sensirion_PM_Sensors_Datasheet_SPS30.pdf
impl ParticleMatterSensor for ParticleMatterSensorReal {
    fn new() -> Self {
        let mut uart = Uart::new(
            115200, 
            rppal::uart::Parity::None, 
            8, 
            1
        ).unwrap();

        uart.set_write_mode(true).unwrap();
        uart.set_read_mode(7, time::Duration::from_secs(0)).unwrap();

        uart.write(&[0x7E, 0x00, 0x00, 0x02, 0x01, 0x05, 0xF9, 0x7E]).unwrap();
        thread::sleep(time::Duration::from_millis(20));

        let mut buf: [u8; 7] = [0; 7];
        let bytes_read = uart.read(&mut buf)
            .unwrap();

        if bytes_read != 7 {
            panic!()
        }

        Self { uart }
    }

    fn read_value(&mut self) -> Option<ParticleMatter> {
        thread::sleep(time::Duration::from_secs(1));
            
        // Read Measured Values
        self.uart.write(&[0x7E, 0x00, 0x03, 0x00, 0xFC, 0x7E]).unwrap();
        self.uart.drain().unwrap();

        let mut buf: [u8; 27] = [0; 27];
        let bytes_read = self.uart.read(&mut buf)
            .unwrap();

        thread::sleep(time::Duration::from_millis(20));

        if bytes_read < 27 {
            return None
        }

        let pm25: u16 = (buf[7] as u16) << 1 | buf[8] as u16;
        let pm10: u16 = (buf[11] as u16) << 1 | buf[12] as u16;

        Some(ParticleMatter {
            pm_25_level: pm25,
            pm_10_level: pm10
        })
    }
}

pub struct ParticleMatterSensorSimulator {}

impl ParticleMatterSensor for ParticleMatterSensorSimulator {
    fn new() -> Self {
        Self {}
    }

    fn read_value(&mut self) -> Option<ParticleMatter> {
        thread::sleep(time::Duration::from_secs(1));
        
        Some(ParticleMatter {
            pm_25_level: (random::<f32>() * 150.0).floor() as u16,
            pm_10_level: (random::<f32>() * 150.0).floor() as u16
        })
    }
}