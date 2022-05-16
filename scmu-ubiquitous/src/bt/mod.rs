use bluer::{
    adv::{
        AdvertisementHandle,
        Advertisement
    }, 
    agent::{
        AgentHandle,
        Agent
    }, 
    gatt::local::{
        ApplicationHandle,
        Application
    }, 
    Session,
    Adapter
};

pub mod services;

use crate::util::{Result, ToErrString};

pub struct Bluetooth {
    agent_handle: AgentHandle,
    adapter: Adapter,
    adv_handle: AdvertisementHandle,
    app_handle: Option<ApplicationHandle>
}

impl Bluetooth {
    pub async fn new(advertisement: Advertisement) -> Result<Self> {
        let session = Session::new()
            .await
            .or_err_str()?;

        let agent_handle = Bluetooth::create_agent(&session)
            .await?;

        let adapter = Bluetooth::get_adapter(&session)
            .await?;

        let adv_handle = Bluetooth::create_advertisement(&adapter, advertisement)
            .await?;

        return Ok(Bluetooth {
            agent_handle,
            adapter,
            adv_handle,
            app_handle: None
        })
    }

    pub async fn start_app(&mut self, application: Application) -> Result<()> {
        let app_handle = self.adapter
            .serve_gatt_application(application)
            .await
            .or_err_str()?;

        self.app_handle = Some(app_handle);
        Ok(())
    }

    async fn get_adapter(session: &Session) -> Result<Adapter> {
        let adapter = session.default_adapter()
            .await
            .or_err_str()?;

        adapter.set_powered(true)
            .await
            .or_err_str()?;

        Ok(adapter)
    }

    async fn create_agent(session: &Session) -> Result<AgentHandle> {
        let agent = Agent {
            request_default: true,
            ..Default::default()
        };

        session.register_agent(agent)
            .await
            .or_err_str()
    }

    async fn create_advertisement(
        adapter: &Adapter,
        advertisement: Advertisement
    ) -> Result<AdvertisementHandle> {
        adapter.advertise(advertisement)
            .await
            .or_err_str()
    }
}

impl Drop for Bluetooth {
    fn drop(&mut self) {
        drop(&self.adv_handle);
        drop(&self.agent_handle);
        drop(&self.app_handle);
    }
}