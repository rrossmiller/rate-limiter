use std::{error::Error, sync};

use reqwest::{self};
use serde_json::Value;

type Watcher = sync::Mutex<i8>;
#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    // let x = reqwest::get("http://httpbin.org/json").await?;
    let i: Watcher = sync::Mutex::new(0);
    for _ in 0..10 {
        tokio::spawn(async move {
            run().await.unwrap();
            let mut lock = i.lock().unwrap();
            *lock += 1;
        });
    }
    tokio::spawn(async move {
        run().await.unwrap();
        *i.lock().unwrap() += 1;
    });
    Ok(())
}

async fn run() -> Result<(), Box<dyn Error>> {
    // task queue
    // init workers to empty queue

    let x = reqwest::get("http://localhost:3000/?i=1").await?;
    let txt = x.text().await?;
    let m: Value = serde_json::from_str(txt.as_str())?;
    // println!("{:?}", m["slideshow"]["title"]);
    println!("{:?}", m["id"]);
    Ok(())
}
