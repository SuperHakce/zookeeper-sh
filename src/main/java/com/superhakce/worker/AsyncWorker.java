package com.superhakce.worker;

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
 * @Description: 异步回调Worker
 * @Date: Create in 2019/1/17 15:47
 */
@Slf4j
public class AsyncWorker implements Watcher{

    // Zookeeper 对象
    private ZooKeeper zk;

    // Zookeeper 服务地址
    private String hostPort;

    // 客户端唯一ID
    private String serverId = Integer.toHexString(new Random().nextInt());

    // 节点状态
    private String status;

    // 节点名称
    private String name;

    public String getServerId() {
        return serverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        updateStatus(status);
    }

    public String getName() {
        return name;
    }

    // 异步 Watcher 构造函数
    public AsyncWorker(String hostPort){
        this.hostPort = hostPort;
    }

    @Override
    public void process(WatchedEvent event){
        log.info(event.toString() + "" + hostPort);
    }

    // 启动连接
    protected void startZK(){
        try {
            zk = new ZooKeeper(hostPort, 15000, this);
        }catch (IOException e){
            log.error("Worker ERROR:", e);
            e.printStackTrace();
        }
    }

    // 注册从节点
    private void register(){
        zk.create("/workers/worker-" + serverId,
                "Idle".getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL,
                createWorkerCallBack,
                null);
        name = "worker-" + serverId;
    }

    // 从节点创建异步回调
    AsyncCallback.StringCallback createWorkerCallBack = new AsyncCallback.StringCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, String name) {
            log.info("createWorkerCallBack收到异步回调，rc:{}, path:{}, ctx:{}, name:{}", rc, path, ctx, name);
            switch (KeeperException.Code.get(rc)){
                case CONNECTIONLOSS:
                    register();
                    break;
                case OK:
                    log.info("Registered successfully, serverId:{}", serverId);
                    break;
                case NODEEXISTS:
                    log.warn("Already registered，serverId:{}", serverId);
                    break;
                default:
                    log.error("Something went wrong, e:{}, path:{}", KeeperException.create(KeeperException.Code.get(rc)
                    ), path);
            }
        }
    };

    // 关闭连接
    protected void closeZK(){
        try{
            this.zk.close();
        }catch (InterruptedException e){
            log.error("zookeeper close have error:", e);
        }
    }


    // 更新状异步态回调
    AsyncCallback.StatCallback statusUpdateCallBack = new AsyncCallback.StatCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, Stat stat) {
            log.info("statusUpdateCallBack收到异步回调，rc:{}, path:{}, ctx:{}, stat:{}", rc, path, ctx, stat);
            switch (KeeperException.Code.get(rc)){
                case CONNECTIONLOSS:
                    updateStatus(ctx.toString());
                    return;
            }
        }
    };

    // 竞态条件更新状态
    synchronized private void updateStatus(String status){
        if(status == this.status){
            zk.setData("/workers/" + name,
                    status.getBytes(),
                    -1,
                    statusUpdateCallBack,
                    status);
        }
    }

    public static void main(String[] args) throws InterruptedException{
        BasicConfigurator.configure();
        AsyncWorker asyncWorker = new AsyncWorker("58.87.111.245:2181");
        asyncWorker.startZK();
        asyncWorker.register();
        asyncWorker.setStatus("heqingjiang");
        Thread.sleep(6000000L);
    }

}
