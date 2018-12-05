package com.superhakce.watcher;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.BasicConfigurator;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

/**
 * @Author: heqingjiang
 * @Maintenance: author
 * @Description: Watcher
 * @Date: Create in 2018/12/5 15:44
 */
@Slf4j
public class Master implements Watcher{
    ZooKeeper zk;
    String hostPort;
    Master(String hostPort){
        this.hostPort = hostPort;
    }
    void startZK(){
        try {
            zk = new ZooKeeper(hostPort, 15000, this);
        }catch (IOException e){
            log.error("Master ERROR:{}", e);
            e.printStackTrace();
        }
    }
    @Override
    public void process(WatchedEvent watchedEvent){
        log.info("Master.process");
    }
    public static void main(String[] args) throws InterruptedException{
        BasicConfigurator.configure();
        Master master = new Master("*.*.*.*:2181");
        master.startZK();
        Thread.sleep(600000L);
    }
}
