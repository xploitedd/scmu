use std::{thread, sync::{Arc, Mutex}, time, net::TcpStream};

use gpio::{rain::{RainSensorSimulator, RainSensor, Rain}, particle_matter::{ParticleMatterSensorSimulator, ParticleMatterSensor, ParticleMatter}};
use serde::{Serialize, Deserialize};
use tungstenite::{connect, handshake::client::Request, Message, WebSocket, stream::MaybeTlsStream};
use url::Url;

mod util;
mod wifi;
mod bt;
mod gpio;

#[derive(Serialize, Deserialize)]
struct PkConfiguration {
    public_key: String
}

#[derive(Serialize, Deserialize)]
struct Incoming {
    pm_25_threshold: u32,
    pm_10_threshold: u32,
    signature: Option<String>
}

#[derive(Serialize, Deserialize)]
struct Wifi {
    ssid: String,
    strength: u32
}

#[derive(Serialize, Deserialize)]
struct Outgoing {
    thresholds: Incoming,
    wifi: Wifi,
    is_closed: bool,
    is_raining: bool,
    pm_25_level: u32,
    pm_10_level: u32
}

struct SharedData {
    ws: WebSocket<MaybeTlsStream<TcpStream>>,
    pm25_threshold: u32,
    pm10_threshold: u32,
    closed: bool
}

fn main() {
    // let wm: &'static WifiManager = Box::leak(Box::new(WifiManager::new()));

    // let mut bt = Bluetooth::new(Advertisement {
    //     discoverable: Some(true),
    //     local_name: Some("joe".to_string()),
    //     ..Default::default()
    // }).await.unwrap();

    // bt.start_app(Application {
    //     services: vec![
    //         WifiConfigurationService::create_service(wm)?
    //     ],
    //     ..Default::default()
    // }).await.unwrap();

    // let rainSensor = RainSensorReal::new();
    let mut rain_sensor = RainSensorSimulator::new();

    // let pmSensor = ParticleMatterSensorReal::new();
    let mut pm_sensor = ParticleMatterSensorSimulator::new();

    let req = Url::parse("ws://192.168.0.232:8080/ubiquitous")
        .unwrap();

    let (mut ws, _) = connect(req)
        .unwrap();

    let config_pkt = serde_json::to_string(&PkConfiguration {
        public_key: "test1_pk".to_string()
    }).unwrap();

    ws.write_message(tungstenite::Message::Text(config_pkt))
        .unwrap();

    let shared_data = Arc::new(Mutex::new(SharedData {
        ws,
        pm25_threshold: 100,
        pm10_threshold: 100,
        closed: false
    }));

    let sd_cln = shared_data.clone();
    thread::spawn(move || {
        loop {
            {
                let mut lkd = sd_cln.lock().unwrap();
                let msg = lkd.ws.read_message().unwrap();
                match msg {
                    Message::Text(text) => {
                        let inc: Incoming = serde_json::from_str(&text)
                            .unwrap();

                        lkd.pm25_threshold = inc.pm_25_threshold;
                        lkd.pm10_threshold = inc.pm_10_threshold;

                        println!("pm 2.5 threshold: {}", inc.pm_25_threshold);
                        println!("pm 10 threshold: {}", inc.pm_10_threshold);
                    },
                    Message::Ping(payload) => {
                        lkd.ws.write_message(Message::Pong(payload))
                            .unwrap()
                    }
                    _ => {}
                }
            }

            thread::sleep(time::Duration::from_secs(2));
        }
    });

    loop {
        let rain_val = rain_sensor.read_value()
           .unwrap();

        let pm_val = pm_sensor.read_value()
            .unwrap();

        {
            let mut sd = shared_data.lock().unwrap();
            if rain_val.is_raining || (pm_val.pm_25_level as u32) > sd.pm25_threshold || (pm_val.pm_10_level as u32) > sd.pm10_threshold {
                if !sd.closed {
                    println!("Closing...");
                    sd.closed = true;
                }
            } else if sd.closed {
                println!("Opening...");
                sd.closed = false;
            }

            let status_pkt = serde_json::to_string(&Outgoing {
                is_closed: sd.closed,
                is_raining: rain_val.is_raining,
                pm_25_level: pm_val.pm_25_level as u32,
                pm_10_level: pm_val.pm_10_level as u32,
                thresholds: Incoming { pm_25_threshold: sd.pm25_threshold, pm_10_threshold: sd.pm10_threshold, signature: None },
                wifi: Wifi { ssid: "hello".to_string(), strength: 100 }
            }).unwrap();

            sd.ws.write_message(tungstenite::Message::Text(status_pkt))
                .unwrap();
        }

        thread::sleep(time::Duration::from_secs(5))
    }
}