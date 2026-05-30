# net-service

网络服务

多语言来实现，java-netty,golang,rust

# TCP服务

协议

fe 04 73 01 00 86 eb 02 19

fe是个魔法，04是个长度，第三个字节是代表消息类型第四个节字代表子消息类型，5-8是数据，最后一位是crc校验码
