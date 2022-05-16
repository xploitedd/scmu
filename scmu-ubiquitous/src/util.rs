use bluer::Uuid;

pub type Result<T> = std::result::Result<T, String>;

pub trait ToErrString<T> {
    fn or_err_str(self) -> Result<T>;
}

impl <T> ToErrString<T> for bluer::Result<T> {
    fn or_err_str(self) -> Result<T> {
        self.or_else(|err| {
            let err_str = format!("[{}] {}", err.kind, err.message);
            Err(err_str)
        })
    }
}

impl <T> ToErrString<T> for std::result::Result<T, uuid::Error> {
    fn or_err_str(self) -> Result<T> {
        self.or_else(|err| {
            Err(err.to_string())
        })
    }
}

impl <T> ToErrString<T> for network_manager::errors::Result<T> {
    fn or_err_str(self) -> Result<T> {
        self.or_else(|err| {
            let err_str = format!("[{}] {}", err.0, err.0.description());
            Err(err_str)
        })
    }
}

pub fn parse_uuid(uuid: &str) -> Result<Uuid> {
    Ok(Uuid::parse_str(uuid).or_err_str()?)
}