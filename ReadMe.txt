一、协议格式
VER | CMD | RSV |

VER:
0x01

CMD:
0x01 登录 LEN | NAME(N)

0x11 登录响应 REP

0x13 连接请求 SRC.ADDR(4) | SRC.PORT(2) | ATYP | DST.ADDR(N) | DST.PORT(2)

0x03 连接响应 SRC.ADDR(4) | SRC.PORT(2) | REP | ATYP | DST.ADDR(N) | DST.PORT(2)
REP: 0x00 成功

0x14 关闭客户端

0x30 数据包 SRC.ADDR(4) | SRC.PORT(2) | LEN(2) | DATA(N)

0x31 心跳包

0x32 连接关闭 SRC.ADDR(4) | SRC.PORT(2)