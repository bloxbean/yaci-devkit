socat TCP-LISTEN:${SOCAT_PORT},reuseaddr,fork UNIX-CONNECT:node-spo1/node.sock
