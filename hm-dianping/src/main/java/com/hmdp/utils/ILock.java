package com.hmdp.utils;

public interface ILock {
    /**
    * @Description: 尝试获取锁
    * @Param: [timeoutSec]
    * @return: boolean
    * @Author: fmy
    * @date:
    */
    boolean tryLock(long timeoutSec);
    
    /** 
    * @Description: 释放锁
    * @Param: []
    * @return: void
    * @Author: fmy
    * @date:
    */
    
    void unLock();
    
}
