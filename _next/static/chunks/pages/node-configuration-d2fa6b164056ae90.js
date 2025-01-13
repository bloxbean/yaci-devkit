(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[487],{3369:function(e,t,i){(window.__NEXT_P=window.__NEXT_P||[]).push(["/node-configuration",function(){return i(9055)}])},9055:function(e,t,i){"use strict";i.r(t),i.d(t,{__toc:function(){return c}});var n=i(5893),o=i(2673),r=i(7913),a=i(8426);i(9128);var s=i(2643),d=i(9013);let c=[{depth:2,value:"Node Configuration",id:"node-configuration"},{depth:2,value:"Example of few key configurations:",id:"example-of-few-key-configurations"},{depth:3,value:"securityParam",id:"securityparam"},{depth:3,value:"peerSharing",id:"peersharing"},{depth:3,value:"slotsPerKESPeriod",id:"slotsperkesperiod"},{depth:3,value:"maxKESEvolutions",id:"maxkesevolutions"}];function _createMdxContent(e){let t=Object.assign({h2:"h2",p:"p",code:"code",strong:"strong",h3:"h3",pre:"pre",span:"span",a:"a"},(0,s.a)(),e.components);return(0,n.jsxs)(n.Fragment,{children:[(0,n.jsx)(t.h2,{id:"node-configuration",children:"Node Configuration"}),"\n",(0,n.jsxs)(t.p,{children:["Apart from a few options like ",(0,n.jsx)(t.code,{children:"block time"}),", ",(0,n.jsx)(t.code,{children:"epoch length"}),", and ",(0,n.jsx)(t.code,{children:"slot length"})," which are provided through Yaci CLI,\nother devnet node-related configurations can be configured through the ",(0,n.jsx)(t.code,{children:"node.properties"})," file. The configurations defined in the\n",(0,n.jsx)(t.code,{children:"node.properties"})," file will be used during devnet node creation."]}),"\n",(0,n.jsxs)(t.p,{children:["For ",(0,n.jsx)(t.strong,{children:"zip"})," based installation, this file is located under installation directory ",(0,n.jsx)(t.code,{children:"config/node.properties"}),"."]}),"\n",(0,n.jsxs)(t.p,{children:["For ",(0,n.jsx)(t.strong,{children:"curl"})," based installation, this file is located under ",(0,n.jsx)(t.code,{children:"$HOME/.yaci-devkit/config/node.properties"}),"."]}),"\n",(0,n.jsxs)(t.p,{children:["Please refer to the ",(0,n.jsx)(t.code,{children:"config/node.properties"})," file for all the node specific configurations."]}),"\n",(0,n.jsx)(t.h2,{id:"example-of-few-key-configurations",children:"Example of few key configurations:"}),"\n",(0,n.jsx)(t.h3,{id:"securityparam",children:"securityParam"}),"\n",(0,n.jsx)(t.p,{children:"To set the security parameter for the node. Default value is 300. (300 blocks)"}),"\n",(0,n.jsxs)(t.p,{children:["To change the configuration, add to the ",(0,n.jsx)(t.code,{children:"node.properties"})," file"]}),"\n",(0,n.jsx)(t.pre,{"data-language":"shell","data-theme":"default",children:(0,n.jsx)(t.code,{"data-language":"shell","data-theme":"default",children:(0,n.jsxs)(t.span,{className:"line",children:[(0,n.jsx)(t.span,{style:{color:"var(--shiki-color-text)"},children:"securityParam"}),(0,n.jsx)(t.span,{style:{color:"var(--shiki-token-keyword)"},children:"=<"}),(0,n.jsx)(t.span,{style:{color:"var(--shiki-token-string)"},children:"int"}),(0,n.jsx)(t.span,{style:{color:"var(--shiki-color-text)"},children:" "}),(0,n.jsx)(t.span,{style:{color:"var(--shiki-token-function)"},children:"value>"})]})})}),"\n",(0,n.jsxs)(t.p,{children:[(0,n.jsx)(t.strong,{children:"Note:"})," If you want to stop your node for some time and restart it later without a reset, allowing it to continue from where\nit left off and use existing data, you may want to set ",(0,n.jsx)(t.code,{children:"securityParam"})," to a higher value.\nOtherwise, the node may get stuck and you need to reset the node."]}),"\n",(0,n.jsxs)(t.p,{children:["Let's say you want to stop your node and start the node again next day to continue from where it left off,\nthen you can set ",(0,n.jsx)(t.code,{children:"securityParam"})," to a higher values like ",(0,n.jsx)(t.code,{children:"86400"})," for 1 sec block time."]}),"\n",(0,n.jsx)(d.UW,{children:(0,n.jsxs)(t.p,{children:["Currently, for Devnet work, the security parameter is set to a reasonable value to improve the developer experience.\nFor a real/multi-node network setup, please refer to this known issue/workaround: ",(0,n.jsx)(t.a,{href:"https://github.com/bloxbean/yaci-devkit/issues/65#issuecomment-2318155838",children:"https://github.com/bloxbean/yaci-devkit/issues/65#issuecomment-2318155838"})]})}),"\n",(0,n.jsx)(t.h3,{id:"peersharing",children:"peerSharing"}),"\n",(0,n.jsxs)(t.p,{children:["To enable or disable peer sharing. Default value is ",(0,n.jsx)(t.code,{children:"true"}),"."]}),"\n",(0,n.jsx)(t.h3,{id:"slotsperkesperiod",children:"slotsPerKESPeriod"}),"\n",(0,n.jsxs)(t.p,{children:["To set the number of slots per KES period. Default value is ",(0,n.jsx)(t.code,{children:"129600"}),"."]}),"\n",(0,n.jsx)(t.h3,{id:"maxkesevolutions",children:"maxKESEvolutions"}),"\n",(0,n.jsxs)(t.p,{children:["To set the maximum KES evolutions. Default value is ",(0,n.jsx)(t.code,{children:"60"}),"."]}),"\n",(0,n.jsxs)(t.p,{children:[(0,n.jsx)(t.strong,{children:"Note:"})," More configurations can be found in ",(0,n.jsx)(t.code,{children:"node.properties"})," file."]})]})}let l={MDXContent:function(){let e=arguments.length>0&&void 0!==arguments[0]?arguments[0]:{},{wrapper:t}=Object.assign({},(0,s.a)(),e.components);return t?(0,n.jsx)(t,{...e,children:(0,n.jsx)(_createMdxContent,{...e})}):_createMdxContent(e)},pageOpts:{filePath:"pages/node-configuration.mdx",route:"/node-configuration",timestamp:1736749167e3,pageMap:[{kind:"Meta",data:{index:"Introduction",docker:"Docker Distribution",yaci_cli_distribution:"Zip (Non-Docker) Distribution",yaci_cli_npm_distr:"NPM Distribution",how_to_use:"How to use",about:{title:"About",type:"page"},services:"Services",commands:"Yaci CLI Commands","node-configuration":"Node Configuration","multi-node-setup":"Multi-node setup",tutorials:"Tutorials"}},{kind:"MdxPage",name:"about",route:"/about"},{kind:"MdxPage",name:"commands",route:"/commands"},{kind:"MdxPage",name:"docker",route:"/docker"},{kind:"MdxPage",name:"how_to_use",route:"/how_to_use"},{kind:"MdxPage",name:"index",route:"/"},{kind:"MdxPage",name:"multi-node-setup",route:"/multi-node-setup"},{kind:"MdxPage",name:"node-configuration",route:"/node-configuration"},{kind:"MdxPage",name:"services",route:"/services"},{kind:"Folder",name:"tutorials",route:"/tutorials",children:[{kind:"Meta",data:{meshjs:"MeshJS",ccl:"Cardano Client Lib (Java)","lucid-evolution":"Lucid Evolution"}},{kind:"Folder",name:"ccl",route:"/tutorials/ccl",children:[{kind:"MdxPage",name:"overview",route:"/tutorials/ccl/overview"},{kind:"Meta",data:{overview:"Overview"}}]},{kind:"Folder",name:"lucid-evolution",route:"/tutorials/lucid-evolution",children:[{kind:"MdxPage",name:"overview",route:"/tutorials/lucid-evolution/overview"},{kind:"Meta",data:{overview:"Overview"}}]},{kind:"Folder",name:"meshjs",route:"/tutorials/meshjs",children:[{kind:"MdxPage",name:"adminstrative-functions",route:"/tutorials/meshjs/adminstrative-functions"},{kind:"MdxPage",name:"overview",route:"/tutorials/meshjs/overview"},{kind:"Meta",data:{"adminstrative-functions":"Adminstrative Functions",overview:"Overview"}}]}]},{kind:"MdxPage",name:"yaci_cli_distribution",route:"/yaci_cli_distribution"},{kind:"MdxPage",name:"yaci_cli_npm_distr",route:"/yaci_cli_npm_distr"}],flexsearch:{codeblocks:!0},title:"Node Configuration",headings:c},pageNextRoute:"/node-configuration",nextraLayout:r.ZP,themeConfig:a.Z};t.default=(0,o.j)(l)},8426:function(e,t,i){"use strict";var n=i(5893);i(7294);let o={logo:(0,n.jsx)("span",{children:(0,n.jsx)("b",{children:"Yaci DevKit"})}),project:{link:"https://github.com/bloxbean/yaci-devkit"},chat:{link:"https://discord.gg/JtQ54MSw6p"},docsRepositoryBase:"https://github.com/bloxbean/yaci-devkit/tree/develop/docs",footer:{text:"\xa9 2024 BloxBean project"},useNextSeoProps:()=>({titleTemplate:"%s – Yaci DevKit"}),head:(0,n.jsxs)(n.Fragment,{children:[(0,n.jsx)("meta",{property:"description",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"}),(0,n.jsx)("meta",{property:"og:title",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet"}),(0,n.jsx)("meta",{property:"og:description",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"})]})};t.Z=o},5789:function(){}},function(e){e.O(0,[774,543,888,179],function(){return e(e.s=3369)}),_N_E=e.O()}]);