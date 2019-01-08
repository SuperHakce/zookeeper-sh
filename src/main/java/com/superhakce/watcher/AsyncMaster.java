package com.superhakce.watcher;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.BasicConfigurator;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Random;

/**
 * @Author: heqingjiang
 * @Maintenance: author
 * @Description: 异步回调Master
 * @Date: Create in 2019/1/8 16:36
 */
@Slf4j
public class AsyncMaster implements Watcher {


    private ZooKeeper zk;
    private String hostPort;
    private boolean isLeader = false;
    private final static String MASTER_NODE = "/master";
    private AsyncCallback.StringCallback masterCreateCallBack = new AsyncCallback.StringCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, String name) {
            log.info("收到异步回调，rc:{}, path:{}, ctx:{}, name:{}", rc, path, ctx, name);
            switch (KeeperException.Code.get(rc)){
                case CONNECTIONLOSS:
                    try{
                        checkMaster();
                    }catch (InterruptedException e){
                        log.error("异步回调出错InterruptedException:", e);
                    }
                    return;
                case OK:
                    isLeader = true;
                    break;
                default:
                    isLeader = false;
            }
            if(isLeader){
                log.info("I am the Leader");
            }else {
                log.info("SomeOne is the Leader");
            }
        }
    };

    private AsyncCallback.DataCallback masterCheckCallBack = new AsyncCallback.DataCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            log.info("收到异步回调，rc:{}, path:{}, ctx:{}, data:{}, stat:{}", rc, path, ctx, data, stat);
            switch (KeeperException.Code.get(rc)){
                case CONNECTIONLOSS:
                    try{
                        checkMaster();
                    }catch (InterruptedException e){
                        log.error("异步回调出错InterruptedException:", e);
                    }
                    return;
                case NONODE:
                    try{
                        runForMaster();
                    }catch (InterruptedException e){
                        log.error("异步回调出错InterruptedException:", e);
                    }
                    return;
            }
        }
    };

    public boolean isLeader() {
        return isLeader;
    }

    public AsyncMaster(String hostPort){
        this.hostPort = hostPort;
    }


    @Override
    public void process(WatchedEvent watchedEvent){
        log.info("Master.process");
    }


    protected void startZK(){
        try {
            zk = new ZooKeeper(hostPort, 15000, this);
        }catch (IOException e){
            log.error("Master ERROR:", e);
            e.printStackTrace();
        }
    }


    protected void closeZK(){
        try{
            this.zk.close();
        }catch (InterruptedException e){
            log.error("zookeeper close have error:", e);
        }
    }


    String serverId = Integer.toHexString(new Random().nextInt());

    protected void runForMaster() throws InterruptedException{
        zk.create(MASTER_NODE,
                serverId.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL,
                masterCreateCallBack,
                null);
    }

    private void checkMaster() throws InterruptedException{
        zk.getData(MASTER_NODE, false, masterCheckCallBack, null);
    }

    public static void main(String[] args) throws InterruptedException{
        BasicConfigurator.configure();
        AsyncMaster asyncMaster = new AsyncMaster("58.87.111.245:2181");
        asyncMaster.startZK();
        asyncMaster.runForMaster();
        Thread.sleep(6000000L);
        asyncMaster.closeZK();
    }
}
