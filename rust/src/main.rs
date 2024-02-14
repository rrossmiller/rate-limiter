use std::error::Error;

use reqwest::{self};
use serde_json::Value;

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    let x = reqwest::get("http://httpbin.org/json").await?;
    let txt = x.text().await?;
    let m: Value = serde_json::from_str(txt.as_str())?;
    println!("{:?}", m["slideshow"]["title"]);
    Ok(())
}
