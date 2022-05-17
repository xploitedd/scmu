use std::{any::Any, thread};

use network_manager::NetworkManager;
use tokio::sync::{mpsc::{Sender, Receiver, channel}, Mutex};

use crate::util::Result;

type WorkResult = Box<dyn Any + Send>;
type Work = Box<dyn (FnOnce(&NetworkManager) -> Result<WorkResult>) + Send>;

struct NmWorkerChannel {
    tx_w: Sender<Work>,
    rc_r: Receiver<Result<WorkResult>>
}

pub(super) struct NmWorker {
    mutex: Mutex<NmWorkerChannel>
}

impl NmWorker {
    pub fn new() -> Self {
        let (tx_w, mut rc_w) = channel::<Work>(1);
        let (tx_r, rc_r) = channel::<Result<WorkResult>>(1);

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

    pub async fn do_task<T : Any + Send>(&self, task: Work) -> Result<T> {
        let mut g = self.mutex
            .lock()
            .await;

        g.tx_w.send(task)
            .await
            .or(Err(String::from("Error executing operation")))?;

        let res = g.rc_r.recv()
            .await
            .unwrap_or(Err(String::from("Error receiving response")))?;

        let res_dc = res.downcast::<T>()
            .or(Err(String::from("Error converting to output type")))
            .unwrap();

        Ok(*res_dc)
    }
}