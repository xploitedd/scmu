use std::{vec, time::Duration, sync::Arc};

use bluer::gatt::local::{
    Service, 
    Characteristic, 
    CharacteristicRead, 
    ReqResult, 
    ReqError,
    CharacteristicNotify, 
    CharacteristicNotifyMethod, 
    CharacteristicWrite, 
    CharacteristicWriteMethod
};

use network_manager::{AccessPointCredentials, Security};
use serde::{Serialize, Deserialize};
use tokio::time;

use crate::{util::{Result, parse_uuid}, wifi::WifiManager};

async fn read_wifi_networks(wm: &'static WifiManager) -> ReqResult<Vec<u8>> {
    #[derive(Serialize)]
    struct AccessPointInfo {
        ssid: String,
        security: u32,
        strength: u32
    }

    let access_points = wm.get_access_points()
        .await
        .or(Err(ReqError::Failed))?;

    let access_points_list: Vec<AccessPointInfo> = access_points.into_iter()
        .filter(|v| v.ssid.as_str().unwrap().len() > 0)
        .map(|v| {
            AccessPointInfo {
                ssid: v.ssid.as_str().unwrap().to_string(),
                security: v.security.bits(),
                strength: v.strength
            }
        })
        .collect();

    let bytes = rmp_serde::to_vec(&access_points_list)
        .or(Err(ReqError::Failed))?;

    Ok(bytes)
}

async fn get_connection_status(wm: &'static WifiManager) -> ReqResult<Vec<u8>> {
    #[derive(Serialize)]
    struct IsConnected {
        is_connected: bool
    }

    let is_connected = wm.is_connected()
        .await
        .or(Err(ReqError::Failed))?;

    let res = IsConnected {
        is_connected
    };

    let bytes = rmp_serde::to_vec(&res)
        .or(Err(ReqError::Failed))?;

    Ok(bytes)
}

async fn connect_to_ap(wm: &'static WifiManager, data: &Vec<u8>) -> ReqResult<()> {
    #[derive(Deserialize)]
    struct AccessPointDetails {
        ssid: String,
        password: Option<String>,
        identity: Option<String>    
    }

    let ap_r: AccessPointDetails = rmp_serde::from_slice(data.as_slice())
        .or(Err(ReqError::Failed))?;

    let ap = wm.get_access_points()
        .await
        .or(Err(ReqError::Failed))?
        .into_iter()
        .find(|ap| ap.ssid.as_str().unwrap() == ap_r.ssid)
        .ok_or(ReqError::Failed)?;

    let ap_creds = match ap.security {
        Security::NONE => AccessPointCredentials::None,
        Security::WEP => AccessPointCredentials::Wep {
            passphrase: ap_r.password.ok_or(ReqError::Failed)?
        },
        Security::WPA |
        Security::WPA2 => AccessPointCredentials::Wpa {
            passphrase: ap_r.password.ok_or(ReqError::Failed)?
        },
        Security::ENTERPRISE => AccessPointCredentials::Enterprise {
            identity: ap_r.identity.ok_or(ReqError::Failed)?, 
            passphrase: ap_r.password.ok_or(ReqError::Failed)?
        },
        _ => Err(ReqError::Failed)?
    };

    let ap_a = Arc::new(ap);
    let ap_creds_a = Arc::new(ap_creds);

    wm.connect_to_ap(&ap_a, &ap_creds_a)
        .await
        .or(Err(ReqError::Failed))?;

    Ok(())
}

pub struct WifiConfigurationService {}

impl WifiConfigurationService {
    pub fn create_service(wifi: &'static WifiManager) -> Result<Service> {
        Ok(Service {
            uuid: parse_uuid("ddbc279f-61eb-484a-bbc2-f65f2d4325be")?,
            primary: true,
            characteristics: vec![
                Characteristic {
                    uuid: parse_uuid("a6bb77a3-e0d5-4841-b424-55a7ddc9f1cb")?,
                    read: Some(CharacteristicRead {
                        read: true,
                        fun: Box::new(move |_| {
                            Box::pin(read_wifi_networks(wifi))
                        }),
                        ..Default::default()
                    }),
                    notify: Some(CharacteristicNotify {
                        notify: true,
                        method: CharacteristicNotifyMethod::Fun(Box::new(move |mut nt| {
                            Box::pin(async move { 
                                while !nt.is_stopped() {
                                    let networks = read_wifi_networks(wifi)
                                        .await;

                                    if let Ok(res) = networks {
                                        nt.notify(res)
                                            .await
                                            .unwrap_or_default();
                                    }

                                    time::sleep(Duration::from_secs(10)).await;
                                }
                            })
                        })),
                        ..Default::default()
                    }),
                    ..Default::default()
                },
                Characteristic {
                    uuid: parse_uuid("3fa8daec-bb2a-465c-b5e5-5735a5c7acbd")?,
                    read: Some(CharacteristicRead {
                        read: true,
                        fun: Box::new(move |_| {
                            Box::pin(get_connection_status(wifi))
                        }),
                        ..Default::default()
                    }),
                    notify: Some(CharacteristicNotify {
                        notify: true,
                        method: CharacteristicNotifyMethod::Fun(Box::new(move |mut nt| {
                            Box::pin(async move {
                                loop {
                                    let is_connected = get_connection_status(wifi)
                                        .await;

                                    if let Ok(res) = is_connected {
                                        nt.notify(res)
                                            .await
                                            .unwrap_or_default();
                                    }

                                    time::sleep(Duration::from_secs(3)).await;
                                }
                            })
                        })),
                        ..Default::default()
                    }),
                    ..Default::default()
                },
                Characteristic {
                    uuid: parse_uuid("beb1ed79-7b42-4bd1-968c-7d6d4c10eaa6")?,
                    write: Some(CharacteristicWrite {
                        write: true,
                        method: CharacteristicWriteMethod::Fun(Box::new(move |data, _| {
                            Box::pin(async move { connect_to_ap(wifi, &data).await })
                        })),
                        ..Default::default()
                    }),
                    ..Default::default()
                }
            ],
            ..Default::default()
        })
    }
}