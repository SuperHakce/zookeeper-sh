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

    // 主节点名称
    private final static String MASTER_NODE = "/master";

    // Zookeeper 对象
    private ZooKeeper zk;

    // Zookeeper 服务地址
    private String hostPort;

    // 该客户端是否为主节点
    private boolean isLeader = false;
    public boolean isLeader() {
        return isLeader;
    }

    // 客户端唯一ID
    private String serverId = Integer.toHexString(new Random().nextInt());
    public String getServerId() {
        return serverId;
    }

    // 主节点创建异步回调
    private AsyncCallback.StringCallback masterCreateCallBack = new AsyncCallback.StringCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, String name) {
            log.info("masterCreateCallBack收到异步回调，rc:{}, path:{}, ctx:{}, name:{}", rc, path, ctx, name);
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

    // 主节点检测异步回调
    private AsyncCallback.DataCallback masterCheckCallBack = new AsyncCallback.DataCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            log.info("masterCheckCallBack收到异步回调，rc:{}, path:{}, ctx:{}, data:{}, stat:{}", rc, path, ctx, data,
                    stat);
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

    // 异步 Watcher 构造函数
    public AsyncMaster(String hostPort){
        this.hostPort = hostPort;
    }


    @Override
    public void process(WatchedEvent watchedEvent){
        log.info("Master.process");
    }

    // 启动连接
    protected void startZK(){
        try {
            zk = new ZooKeeper(hostPort, 15000, this);
        }catch (IOException e){
            log.error("Master ERROR:", e);
            e.printStackTrace();
        }
    }


    // 关闭连接
    protected void closeZK(){
        try{
            this.zk.close();
        }catch (InterruptedException e){
            log.error("zookeeper close have error:", e);
        }
    }

    // 创建主节点，成为主节点
    protected void runForMaster() throws InterruptedException{
        zk.create(MASTER_NODE,
                serverId.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL,
                masterCreateCallBack,
                null);
        bootstrap();
    }

    // 检测主节点
    private void checkMaster() throws InterruptedException{
        zk.getData(MASTER_NODE, false, masterCheckCallBack, null);
    }

    // 创建主从模式需要的根目录
    public void bootstrap(){
        createParent("/workers", new byte[0]);
        createParent("/assign", new byte[0]);
        createParent("/tasks", new byte[0]);
        createParent("/status", new byte[0]);
    }

    // 创建目录
    void createParent(String path, byte[] data){
        zk.create(path, data,
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT,
                createParentCallBack,
                data);
    }

    // 创建目录结构异步回调
    AsyncCallback.StringCallback createParentCallBack = new AsyncCallback.StringCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, String name) {
            log.info("createParentCallBack收到异步回调，rc:{}, path:{}, ctx:{}, name:{}", rc, path, ctx, name);
            switch (KeeperException.Code.get(rc)){
                case CONNECTIONLOSS:
                    createParent(path, (byte[]) ctx);
                    break;
                case OK:
                    log.info("Parent created, path:{}", path);
                    break;
                case NODEEXISTS:
                    log.warn("Parent already registered, path:{}", path);
                    break;
                default:
                    log.error("Something went wrong, e:{}, path:{}", KeeperException.create(KeeperException.Code.get(rc)
                    ), path);
            }
        }
    };

    public static void main(String[] args) throws InterruptedException{
        BasicConfigurator.configure();
        AsyncMaster asyncMaster = new AsyncMaster("58.87.111.245:2181");
        asyncMaster.startZK();
        asyncMaster.runForMaster();
        Thread.sleep(6000000L);
        asyncMaster.closeZK();
    }
}
