use std::{thread, sync::Arc};

use network_manager::*;
use tokio::{time::*, sync::{mpsc::{channel, Receiver, Sender}, Mutex}};

use crate::util::{Result, ToErrString};

type Work = Box<dyn (FnOnce(&NetworkManager) -> Result<()>) + Send>;

struct NmWorkerChannel {
    tx_w: Sender<Work>,
    rc_r: Receiver<Result<()>>
}

struct NmWorker {
    mutex: Mutex<NmWorkerChannel>
}

impl NmWorker {
    fn new() -> Self {
        let (tx_w, mut rc_w) = channel::<Work>(1);
        let (tx_r, rc_r) = channel::<Result<()>>(1);

        thread::spawn(move || {
            let nm = NetworkManager::new();
            loop {
                let rc_r = rc_w.blocking_recv();
                if let Some(res) = rc_r {
                    tx_r.blocking_send(res(&nm))
                        .unwrap_or_default();
                }
            }
        });

        NmWorker {
            mutex: Mutex::new(NmWorkerChannel { tx_w, rc_r })
        }
    }

    async fn do_task(&self, task: Work) -> Result<()> {
        let mut g = self.mutex
            .lock()
            .await;

        g.tx_w.send(task)
            .await
            .or(Err(String::from("Error executing operation")))?;

        g.rc_r.recv()
            .await
            .unwrap_or(Err(String::from("Error receiving response")))
    }
}

pub struct WifiManager {
    nm_worker: NmWorker
}
    
impl WifiManager {
    pub fn new() -> Self {
        let nm_worker = NmWorker::new();
        Self { nm_worker }
    }

    pub async fn get_access_points(&self) -> Result<Vec<AccessPoint>> {
        let (tx, mut rx) = channel::<Vec<AccessPoint>>(1);
        
        self.nm_worker.do_task(Box::new(move |nm| {
            let device = WifiManager::find_wifi_device(nm)?;
            let wd = device.as_wifi_device().unwrap();

            wd.request_scan().or_err_str()?;
            
            let access_points = wd.get_access_points().or_err_str()?;
            tx.blocking_send(access_points).unwrap();

            Ok(())
        })).await?;

        Ok(rx.recv().await.unwrap())
    }

    pub async fn connect_to_ap(&self, ap: &Arc<AccessPoint>, credentials: &Arc<AccessPointCredentials>) -> Result<()> {
        let ap_c = ap.clone();
        let credentials_c = credentials.clone();
        
        self.nm_worker.do_task(Box::new(move |nm| {
            WifiManager::try_delete_connection_by_ssid(nm, ap_c.ssid.as_str().unwrap());

            let wd = WifiManager::find_wifi_device(nm)?;
            let (_, con_state) = wd.as_wifi_device()
                .unwrap()
                .connect(&ap_c, &credentials_c)
                .or_err_str()?;
    
            match con_state {
                ConnectionState::Unknown |
                ConnectionState::Deactivated |
                ConnectionState::Deactivating => {
                    WifiManager::try_delete_connection_by_ssid(nm, ap_c.ssid.as_str().unwrap());
                    Err(String::from("Failed to establish connection"))
                },
                _ => Ok(())
            }
        })).await
    }

    pub async fn is_connected(&self) -> Result<bool> {
        let (tx, mut rx) = channel::<bool>(1);
        self.nm_worker.do_task(Box::new(move |nm| {
            let connectivity = nm.get_connectivity()
                .or_err_str()?;

            tx.blocking_send(connectivity == Connectivity::Full).unwrap();

            Ok(())
        })).await?;

        Ok(rx.recv().await.unwrap())
    }

    pub async fn wait_for_connection(&self) {
        while !self.is_connected().await.unwrap_or_default() {
            sleep(Duration::from_millis(200)).await;
        }
    }

    fn try_delete_connection_by_ssid(network_manager: &NetworkManager, ssid: &str) {
        let existent = network_manager.get_connections()
            .unwrap_or_default()
            .into_iter()
            .find(|con| {
                con.settings()
                    .ssid
                    .as_str()
                    .unwrap() == ssid
            });

        if existent.is_some() {
            existent.unwrap()
                .delete()
                .unwrap_or_default();
        }
    }

    fn find_wifi_device(network_manager: &NetworkManager) -> Result<Device> {
        let wifi_device: Option<Device> = network_manager.get_devices()
            .unwrap_or(vec![])
            .into_iter()
            .find(|dev| {
                *dev.device_type() == DeviceType::WiFi
            });
    
        match wifi_device {
            Some(device) => Ok(device),
            None => Err(String::from("No wifi device has been found"))
        }
    }
}