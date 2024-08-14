(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[209],{8767:function(e,n,s){(window.__NEXT_P=window.__NEXT_P||[]).push(["/multi-node-setup",function(){return s(1731)}])},1731:function(e,n,s){"use strict";s.r(n),s.d(n,{__toc:function(){return r}});var i=s(5893),t=s(2673),o=s(7913),a=s(8426);s(9128);var l=s(2643);let r=[{depth:2,value:"Multi Node Setup (Experimental)",id:"multi-node-setup-experimental"},{depth:3,value:"1. Setup Additional Node folders",id:"1-setup-additional-node-folders"},{depth:4,value:"Additional nodes on the same machine",id:"additional-nodes-on-the-same-machine"},{depth:4,value:"Additional nodes on different machines",id:"additional-nodes-on-different-machines"},{depth:3,value:"2. Start the main devnet node",id:"2-start-the-main-devnet-node"},{depth:3,value:"3. Start DevKit instance for the additional node",id:"3-start-devkit-instance-for-the-additional-node"},{depth:3,value:"4. Join to the main node to download the genesis files and create the node",id:"4-join-to-the-main-node-to-download-the-genesis-files-and-create-the-node"},{depth:3,value:"5. Start the additional node",id:"5-start-the-additional-node"},{depth:3,value:"6. Register the pool for new block producing node (Only for BP nodes)",id:"6-register-the-pool-for-new-block-producing-node-only-for-bp-nodes"}];function _createMdxContent(e){let n=Object.assign({h2:"h2",p:"p",strong:"strong",h3:"h3",h4:"h4",code:"code",pre:"pre",span:"span",a:"a",ul:"ul",li:"li",ol:"ol"},(0,l.a)(),e.components);return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.h2,{id:"multi-node-setup-experimental",children:"Multi Node Setup (Experimental)"}),"\n",(0,i.jsxs)(n.p,{children:["While the primary purpose of DevKit is to provide a simple way to get started with a single devnet node, it is possible to use\nDevKit to create a multi-node setup. The additional nodes can be mix of ",(0,i.jsx)(n.strong,{children:"block producing"})," nodes and ",(0,i.jsx)(n.strong,{children:"relay"})," nodes."]}),"\n",(0,i.jsx)(n.p,{children:"A multi-node setup can be useful for various testing scenarios."}),"\n",(0,i.jsxs)(n.p,{children:["The additional nodes can be on the ",(0,i.jsx)(n.strong,{children:"same machine"})," or on ",(0,i.jsx)(n.strong,{children:"different machines"}),". There are separate commands to start a block producing node or a relay node."]}),"\n",(0,i.jsx)(n.p,{children:(0,i.jsx)(n.strong,{children:"You need to following steps in order to setup a multi-node setup:"})}),"\n",(0,i.jsx)(n.h3,{id:"1-setup-additional-node-folders",children:"1. Setup Additional Node folders"}),"\n",(0,i.jsx)(n.h4,{id:"additional-nodes-on-the-same-machine",children:"Additional nodes on the same machine"}),"\n",(0,i.jsxs)(n.p,{children:["In this case, you can copy the original ",(0,i.jsx)(n.code,{children:"devkit"})," installation directory and rename it to ",(0,i.jsx)(n.code,{children:"devkit2"})," or ",(0,i.jsx)(n.code,{children:"node2"})," etc."]}),"\n",(0,i.jsxs)(n.p,{children:["Update the ",(0,i.jsx)(n.code,{children:"config/env"})," file to use a different ",(0,i.jsx)(n.strong,{children:"node name"})," and ",(0,i.jsx)(n.strong,{children:"ports"})," for the additional node."]}),"\n",(0,i.jsx)(n.pre,{"data-language":"text","data-theme":"default",children:(0,i.jsxs)(n.code,{"data-language":"text","data-theme":"default",children:[(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"node=node2"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"}})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"#######################################################"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"# Ports"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"#######################################################"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"HOST_N2N_PORT=3002"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"HOST_N2C_SOCAT_PORT=3334"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"HOST_STORE_API_PORT=8081"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"HOST_VIEWER_PORT=5174"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"HOST_CLUSTER_API_PORT=10001"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"HOST_SUBMIT_API_PORT=8091"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"HOST_OGMIOS_PORT=1338"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"HOST_KUPO_PORT=1443"})})]})}),"\n",(0,i.jsx)(n.h4,{id:"additional-nodes-on-different-machines",children:"Additional nodes on different machines"}),"\n",(0,i.jsx)(n.p,{children:"Install DevKit on the additional machines using standard installation instructions."}),"\n",(0,i.jsx)(n.h3,{id:"2-start-the-main-devnet-node",children:"2. Start the main devnet node"}),"\n",(0,i.jsxs)(n.p,{children:["Start the first devnet node using the ",(0,i.jsx)(n.code,{children:"start"})," command with the standard steps. This will also act as our ",(0,i.jsx)(n.strong,{children:"main bootstrap node"})," or ",(0,i.jsx)(n.strong,{children:"admin node"}),"."]}),"\n",(0,i.jsx)(n.pre,{"data-language":"bash","data-theme":"default",children:(0,i.jsxs)(n.code,{"data-language":"bash","data-theme":"default",children:[(0,i.jsxs)(n.span,{className:"line",children:[(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-function)"},children:"devkit"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"start"})]}),"\n",(0,i.jsx)(n.span,{className:"line",children:" "}),"\n",(0,i.jsxs)(n.span,{className:"line",children:[(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-function)"},children:"yaci-cli>"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"create-node"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"-o"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"--start"})]})]})}),"\n",(0,i.jsxs)(n.p,{children:[(0,i.jsx)(n.strong,{children:"Note:"})," When we start a DevKit instance, it exposes a ",(0,i.jsx)(n.strong,{children:"cluster API endpoint"})," (default port ",(0,i.jsx)(n.strong,{children:"10000"}),") which is used to get the genesis files and other configuration\ninformation while starting additional nodes."]}),"\n",(0,i.jsxs)(n.p,{children:["You can check available cluster apis here ",(0,i.jsx)(n.a,{href:"http://localhost:10000/swagger-ui/index.html",children:"http://localhost:10000/swagger-ui/index.html"})]}),"\n",(0,i.jsx)(n.h3,{id:"3-start-devkit-instance-for-the-additional-node",children:"3. Start DevKit instance for the additional node"}),"\n",(0,i.jsx)(n.p,{children:"Go to the additional node folder if it is on the same machine or login to additional machine."}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"Start devkit in the additional node folder"}),"\n"]}),"\n",(0,i.jsx)(n.pre,{"data-language":"bash","data-theme":"default",children:(0,i.jsxs)(n.code,{"data-language":"bash","data-theme":"default",children:[(0,i.jsxs)(n.span,{className:"line",children:[(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-function)"},children:"devkit"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"start"})]}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-function)"},children:"or,"})}),"\n",(0,i.jsxs)(n.span,{className:"line",children:[(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-function)"},children:"./bin/devkit.sh"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"start"})]})]})}),"\n",(0,i.jsx)(n.h3,{id:"4-join-to-the-main-node-to-download-the-genesis-files-and-create-the-node",children:"4. Join to the main node to download the genesis files and create the node"}),"\n",(0,i.jsxs)(n.ol,{children:["\n",(0,i.jsxs)(n.li,{children:["Join to create a ",(0,i.jsx)(n.strong,{children:"block producing"})," node"]}),"\n"]}),"\n",(0,i.jsx)(n.pre,{"data-language":"bash","data-theme":"default",children:(0,i.jsx)(n.code,{"data-language":"bash","data-theme":"default",children:(0,i.jsxs)(n.span,{className:"line",children:[(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-function)"},children:"yaci-cli:>"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"join"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"--admin-url"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"http://"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-keyword)"},children:"<"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"main_node_i"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"p"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-keyword)"},children:">"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:":10000"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"--bp"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"--overwrite"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"--overwrite--pool-keys"})]})})}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"--admin-url"})," is the cluster API endpoint of the main node. You can't use ",(0,i.jsx)(n.code,{children:"localhost"})," here, you need to use the IP address of the main node."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"--bp"})," is used to create a block producing node."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"--overwrite"})," is used to overwrite the existing configuration files or node folder if it already exists."]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"--overwrite-pool-keys"})," is used to overwrite the pool keys with new keys. To use the previously generated pool keys, you can skip this flag."]}),"\n"]}),"\n",(0,i.jsx)(n.p,{children:"This command will download the genesis files and other configuration files from the main node, create keys required\nfor the new block producing node and create the node folder."}),"\n",(0,i.jsxs)(n.ol,{start:"2",children:["\n",(0,i.jsxs)(n.li,{children:["Join to create a ",(0,i.jsx)(n.strong,{children:"relay"})," node"]}),"\n"]}),"\n",(0,i.jsxs)(n.p,{children:["To create a ",(0,i.jsx)(n.strong,{children:"relay"})," node, just skip the ",(0,i.jsx)(n.code,{children:"--bp"})," flag."]}),"\n",(0,i.jsx)(n.pre,{"data-language":"bash","data-theme":"default",children:(0,i.jsx)(n.code,{"data-language":"bash","data-theme":"default",children:(0,i.jsxs)(n.span,{className:"line",children:[(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-function)"},children:"yaci-cli:>"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"join"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"--admin-url"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"http://"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-keyword)"},children:"<"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"main_node_i"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"p"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-keyword)"},children:">"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:":10000"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"--overwrite"})]})})}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.code,{children:"--admin-url"})," is the cluster API endpoint of the main node. You can't use ",(0,i.jsx)(n.code,{children:"localhost"})," here, you need to use the IP address of the main node."]}),"\n"]}),"\n",(0,i.jsxs)(n.p,{children:[(0,i.jsx)(n.strong,{children:"Important:"})," Don't use ",(0,i.jsx)(n.code,{children:"--name"})," flag in join command. This option will be removed in future releases. Using this flag may cause issues in the current version."]}),"\n",(0,i.jsx)(n.h3,{id:"5-start-the-additional-node",children:"5. Start the additional node"}),"\n",(0,i.jsxs)(n.p,{children:["After the join command is successful, you should be already in bp or relay node context in Yaci CLI.\nNow, you can start the additional node using the ",(0,i.jsx)(n.code,{children:"start"})," command."]}),"\n",(0,i.jsx)(n.pre,{"data-language":"bash","data-theme":"default",children:(0,i.jsx)(n.code,{"data-language":"bash","data-theme":"default",children:(0,i.jsxs)(n.span,{className:"line",children:[(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-function)"},children:"devnet-peer/bp:default>"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"start"})]})})}),"\n",(0,i.jsx)(n.h3,{id:"6-register-the-pool-for-new-block-producing-node-only-for-bp-nodes",children:"6. Register the pool for new block producing node (Only for BP nodes)"}),"\n",(0,i.jsx)(n.p,{children:"If you have created a new block producing node, you need to register the pool for the new node, so that it can participate in the slot leader selection process."}),"\n",(0,i.jsx)(n.pre,{"data-language":"bash","data-theme":"default",children:(0,i.jsx)(n.code,{"data-language":"bash","data-theme":"default",children:(0,i.jsxs)(n.span,{className:"line",children:[(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-function)"},children:"devnet-peer/bp:default>"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"register-pool"})]})})}),"\n",(0,i.jsx)(n.p,{children:"The above command will register the pool for the new block producing node with default options."}),"\n",(0,i.jsx)(n.p,{children:"During the registration process, you can also see pool's owner address. You can now topup the owner address with some funds\nto increase the stake amount of the pool. Topup enough ada, so that the new pool is eligible to create blocks."}),"\n",(0,i.jsx)(n.p,{children:"In the below example, we are topping up the owner address with 650,000 Ada."}),"\n",(0,i.jsx)(n.pre,{"data-language":"bash","data-theme":"default",children:(0,i.jsx)(n.code,{"data-language":"bash","data-theme":"default",children:(0,i.jsxs)(n.span,{className:"line",children:[(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-function)"},children:"devnet-peer/bp:default>"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"topup"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-string)"},children:"addr_test1qr3ew7uu9eg8asdupaqrpf9t7ug4l62jcxnpy96vl68xhwmvnkyw67tk4wptp7ggdsxw7ty5pwt4s4jjp7ykx68wrzwszqdxm8"}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,i.jsx)(n.span,{style:{color:"var(--shiki-token-constant)"},children:"650000"})]})})}),"\n",(0,i.jsxs)(n.p,{children:[(0,i.jsx)(n.strong,{children:"Note"}),": You can also provide additional options while registering the pool. (Optional)"]}),"\n",(0,i.jsx)(n.pre,{"data-language":"text","data-theme":"default",children:(0,i.jsxs)(n.code,{"data-language":"text","data-theme":"default",children:[(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       --ticker or -t String"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       Pool Ticker"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       [Optional, default = DEFAULT_POOL]"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"}})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       --metadata-hash or -m String"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       Metadata Hash"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       [Optional, default = 6bf124f217d0e5a0a8adb1dbd8540e1334280d49ab861127868339f43b3948af]"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"}})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       --pledge or -p BigInteger"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       Pledge. Default 10,000,000 lovelace"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       [Optional, default = 10000000]"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"}})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       --cpst or -c BigInteger"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       Cost in lovelace. Default 340 Ada"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       [Optional, default = 340000000]"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"}})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       --margin double"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       Relay Host. Default 0.02"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       [Optional, default = 0.02]"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"}})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       --relay-host or -r String"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       Relay Host"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       [Optional, default = 127.0.0.1]"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"}})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       --relay-port or -p int"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       Relay Port"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       [Optional, default = 0]"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"}})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       --help or -h"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       help for register-pool"})}),"\n",(0,i.jsx)(n.span,{className:"line",children:(0,i.jsx)(n.span,{style:{color:"var(--shiki-color-text)"},children:"       [Optional]"})})]})})]})}let d={MDXContent:function(){let e=arguments.length>0&&void 0!==arguments[0]?arguments[0]:{},{wrapper:n}=Object.assign({},(0,l.a)(),e.components);return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(_createMdxContent,{...e})}):_createMdxContent(e)},pageOpts:{filePath:"pages/multi-node-setup.mdx",route:"/multi-node-setup",timestamp:1723629398e3,pageMap:[{kind:"Meta",data:{index:"Introduction",yaci_cli_distribution:"Yaci CLI (Non Docker Dist)",how_to_use:"How to use",about:{title:"About",type:"page"},services:"Services",commands:"Yaci CLI Commands","node-configuration":"Node Configuration","multi-node-setup":"Multi-node setup",tutorials:"Tutorials"}},{kind:"MdxPage",name:"about",route:"/about"},{kind:"MdxPage",name:"commands",route:"/commands"},{kind:"MdxPage",name:"how_to_use",route:"/how_to_use"},{kind:"MdxPage",name:"index",route:"/"},{kind:"MdxPage",name:"multi-node-setup",route:"/multi-node-setup"},{kind:"MdxPage",name:"node-configuration",route:"/node-configuration"},{kind:"MdxPage",name:"services",route:"/services"},{kind:"Folder",name:"tutorials",route:"/tutorials",children:[{kind:"Meta",data:{meshjs:"MeshJS",ccl:"Cardano Client Lib (Java)","lucid-evolution":"Lucid Evolution"}},{kind:"Folder",name:"ccl",route:"/tutorials/ccl",children:[{kind:"MdxPage",name:"overview",route:"/tutorials/ccl/overview"},{kind:"Meta",data:{overview:"Overview"}}]},{kind:"Folder",name:"lucid-evolution",route:"/tutorials/lucid-evolution",children:[{kind:"MdxPage",name:"overview",route:"/tutorials/lucid-evolution/overview"},{kind:"Meta",data:{overview:"Overview"}}]},{kind:"Folder",name:"meshjs",route:"/tutorials/meshjs",children:[{kind:"MdxPage",name:"overview",route:"/tutorials/meshjs/overview"},{kind:"Meta",data:{overview:"Overview"}}]}]},{kind:"MdxPage",name:"yaci_cli_distribution",route:"/yaci_cli_distribution"}],flexsearch:{codeblocks:!0},title:"Multi Node Setup",headings:r},pageNextRoute:"/multi-node-setup",nextraLayout:o.ZP,themeConfig:a.Z};n.default=(0,t.j)(d)},8426:function(e,n,s){"use strict";var i=s(5893);s(7294);let t={logo:(0,i.jsx)("span",{children:(0,i.jsx)("b",{children:"Yaci DevKit"})}),project:{link:"https://github.com/bloxbean/yaci-devkit"},chat:{link:"https://discord.gg/JtQ54MSw6p"},docsRepositoryBase:"https://github.com/bloxbean/yaci-devkit/tree/develop/docs",footer:{text:"\xa9 2024 BloxBean project"},useNextSeoProps:()=>({titleTemplate:"%s – Yaci DevKit"}),head:(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)("meta",{property:"description",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"}),(0,i.jsx)("meta",{property:"og:title",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet"}),(0,i.jsx)("meta",{property:"og:description",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"})]})};n.Z=t},5789:function(){}},function(e){e.O(0,[774,543,888,179],function(){return e(e.s=8767)}),_N_E=e.O()}]);