(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[54],{7373:function(e,i,n){(window.__NEXT_P=window.__NEXT_P||[]).push(["/services",function(){return n(8642)}])},8642:function(e,i,n){"use strict";n.r(i),n.d(i,{__toc:function(){return l}});var t=n(5893),o=n(2673),r=n(3393),s=n(8426);n(9128);var a=n(2643);let l=[{depth:2,value:"Services",id:"services"},{depth:2,value:"Configuration",id:"configuration"},{depth:3,value:"Location of env file",id:"location-of-env-file"},{depth:3,value:"Configuration options",id:"configuration-options"}];function _createMdxContent(e){let i=Object.assign({h2:"h2",p:"p",strong:"strong",ul:"ul",li:"li",code:"code",h3:"h3"},(0,a.a)(),e.components);return(0,t.jsxs)(t.Fragment,{children:[(0,t.jsx)(i.h2,{id:"services",children:"Services"}),"\n",(0,t.jsxs)(i.p,{children:[(0,t.jsx)(i.strong,{children:"Yaci DevKit"})," has the following optional services. You may want to enable or disable them based on your application's requirements or the client library you are using."]}),"\n",(0,t.jsxs)(i.ul,{children:["\n",(0,t.jsxs)(i.li,{children:[(0,t.jsx)(i.strong,{children:"Yaci Store :"})," An indexer that provides Blockfrost-compatible API. It's useful if you're using a client library that relies on the Blockfrost API."]}),"\n",(0,t.jsxs)(i.li,{children:[(0,t.jsx)(i.strong,{children:"Ogmios :"})," A lightweight bridge interface for cardano-node. It offers a WebSockets API that enables local clients to speak\nOuroboros' mini-protocols via JSON/RPC."]}),"\n",(0,t.jsxs)(i.li,{children:[(0,t.jsx)(i.strong,{children:"Kupo :"})," Kupo is fast, lightweight and configurable chain-index for the Cardano blockchain."]}),"\n"]}),"\n",(0,t.jsxs)(i.p,{children:["By default, ",(0,t.jsx)(i.strong,{children:"Yaci Store"})," and ",(0,t.jsx)(i.strong,{children:"Ogmios"})," are enabled. While Yaci Store provides a Blockfrost-compatible API, it relies on Ogmios\nto support the transaction evaluation endpoint (/api/v1/utils/txs/evaluate). This is useful if you're using a client library\nthat performs online script cost estimation via Blockfrost's transaction evaluation endpoint."]}),"\n",(0,t.jsx)(i.p,{children:'If you enable Kupo, a default wildcard "catch all" matching pattern is pre-configured.'}),"\n",(0,t.jsx)(i.h2,{id:"configuration",children:"Configuration"}),"\n",(0,t.jsxs)(i.p,{children:["Based on your requirements, you should enable or disable services in the ",(0,t.jsx)(i.code,{children:"env"})," file to ",(0,t.jsx)(i.strong,{children:"minimize"})," the ",(0,t.jsx)(i.strong,{children:"runtime memory"})," footprint of DevKit.\nThough, you can also enable or disable services using specific commands in yaci-cli, but it's recommended to use the ",(0,t.jsx)(i.code,{children:"env"})," file for a persistent configuration."]}),"\n",(0,t.jsx)(i.h3,{id:"location-of-env-file",children:"Location of env file"}),"\n",(0,t.jsxs)(i.p,{children:["For zip based installation, the env file is located under installation directory ",(0,t.jsx)(i.code,{children:"config/env"}),"."]}),"\n",(0,t.jsxs)(i.p,{children:["For curl based installation, the env file is located under ",(0,t.jsx)(i.code,{children:"$HOME/.yaci-devkit/config/env"}),"."]}),"\n",(0,t.jsx)(i.h3,{id:"configuration-options",children:"Configuration options"}),"\n",(0,t.jsxs)(i.ul,{children:["\n",(0,t.jsxs)(i.li,{children:[(0,t.jsx)(i.strong,{children:"yaci_store_enabled"})," : Enable or disable Yaci Store service. Default is ",(0,t.jsx)(i.code,{children:"true"}),"."]}),"\n",(0,t.jsxs)(i.li,{children:[(0,t.jsx)(i.strong,{children:"ogmios_enabled"})," : Enable or disable Ogmios service. Default is ",(0,t.jsx)(i.code,{children:"true"}),"."]}),"\n",(0,t.jsxs)(i.li,{children:[(0,t.jsx)(i.strong,{children:"kupo_enabled"})," : Enable or disable Kupo service. Default is ",(0,t.jsx)(i.code,{children:"false"}),"."]}),"\n"]})]})}let c={MDXContent:function(){let e=arguments.length>0&&void 0!==arguments[0]?arguments[0]:{},{wrapper:i}=Object.assign({},(0,a.a)(),e.components);return i?(0,t.jsx)(i,{...e,children:(0,t.jsx)(_createMdxContent,{...e})}):_createMdxContent(e)},pageOpts:{filePath:"pages/services.mdx",route:"/services",timestamp:1722424192e3,pageMap:[{kind:"Meta",data:{index:"Introduction",how_to_use:"How to use",about:{title:"About",type:"page"},services:"Services",commands:"Yaci CLI Commands","node-configuration":"Node Configuration","multi-node-setup":"Multi-node setup",tutorials:"Tutorials"}},{kind:"MdxPage",name:"about",route:"/about"},{kind:"MdxPage",name:"commands",route:"/commands"},{kind:"MdxPage",name:"how_to_use",route:"/how_to_use"},{kind:"MdxPage",name:"index",route:"/"},{kind:"MdxPage",name:"multi-node-setup",route:"/multi-node-setup"},{kind:"MdxPage",name:"node-configuration",route:"/node-configuration"},{kind:"MdxPage",name:"services",route:"/services"},{kind:"Folder",name:"tutorials",route:"/tutorials",children:[{kind:"Meta",data:{meshjs:"MeshJS",ccl:"Cardano Client Lib (Java)","lucid-evolution":"Lucid Evolution"}},{kind:"Folder",name:"ccl",route:"/tutorials/ccl",children:[{kind:"MdxPage",name:"overview",route:"/tutorials/ccl/overview"},{kind:"Meta",data:{overview:"Overview"}}]},{kind:"Folder",name:"lucid-evolution",route:"/tutorials/lucid-evolution",children:[{kind:"MdxPage",name:"overview",route:"/tutorials/lucid-evolution/overview"},{kind:"Meta",data:{overview:"Overview"}}]},{kind:"Folder",name:"meshjs",route:"/tutorials/meshjs",children:[{kind:"MdxPage",name:"overview",route:"/tutorials/meshjs/overview"},{kind:"Meta",data:{overview:"Overview"}}]}]}],flexsearch:{codeblocks:!0},title:"Services",headings:l},pageNextRoute:"/services",nextraLayout:r.ZP,themeConfig:s.Z};i.default=(0,o.j)(c)},8426:function(e,i,n){"use strict";var t=n(5893);n(7294);let o={logo:(0,t.jsx)("span",{children:(0,t.jsx)("b",{children:"Yaci DevKit"})}),project:{link:"https://github.com/bloxbean/yaci-devkit"},chat:{link:"https://discord.gg/JtQ54MSw6p"},docsRepositoryBase:"https://github.com/bloxbean/yaci-devkit/tree/develop/docs",footer:{text:"\xa9 2024 BloxBean project"},useNextSeoProps:()=>({titleTemplate:"%s – Yaci DevKit"}),head:(0,t.jsxs)(t.Fragment,{children:[(0,t.jsx)("meta",{property:"description",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"}),(0,t.jsx)("meta",{property:"og:title",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet"}),(0,t.jsx)("meta",{property:"og:description",content:"Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"})]})};i.Z=o},5789:function(){}},function(e){e.O(0,[774,890,888,179],function(){return e(e.s=7373)}),_N_E=e.O()}]);