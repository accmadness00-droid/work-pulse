import{bt as c,a2 as o}from"./index-BPo6mI2k.js";const r=new o("antFadeIn",{"0%":{opacity:0},"100%":{opacity:1}}),s=new o("antFadeOut",{"0%":{opacity:1},"100%":{opacity:0}}),p=(a,i=!1)=>{const{antCls:e}=a,n=`${e}-fade`,t=i?"&":"";return[c(n,r,s,a.motionDurationMid,i),{[`
        ${t}${n}-enter,
        ${t}${n}-appear
      `]:{opacity:0,animationTimingFunction:"linear"},[`${t}${n}-leave`]:{animationTimingFunction:"linear"}}]};export{p as i};
