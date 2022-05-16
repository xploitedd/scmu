use std::io::stdin;

use bluer::{adv::Advertisement, gatt::local::Application};
use bt::services::wifi::WifiConfigurationService;
use util::Result;

use crate::wifi::WifiManager;
use crate::bt::Bluetooth;

mod util;
mod wifi;
mod bt;

#[tokio::main]
async fn main() -> Result<()> {
    let wm: &'static WifiManager = Box::leak(Box::new(WifiManager::new()));

    let mut bt = Bluetooth::new(Advertisement {
        discoverable: Some(true),
        local_name: Some("joe".to_string()),
        ..Default::default()
    }).await.unwrap();

    bt.start_app(Application {
        services: vec![
            WifiConfigurationService::create_service(wm)?
        ],
        ..Default::default()
    }).await.unwrap();

    loop {
        match stdin().read_line(&mut String::new()) {
            Ok(_) => {
                drop(bt);
                break;
            },
            Err(err) => panic!("{}", err)
        }
    }

    Ok(())
}