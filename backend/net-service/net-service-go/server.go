package main

import (
	"context"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"os/signal"
	"sync/atomic"
	"syscall"
)

type Server struct {
	addr     string
	listener net.Listener
	connID   atomic.Uint64
}

func NewServer(addr string) *Server {
	return &Server{addr: addr}
}

func (s *Server) Start() {
	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	var err error
	s.listener, err = (&net.ListenConfig{}).Listen(ctx, "tcp", s.addr)
	if err != nil {
		log.Fatalf("启动失败: %v", err)
	}
	defer s.listener.Close()

	fmt.Printf("Go TCP server 启动成功，监听端口: %s\n", s.addr)

	go func() {
		<-ctx.Done()
		fmt.Println("\n收到关闭信号，优雅停机...")
		s.listener.Close()
	}()

	for {
		conn, err := s.listener.Accept()
		if err != nil {
			select {
			case <-ctx.Done():
				return
			default:
				log.Printf("accept 错误: %v", err)
				continue
			}
		}
		go s.handleConn(conn)
	}
}

func (s *Server) handleConn(conn net.Conn) {
	defer conn.Close()
	remoteAddr := conn.RemoteAddr().String()
	id := s.connID.Add(1)
	fmt.Printf("[#%d] 新连接: %s\n", id, remoteAddr)
	buf := make([]byte, 1024)
	for {
		n, err := conn.Read(buf)
		if err != nil {
			if err != io.EOF {
				log.Printf("读取错误: %v", err)
			}
			fmt.Printf("[#%d] 连接关闭: %s\n", id, remoteAddr)
			return
		}
		if _, err := conn.Write(buf[:n]); err != nil {
			return
		}
	}
}
