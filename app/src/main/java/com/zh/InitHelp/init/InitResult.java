package com.zh.InitHelp.init;

public enum InitResult {
    SUCCESS_NEXT            //成功并且执行下一个初始化
    , FAIL_NEXT             //失败并且执行下一个初始化
    , FAIL_DISCONTINUE      //失败并且中断剩下的初始化工作
    , SUCCESS_DISCONTINUE   //成功并且中断剩下的初始化工作
}
