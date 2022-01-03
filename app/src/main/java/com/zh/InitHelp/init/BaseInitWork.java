package com.zh.InitHelp.init;

public class BaseInitWork {
    private String key = null;
    private InitStatus initStatus = InitStatus.NOT_RUNNING_YET;
    private boolean isMainThread = true;
    private boolean isPassiveInit = false;

    protected String getKey() {
        return key;
    }

    protected void setKey(String key) {
        this.key = key;
    }

    protected void setMainThread(boolean mainThread) {
        isMainThread = mainThread;
    }

    protected boolean isMainThread() {
        return isMainThread;
    }

    protected boolean isPassiveInit() {
        return isPassiveInit;
    }

    protected void setPassiveInit(boolean passiveInit) {
        isPassiveInit = passiveInit;
    }

    public InitStatus getInitStatus() {
        return initStatus;
    }

    public void setInitStatus(InitStatus initStatus) {
        this.initStatus = initStatus;
    }


    @Override
    public String toString() {
        return "BaseInitWork{" +
                "key='" + key + '\'' +
                ", initStatus=" + initStatus +
                ", isMainThread=" + isMainThread +
                ", isPassiveInit=" + isPassiveInit +
                '}';
    }
}
