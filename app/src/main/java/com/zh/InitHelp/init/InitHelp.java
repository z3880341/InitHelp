package com.zh.InitHelp.init;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class InitHelp {
    private List<BaseInitWork> mInitWorkList;
    private Queue<ListenerData> mListenerQueue = new LinkedList<>();
    private Queue<ListenerData> mViscosityListenerQueue = new LinkedList<>();
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Handler mListenerHandler = new Handler(Looper.getMainLooper());
    private Handler mChildHandler;
    private HandlerThread mChildHandlerThread = new HandlerThread("ChildInitHelp");
    private Application mApplication;
    private int mIndex = 0;

    private InitHelp(List<BaseInitWork> initWorkList, Application application) {
        mApplication = application;
        mInitWorkList = initWorkList;
        mChildHandlerThread.start();
        mChildHandler = new Handler(mChildHandlerThread.getLooper());
    }

    /**
     * 开始执行已经添加的所有初始化任务的初始化
     */
    public void launch() {
        if (mInitWorkList == null || mInitWorkList.isEmpty()) {
            return;
        }
        mIndex = 0;
        initRecursion(mIndex);
        handlerViscosityListener();
    }

    /**
     * 递归遍历初始化work
     *
     * @param index
     */
    private void initRecursion(int index) {
        if (index >= mInitWorkList.size()) {
            return;
        }
        BaseInitWork itemInitWork = mInitWorkList.get(index);
        if (itemInitWork instanceof InitWork) {
            if (itemInitWork.isMainThread()) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        InitResult result = initItem((InitWork) itemInitWork);
                        if (result == InitResult.FAIL_DISCONTINUE || result == InitResult.SUCCESS_DISCONTINUE) {
                            setDiscontinueNotRunningStatus(mIndex++);
                            return;
                        }
                        mIndex++;
                        initRecursion(mIndex);
                    }
                });

            } else {
                mChildHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        InitResult result = initItem((InitWork) itemInitWork);
                        if (result == InitResult.FAIL_DISCONTINUE || result == InitResult.SUCCESS_DISCONTINUE) {
                            setDiscontinueNotRunningStatus(mIndex++);
                            return;
                        }
                        mIndex++;
                        initRecursion(mIndex);
                    }
                });
            }
        } else {
            mIndex++;
            initRecursion(mIndex);
        }
    }

    private InitResult initItem(InitWork itemInitWork) {
        InitResult result = itemInitWork.onInit(mApplication);
        if (result == InitResult.FAIL_DISCONTINUE) {
            itemInitWork.setInitStatus(InitStatus.FAIL);
            handlerListener(itemInitWork.getKey(), false);
        }
        if (result == InitResult.SUCCESS_DISCONTINUE) {
            itemInitWork.setInitStatus(InitStatus.SUCCESS);
            handlerListener(itemInitWork.getKey(), true);
        }
        if (result == InitResult.SUCCESS_NEXT) {
            itemInitWork.setInitStatus(InitStatus.SUCCESS);
            handlerListener(itemInitWork.getKey(), true);
        }
        if (result == InitResult.FAIL_NEXT) {
            itemInitWork.setInitStatus(InitStatus.FAIL);
            handlerListener(itemInitWork.getKey(), false);
        }
        return result;
    }

    private void setDiscontinueNotRunningStatus(int startIndex) {
        for (int i = startIndex; i < mInitWorkList.size(); i++) {
            mInitWorkList.get(i).setInitStatus(InitStatus.DISCONTINUE_NOT_RUNNING);
        }
    }

    /**
     * 目标Key开始初始化
     *
     * @param targetKey
     */
    public void launchPassiveInitWork(String targetKey) {
        if (mInitWorkList == null || mInitWorkList.isEmpty()) {
            return;
        }
        for (BaseInitWork itemInitWork : mInitWorkList) {
            if (itemInitWork instanceof PassiveInitWork) {
                if (TextUtils.equals(itemInitWork.getKey(), targetKey)) {
                    if (itemInitWork.isMainThread()) {
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                itemInitWork.setInitStatus(((PassiveInitWork) itemInitWork).onInit(mApplication) ? InitStatus.SUCCESS : InitStatus.FAIL);
                                handlerListener(itemInitWork.getKey(), itemInitWork.getInitStatus() == InitStatus.SUCCESS);
                            }
                        });
                    } else {
                        mChildHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                itemInitWork.setInitStatus(((PassiveInitWork) itemInitWork).onInit(mApplication) ? InitStatus.SUCCESS : InitStatus.FAIL);
                                Log.d("ytzn", itemInitWork.getKey() + "初始化结果 = " + itemInitWork.getInitStatus());
                                handlerListener(itemInitWork.getKey(), itemInitWork.getInitStatus() == InitStatus.SUCCESS);
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * 查询初始化状态
     *
     * @param key 查询的key
     * @return
     */
    public InitStatus inquiryInitStatus(String key) {
        for (BaseInitWork item : mInitWorkList) {
            if (TextUtils.equals(item.getKey(), key)) {
                return item.getInitStatus();
            }
        }
        return InitStatus.NOT_RUNNING_YET;
    }

    /**
     * 设置初始化监听
     *
     * @param targetKey 需要监听的目标key
     * @param viscosity <p>
     *                  是否是粘性监听
     *                  粘性监听有必达性,就算是目标初始化已经完成了依然会回调初始化结果.
     *                  如果目标初始化未进行,此监听会等待初始化结果完成后在回调.
     *                  粘性监听注册一次,只会回调一次
     *                  </p>
     *                  <p>
     *                  如果是非粘性监听,那就是正常监听回调,只有目标初始化完成后会回调
     *                  </p>
     * @param listener  监听回调
     */
    public void setInitListener(String targetKey, boolean viscosity, InitListener listener) {
        if (viscosity) {
            mViscosityListenerQueue.offer(new ListenerData(targetKey, listener));
            handlerViscosityListener();
        } else {
            mListenerQueue.offer(new ListenerData(targetKey, listener));
        }
    }

    /**
     * 移除监听器
     *
     * @param targetKey
     */
    public void removeInitListener(String targetKey) {
        for (ListenerData item : mListenerQueue) {
            if (TextUtils.equals(item.getKey(), targetKey)) {
                mListenerQueue.remove(item);
            }
        }
        for (ListenerData item : mViscosityListenerQueue) {
            if (TextUtils.equals(item.getKey(), targetKey)) {
                mViscosityListenerQueue.remove(item);
            }
        }
    }

    private void handlerListener(String targetKey, boolean isInitSuccess) {
        for (ListenerData item : mListenerQueue) {
            if (TextUtils.equals(item.getKey(), targetKey)) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        item.listener.onInitResult(isInitSuccess);
                        mListenerQueue.remove(item);
                    }
                });
            }
        }
    }

    private synchronized void handlerViscosityListener() {
        if (mViscosityListenerQueue.isEmpty()) {
            return;
        }
        if (mInitWorkList == null || mInitWorkList.isEmpty()) {
            return;
        }
        synchronized (InitHelp.class) {
            List<ListenerData> removeList = new ArrayList<>();
            for (ListenerData item : mViscosityListenerQueue) {
                for (BaseInitWork initWork : mInitWorkList) {
                    if (TextUtils.equals(initWork.getKey(), item.getKey())) {
                        if (initWork.getInitStatus() != InitStatus.NOT_RUNNING_YET) {
                            if (initWork.getInitStatus() == InitStatus.SUCCESS) {
                                item.listener.onInitResult(true);
                            } else if (initWork.getInitStatus() == InitStatus.FAIL) {
                                item.listener.onInitResult(false);
                            }
                            removeList.add(item);
                        }
                    }
                }
            }
            mViscosityListenerQueue.removeAll(removeList);
            if (!mViscosityListenerQueue.isEmpty()) {
                boolean isHasTask = false;
                for (BaseInitWork initWork : mInitWorkList) {
                    if (initWork.getInitStatus() == InitStatus.NOT_RUNNING_YET) {
                        isHasTask = true;
                        break;
                    }
                }
                mListenerHandler.removeCallbacksAndMessages(null);
                if (isHasTask) {
                    mListenerHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            handlerViscosityListener();
                        }
                    }, 500);
                }
            }
        }
    }

    public static class Build {
        private List<BaseInitWork> mInitWorkList = new ArrayList<>();
        private Application mApplication;

        public Build(Application application) {
            mApplication = application;
        }

        /**
         * 添加初始化工作
         *
         * @param key          key
         * @param isMainThread 是否是主线程初始化
         * @param initWork     初始化工作
         * @return
         */
        public Build addInitWork(String key, boolean isMainThread, InitWork initWork) {
            initWork.setMainThread(isMainThread);
            initWork.setKey(key);
            mInitWorkList.add(initWork);
            return this;
        }

        /**
         * 添加被动初始化工作
         * <p>
         * 这里的被动情况是指在正常初始化的情况下不执行这种被动初始化.
         * 这种被动初始化,需要调用{@link InitHelp#launchPassiveInitWork}传入key才能执行初始化.
         * 这种看似很鸡肋的添加初始化,是为了有统一的地方阅读初始化功能的代码
         * </p>
         *
         * @param key          key
         * @param isMainThread 是否是主线程初始化
         * @param initWork     初始化工作
         * @return
         */
        public Build addPassiveInitWork(String key, boolean isMainThread, PassiveInitWork initWork) {
            initWork.setMainThread(isMainThread);
            initWork.setKey(key);
            mInitWorkList.add(initWork);
            return this;
        }


        public InitHelp build() {
            return new InitHelp(mInitWorkList, mApplication);
        }
    }


    public static abstract class InitWork extends BaseInitWork {
        public abstract InitResult onInit(Application application);
    }

    public static abstract class PassiveInitWork extends BaseInitWork {
        /**
         * 正在初始化
         *
         * @param application 初始化结果 true=成功 false=失败
         * @return
         */
        public abstract boolean onInit(Application application);
    }

    private static class ListenerData {
        private String key;
        private InitListener listener;

        public ListenerData(String key, InitListener listener) {
            this.key = key;
            this.listener = listener;
        }

        public String getKey() {
            return key;
        }

        public InitListener getListener() {
            return listener;
        }
    }

}
