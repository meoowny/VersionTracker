package edu.tongji.versiontracker;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;

public class AppOpenListener implements AppLifecycleListener {
    /**
     * 应用启动后注册插件相关服务。IDEA 推荐在项目加载后再注册相关服务，以保证 IDEA 的启动速度。
     * 但是考虑到 component 被弃用，而 ProjectActivity 需要使用 Kotlin 代码，因此这里退而求其次，直接在应用启动时注册相关服务。
     */
    @Override
    public void appStarted() {
        System.out.println("hi");
        var service = ApplicationManager.getApplication().getService(VersionTrackerService.class);
        service.initService();
    }
}
