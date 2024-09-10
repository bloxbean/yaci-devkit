(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[794],{8016:function(e,s,t){(window.__NEXT_P=window.__NEXT_P||[]).push(["/how_to_use",function(){return t(9568)}])},9568:function(e,s,t){"use strict";t.r(s),t.d(s,{__toc:function(){return l}});var n=t(5893),a=t(2673),o=t(7913),i=t(8426);t(9128);var r=t(2643);let l=[{depth:2,value:"DevKit Script",id:"devkit-script"},{depth:3,value:"To start the DevKit docker compose",id:"to-start-the-devkit-docker-compose"},{depth:3,value:"Fund default test accounts (Optional)",id:"fund-default-test-accounts-optional"},{depth:3,value:"Enable Ogmios and Kupo Support (Optional)",id:"enable-ogmios-and-kupo-support-optional"},{depth:3,value:"To stop DevKit",id:"to-stop-devkit"},{depth:2,value:"Yaci CLI - Few Key Commands",id:"yaci-cli---few-key-commands"},{depth:3,value:"Create a default devnet",id:"create-a-default-devnet"},{depth:3,value:"Create a default devnet node with Conway Era",id:"create-a-default-devnet-node-with-conway-era"},{depth:3,value:"Create a devnet with custom slots per epoch",id:"create-a-devnet-with-custom-slots-per-epoch"},{depth:3,value:"To reset cluster's data",id:"to-reset-clusters-data"},{depth:3,value:"To stop",id:"to-stop"},{depth:3,value:"To fund a new address",id:"to-fund-a-new-address"},{depth:3,value:"To check utxos at an address",id:"to-check-utxos-at-an-address"},{depth:3,value:"To check devnet and url info",id:"to-check-devnet-and-url-info"},{depth:3,value:"Default Pool Id",id:"default-pool-id"},{depth:2,value:"Query Devnet's Cardano Node using cardano-cli",id:"query-devnets-cardano-node-using-cardano-cli"}];function _createMdxContent(e){let s=Object.assign({h2:"h2",p:"p",strong:"strong",code:"code",pre:"pre",span:"span",h3:"h3",a:"a"},(0,r.a)(),e.components);return(0,n.jsxs)(n.Fragment,{children:[(0,n.jsx)(s.h2,{id:"devkit-script",children:"DevKit Script"}),"\n",(0,n.jsxs)(s.p,{children:["For ",(0,n.jsx)(s.strong,{children:"zip"})," based installation, you can find the ",(0,n.jsx)(s.code,{children:"devkit.sh"})," script under the ",(0,n.jsx)(s.code,{children:"bin"})," folder. This script is used to manage the DevKit containers and Yaci CLI."]}),"\n",(0,n.jsxs)(s.p,{children:["For ",(0,n.jsx)(s.strong,{children:"curl"})," based installation, you can find the ",(0,n.jsx)(s.code,{children:"devkit.sh"})," script in $HOME/.yaci-devkit/bin folder.\nIf the bin folder is in your PATH, you can just use ",(0,n.jsx)(s.code,{children:"devkit"})," command to manage the DevKit containers and Yaci CLI."]}),"\n",(0,n.jsx)(s.pre,{"data-language":"shell","data-theme":"default",children:(0,n.jsxs)(s.code,{"data-language":"shell","data-theme":"default",children:[(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"Options:"})}),"\n",(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"  "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"start"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"   "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"Start"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"the"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"DevKit"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"containers"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"and"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"CLI."})]}),"\n",(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"  "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"stop"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"    "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"Stop"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"the"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"DevKit"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"containers."})]}),"\n",(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"  "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"cli"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"     "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"Query"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"the"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"Cardano"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"node"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"in"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"the"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"DevKit"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"container"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"using"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"cardano-cli."})]}),"\n",(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"  "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"ssh"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"     "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"Establish"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"an"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"SSH"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"connection"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"to"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"the"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"DevKit"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"container."})]}),"\n",(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"  "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"info"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"    "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"Display"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"information"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"about"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"the"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"Dev"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"Node."})]}),"\n",(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"  "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"version"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"Display"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"the"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"version"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"of"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"the"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"DevKit."})]}),"\n",(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"  "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"help"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"    "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"Display"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"this"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"help"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"message."})]}),"\n",(0,n.jsx)(s.span,{className:"line",children:" "})]})}),"\n",(0,n.jsx)(s.h3,{id:"to-start-the-devkit-docker-compose",children:"To start the DevKit docker compose"}),"\n",(0,n.jsx)(s.p,{children:"To start the DevKit containers and yaci-cli."}),"\n",(0,n.jsx)(s.p,{children:"For, curl based installation, you can use the following command."}),"\n",(0,n.jsx)(s.pre,{"data-language":"shell","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"shell","data-theme":"default",children:(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"devkit"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"start"})]})})}),"\n",(0,n.jsx)(s.p,{children:"For, zip based installation, you can use the following command."}),"\n",(0,n.jsx)(s.pre,{"data-language":"shell","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"shell","data-theme":"default",children:(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"./devkit.sh"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"start"})]})})}),"\n",(0,n.jsxs)(s.p,{children:[(0,n.jsx)(s.strong,{children:"Note:"})," If you have some ",(0,n.jsx)(s.strong,{children:"ports"})," already in use, please make sure the mentioned ports in ",(0,n.jsx)(s.code,{children:"config/env"})," file are free.\nYou can also change the ports in ",(0,n.jsx)(s.code,{children:"config/env"})," file. Any changes to ",(0,n.jsx)(s.code,{children:"env"})," file will be applied when you restart the docker compose."]}),"\n",(0,n.jsx)(s.h3,{id:"fund-default-test-accounts-optional",children:"Fund default test accounts (Optional)"}),"\n",(0,n.jsxs)(s.p,{children:["Update ",(0,n.jsx)(s.code,{children:"config/env"})," file to include your test Cardano addresses to automatically topup Ada."]}),"\n",(0,n.jsx)(s.pre,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"topup_addresses=<address1>:<ada_value>,<address2><ada_value>"})})})}),"\n",(0,n.jsx)(s.p,{children:(0,n.jsx)(s.strong,{children:"Example"})}),"\n",(0,n.jsx)(s.pre,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"topup_addresses=addr_test1qzlwg5c3mpr0cz5td0rvr5rvcgf02al05cqgd2wzv7pud6chpzk4elx4jh2f7xtftjrdxddr88wg6sfszu8r3gktpjtqrr00q9:20000,addr_test1qqwpl7h3g84mhr36wpetk904p7fchx2vst0z696lxk8ujsjyruqwmlsm344gfux3nsj6njyzj3ppvrqtt36cp9xyydzqzumz82:10000"})})})}),"\n",(0,n.jsxs)(s.p,{children:[(0,n.jsx)(s.strong,{children:"Important:"})," After updating env file, you need to restart the docker compose using ",(0,n.jsx)(s.code,{children:"devkit.sh stop"})," and ",(0,n.jsx)(s.code,{children:"devkit.sh start"})," options."]}),"\n",(0,n.jsxs)(s.p,{children:[(0,n.jsx)(s.strong,{children:"Note:"})," You can also use the ",(0,n.jsx)(s.code,{children:"topup"})," command in Yaci CLI to fund your test addresses later."]}),"\n",(0,n.jsx)(s.h3,{id:"enable-ogmios-and-kupo-support-optional",children:"Enable Ogmios and Kupo Support (Optional)"}),"\n",(0,n.jsxs)(s.p,{children:["Yaci DevKit bundles both Ogmios and Kupo. However, Kupo is not enabled by default. To activate both Ogmios and Kupo support,\nset ",(0,n.jsx)(s.code,{children:"ogmios_enabled"})," & ",(0,n.jsx)(s.code,{children:"kupo_enabled"})," flag in ",(0,n.jsx)(s.code,{children:"env"})," file to true. Alternatively, you can enable both Ogmios & Kupo support using ",(0,n.jsx)(s.code,{children:"enable-kupomios"})," command in Yaci CLI."]}),"\n",(0,n.jsx)(s.h3,{id:"to-stop-devkit",children:"To stop DevKit"}),"\n",(0,n.jsxs)(s.p,{children:["Use ",(0,n.jsx)(s.code,{children:"devkit.sh"})," script to stop the DevKit containers."]}),"\n",(0,n.jsx)(s.p,{children:"For, curl based installation, you can use the following command."}),"\n",(0,n.jsx)(s.pre,{"data-language":"shell","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"shell","data-theme":"default",children:(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"devkit"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"stop"})]})})}),"\n",(0,n.jsx)(s.p,{children:"For, zip based installation, you can use the following command."}),"\n",(0,n.jsx)(s.pre,{"data-language":"shell","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"shell","data-theme":"default",children:(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"./devkit.sh"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"stop"})]})})}),"\n",(0,n.jsx)(s.h2,{id:"yaci-cli---few-key-commands",children:"Yaci CLI - Few Key Commands"}),"\n",(0,n.jsx)(s.p,{children:"This section explains a few key commands specific to Yaci CLI."}),"\n",(0,n.jsx)(s.h3,{id:"create-a-default-devnet",children:"Create a default devnet"}),"\n",(0,n.jsx)(s.pre,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"yaci-cli:>create-node"})})})}),"\n",(0,n.jsx)(s.p,{children:'To overwrite data or reset the existing default devnet, use the "-o" flag.\nUse --start flag to start the devnet after creation.'}),"\n",(0,n.jsx)(s.pre,{"data-language":"text","data-theme":"default",children:(0,n.jsxs)(s.code,{"data-language":"text","data-theme":"default",children:[(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"yaci-cli:>create-node -o"})}),"\n",(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"or,"})}),"\n",(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"yaci-cli:>create-node -o --start"})})]})}),"\n",(0,n.jsxs)(s.p,{children:[(0,n.jsx)(s.strong,{children:"Known Issue:"})," Yaci DevKit uses a share folder to store the data on host machine. In some setup, this causes issue due to permission.\nIf you face similar issue and not able to start the devnet, you can remove ",(0,n.jsx)(s.code,{children:"volumes"})," section from ",(0,n.jsx)(s.code,{children:"docker-compose.yml"})," file and restart the docker compose.\nIt should work fine and create the devnet data in the docker container itself. Please check this ",(0,n.jsx)(s.a,{href:"https://github.com/bloxbean/yaci-devkit/issues/11",children:"issue"})," for more details."]}),"\n",(0,n.jsx)(s.h3,{id:"create-a-default-devnet-node-with-conway-era",children:"Create a default devnet node with Conway Era"}),"\n",(0,n.jsxs)(s.p,{children:["By default, Yaci DevKit creates a devnet with ",(0,n.jsx)(s.strong,{children:"Babbage"})," era. If you want to create a devnet with ",(0,n.jsx)(s.strong,{children:"Conway"})," era, use the following command."]}),"\n",(0,n.jsx)(s.pre,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"yaci-cli:>create-node -o --era conway"})})})}),"\n",(0,n.jsx)(s.h3,{id:"create-a-devnet-with-custom-slots-per-epoch",children:"Create a devnet with custom slots per epoch"}),"\n",(0,n.jsx)(s.p,{children:"To create devnet with a custom slots per epoch (By default 500 slots/epoch)"}),"\n",(0,n.jsxs)(s.p,{children:[(0,n.jsx)(s.strong,{children:"For example:"})," Create and start a devnet with 30 slots per epoch"]}),"\n",(0,n.jsx)(s.pre,{"data-language":"text","data-theme":"default",children:(0,n.jsxs)(s.code,{"data-language":"text","data-theme":"default",children:[(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"yaci-cli> create-node -o -e 30 --start"})}),"\n",(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"}})})]})}),"\n",(0,n.jsx)(s.p,{children:'Now, you should be in the "devnet" context. To start the devnet, use the "start" command.'}),"\n",(0,n.jsx)(s.pre,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"devnet:default>start"})})})}),"\n",(0,n.jsxs)(s.p,{children:[(0,n.jsx)(s.strong,{children:"Note"})," Now, with Yaci Viewer, you can conveniently check the devnet's data right from the browser. Simply open the following URL\nin your browser to access the Yaci Viewer."]}),"\n",(0,n.jsx)(s.p,{children:(0,n.jsx)(s.a,{href:"http://localhost:5173",children:"http://localhost:5173"})}),"\n",(0,n.jsx)(s.h3,{id:"to-reset-clusters-data",children:"To reset cluster's data"}),"\n",(0,n.jsx)(s.p,{children:'If your devnet gets stuck or you simply want to reset the data and restart with the same configuration, simply use the command "reset".\nIt will restore your devnet to its initial state, allowing you to continue your development seamlessly.'}),"\n",(0,n.jsx)(s.pre,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"devnet:default>reset"})})})}),"\n",(0,n.jsx)(s.h3,{id:"to-stop",children:"To stop"}),"\n",(0,n.jsx)(s.pre,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"text","data-theme":"default",children:(0,n.jsx)(s.span,{className:"line",children:(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"devnet:default>stop"})})})}),"\n",(0,n.jsx)(s.h3,{id:"to-fund-a-new-address",children:"To fund a new address"}),"\n",(0,n.jsx)(s.p,{children:'Easily fund your test account with ADA using the "topup" command.'}),"\n",(0,n.jsx)(s.pre,{"data-language":"shell","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"shell","data-theme":"default",children:(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"devnet:default>"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"topup"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-keyword)"},children:"<"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"addres"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"s"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-keyword)"},children:">"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-keyword)"},children:"<"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"ada"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"valu"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"e"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-keyword)"},children:">"})]})})}),"\n",(0,n.jsx)(s.h3,{id:"to-check-utxos-at-an-address",children:"To check utxos at an address"}),"\n",(0,n.jsx)(s.pre,{"data-language":"shell","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"shell","data-theme":"default",children:(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"devnet:default>"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"utxos"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-keyword)"},children:"<"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"addres"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:"s"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-keyword)"},children:">"})]})})}),"\n",(0,n.jsx)(s.h3,{id:"to-check-devnet-and-url-info",children:"To check devnet and url info"}),"\n",(0,n.jsx)(s.pre,{"data-language":"shell","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"shell","data-theme":"default",children:(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"devnet:default>"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"info"})]})})}),"\n",(0,n.jsx)(s.h3,{id:"default-pool-id",children:"Default Pool Id"}),"\n",(0,n.jsx)(s.p,{children:"If you are testing pool delegation and reward withdrawal transaction in your application, you can use the following pool id\nwhich is already registered in the devnet."}),"\n",(0,n.jsx)(s.p,{children:(0,n.jsx)(s.strong,{children:"pool1wvqhvyrgwch4jq9aa84hc8q4kzvyq2z3xr6mpafkqmx9wce39zy"})}),"\n",(0,n.jsx)(s.h2,{id:"query-devnets-cardano-node-using-cardano-cli",children:"Query Devnet's Cardano Node using cardano-cli"}),"\n",(0,n.jsx)(s.p,{children:"DevKit script has a wrapper script to query the Cardano node running in the devnet. You can use this script to query the Cardano node like\nyou usually do with cardano-cli command line tool. You don't need to install cardano-cli in your local machine or use protocol magic number in the command."}),"\n",(0,n.jsx)(s.p,{children:(0,n.jsx)(s.strong,{children:"For example:"})}),"\n",(0,n.jsxs)(s.p,{children:["To query protocol parameters, you can use ",(0,n.jsx)(s.code,{children:"cli"})," option with devkit script."]}),"\n",(0,n.jsx)(s.pre,{"data-language":"shell","data-theme":"default",children:(0,n.jsx)(s.code,{"data-language":"shell","data-theme":"default",children:(0,n.jsxs)(s.span,{className:"line",children:[(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-function)"},children:"./devkit.sh"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"cli"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"query"}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(s.span,{style:{color:"var(--shiki-token-string)"},children:"protocol-parameters"})]})})})]})}let c={MDXContent:function(){let e=arguments.length>0&&void 0!==arguments[0]?arguments[0]:{},{wrapper:s}=Object.assign({},(0,r.a)(),e.components);return s?(0,n.jsx)(s,{...e,children:(0,n.jsx)(_createMdxContent,{...e})}):_createMdxContent(e)},pageOpts:{filePath:"pages/how_to_use.mdx",route:"/how_to_use",timestamp:1725879686e3,pageMap:[{kind:"Meta",data:{index:"Introduction",yaci_cli_distribution:"Yaci CLI (Non Docker Dist)",how_to_use:"How to use",about:{title:"About",type:"page"},services:"Services",commands:"Yaci CLI Commands","node-configuration":"Node Configuration","multi-node-setup":"Multi-node setup",tutorials:"Tutorials"}},{kind:"MdxPage",name:"about",route:"/about"},{kind:"MdxPage",name:"commands",route:"/commands"},{kind:"MdxPage",name:"how_to_use",route:"/how_to_use"},{kind:"MdxPage",name:"index",route:"/"},{kind:"MdxPage",name:"multi-node-setup",route:"/multi-node-setup"},{kind:"MdxPage",name:"node-configuration",route:"/node-configuration"},{kind:"MdxPage",name:"services",route:"/services"},{kind:"Folder",name:"tutorials",route:"/tutorials",children:[{kind:"Meta",data:{meshjs:"MeshJS",ccl:"Cardano Client Lib (Java)","lucid-evolution":"Lucid Evolution"}},{kind:"Folder",name:"ccl",route:"/tutorials/ccl",children:[{kind:"MdxPage",name:"overview",route:"/tutorials/ccl/overview"},{kind:"Meta",data:{overview:"Overview"}}]},{kind:"Folder",name:"lucid-evolution",route:"/tutorials/lucid-evolution",children:[{kind:"MdxPage",name:"overview",route:"/tutorials/lucid-evolution/overview"},{kind:"Meta",data:{overview:"Overview"}}]},{kind:"Folder",name:"meshjs",route:"/tutorials/meshjs",children:[{kind:"MdxPage",name:"overview",route:"/tutorials/meshjs/overview"},{kind:"Meta",data:{overview:"Overview"}}]}]},{kind:"MdxPage",name:"yaci_cli_distribution",route:"/yaci_cli_distribution"}],flexsearch:{codeblocks:!0},title:"How to Use",headings:l},pageNextRoute:"/how_to_use",nextraLayout:o.ZP,themeConfig:i.Z};s.default=(0,a.j)(c)},8426:function(e,s,t){"use strict";var n=t(5893);t(7294);let a={logo:(0,n.jsx)("span",{children:(0,n.jsx)("b",{children:"Yaci DevKit"})}),project:{link:"https://github.com/bloxbean/yaci-devkit"},chat:{link:"https://discord.gg/JtQ54MSw6p"},docsRepositoryBase:"https://github.com/bloxbean/yaci-devkit/tree/develop/docs",footer:{text:"\xa9 2024 BloxBean project"},useNextSeoProps:()=>({titleTemplate:"%s – Yaci DevKit"}),head:(0,n.jsxs)(n.Fragment,{children:[(0,n.jsx)("meta",{property:"description",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"}),(0,n.jsx)("meta",{property:"og:title",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet"}),(0,n.jsx)("meta",{property:"og:description",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"})]})};s.Z=a},5789:function(){}},function(e){e.O(0,[774,543,888,179],function(){return e(e.s=8016)}),_N_E=e.O()}]);