use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpListener;

pub struct Server {
    addr: String,
}

impl Server {
    pub fn new(addr: &str) -> Self {
        Self {
            addr: addr.to_string(),
        }
    }

    pub async fn start(&self) {
        let listener = TcpListener::bind(&self.addr).await.unwrap();
        println!("Rust TCP server 启动成功，监听端口: {}", self.addr);

        let (tx, mut rx) = tokio::sync::mpsc::unbounded_channel();

        // 监听 Ctrl+C，发关闭信号
        tokio::spawn(async move {
            tokio::signal::ctrl_c().await.unwrap();
            let _ = tx.send(());
        });

        let mut next_conn_id: u64 = 0;

        loop {
            tokio::select! {
                result = listener.accept() => {
                    let (mut socket, addr) = result.unwrap();
                    next_conn_id += 1;
                    let conn_id = next_conn_id;
                    println!("[#{conn_id}] 新连接: {}", addr);
                    tokio::spawn(async move {
                        let mut buf = vec![0u8; 1024];
                        loop {
                            match socket.read(&mut buf).await {
                                Ok(0) => {
                                    println!("[#{conn_id}] 连接关闭: {}", addr);
                                    return;
                                }
                                Ok(n) => {
                                    if socket.write_all(&buf[..n]).await.is_err() {
                                        return;
                                    }
                                }
                                Err(_) => return,
                            }
                        }
                    });
                }
                _ = rx.recv() => {
                    println!("\n收到关闭信号，优雅停机...");
                    break;
                }
            }
        }
    }
}
