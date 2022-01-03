# InitHelp
    帮助管理初始化流程的框架

'''
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        InitHelp help = new InitHelp.Build(this)
                //添加主动初始化
                .addInitWork("测试1", false, new InitHelp.InitWork() {
                    @Override
                    public InitResult onInit(Application application) {
                        Log.e("zh", "onInit: 测试1 初始化");
                        return InitResult.SUCCESS_NEXT;
                    }
                })
                //添加被动初始化
                .addPassiveInitWork("测试2", false, new InitHelp.PassiveInitWork() {
                    @Override
                    public boolean onInit(Application application) {
                        Log.e("zh", "onInit: 测试2 初始化");
                        return true;
                    }
                })
                .build();
        //开始初始化
        help.launch();
        //开始执行被动初始化
        help.launchPassiveInitWork("测试2");
        //添加粘性初始化结果查询
        help.setInitListener("测试2", true, new InitListener() {
            @Override
            public void onInitResult(boolean isInitSuccess) {
                Log.e("zh", "onInit: 测试2 初始化 结果 = " + isInitSuccess);
            }
        });
        //查询指定key的初始化结果
        InitStatus initStatus = help.inquiryInitStatus("测试1");
        Log.e("zh", "onInit: 测试1 初始化 结果 = " + initStatus);
    }
}
'''
