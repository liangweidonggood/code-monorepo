mod server;

use server::Server;

#[tokio::main]
async fn main() {
    let server = Server::new("0.0.0.0:8889");
    server.start().await;
}
