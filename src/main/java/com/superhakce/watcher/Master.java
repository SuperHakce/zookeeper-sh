package com.superhakce.watcher;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.BasicConfigurator;
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
 * @Description: Watcher
 * @Date: Create in 2018/12/5 15:44
 */
@Slf4j
public class Master implements Watcher{



    private ZooKeeper zk;
    private String hostPort;
    private boolean isLeader = false;
    private final static String MASTER_NODE = "/master";

    public boolean isLeader() {
        return isLeader;
    }

    public Master(String hostPort){
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
        while(true){
            try{
                String ZK_CREATE = zk.create(MASTER_NODE,
                        serverId.getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL);
            }catch (KeeperException.NodeExistsException e){
                log.error("创建/master失败NodeExistsException:", e);
                isLeader = false;
                break;
            } catch (KeeperException.ConnectionLossException e){
                log.error("创建/master失败ConnectionLossException:", e);
            } catch (KeeperException e){
                log.error("创建/master失败KeeperException:", e);
            }
            if(checkMaster()) break;
        }
    }

    private boolean checkMaster() throws InterruptedException{
        while (true){
            try{
                byte data[] = zk.getData(MASTER_NODE, false, new Stat());
                isLeader = new String(data).equals(serverId);
                return true;
            }catch (KeeperException.NoNodeException e){
                log.error("查询是否为主节点NoNodeException:", e);
                return false;
            }catch (KeeperException.ConnectionLossException e){
                log.error("查询是否为主节点ConnectionLossException:", e);
            }catch (KeeperException e){
                log.error("查询是否为主节点KeeperException:{}", e);
                return true;
            }
        }
    }


    public static void main(String[] args) throws InterruptedException{
        BasicConfigurator.configure();
        Master master = new Master("58.87.111.245:2181");
        master.startZK();
        master.runForMaster();
        if(master.isLeader()){
            log.info("I am the Leader");
            Thread.sleep(6000000L);
        }else{
            log.info("SomeOne is the Leader");
        }
        master.closeZK();
    }
}
