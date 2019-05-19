package com.hzq;

import com.hzq.annotation.MyAutowired;
import com.hzq.annotation.MyController;
import com.hzq.annotation.MyRequestMapping;
import com.hzq.annotation.MyService;
import sun.net.www.protocol.jar.URLJarFile;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MyDispatcherServlet extends HttpServlet {

    private static final String scanPackage = "scanPackage";
    private static final String contextConfigLocation = "contextConfigLocation";

    //保存配置信息
    private Properties properties = new Properties();
    private List<String> classNames = new ArrayList<>();
    private Map<String, Object> ioc = new HashMap<>();
    private Map<String, Method> handlerMapping = new HashMap<>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6.dispatcher
        try {
            doDipatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDipatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        System.out.println("uri:" + url + ", contextPath:" + contextPath);
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 not found!");
            return;
        }

        Method method = this.handlerMapping.get(url);
        Class[] paramTypes = method.getParameterTypes();
        Map<String, String[]> params = req.getParameterMap();
        Object[] paramValues = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++){
            Class paramType = paramTypes[i];
            if(paramType == HttpServletRequest.class){
                paramValues[i] = req;
            } else if (paramType == HttpServletResponse.class){
                paramValues[i] = resp;
            } else if(paramType == String.class){
                paramValues[i] = params.get(0)[0];
            }
        }

        String beanName = lowerFirstCase(method.getDeclaringClass().getName());
        method.invoke(ioc.get(beanName), paramValues);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.load config
        doLoadConfig(config.getInitParameter(contextConfigLocation));

        //2.scan package
        doScannerPackage(properties.getProperty(scanPackage));

        //3.instance ioc
        doInstanceIoc();

        //4.DI
        doAutowired();

        //5.init handler mapping
        initHandlerMapping();

        System.out.println("MyDispatcherServlet init success!");

    }

    private void initHandlerMapping() {
        for (Map.Entry<String, Object> entry : ioc.entrySet()){
            Class clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(MyController.class)){continue;}
            String baseUrl = "";
            if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping myRequestMapping = (MyRequestMapping)clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = myRequestMapping.value();
            }

            Method[] methods = clazz.getMethods();
            for (Method m : methods){
                if(!m.isAnnotationPresent(MyRequestMapping.class)){continue;}
                MyRequestMapping myRequestMapping = m.getAnnotation(MyRequestMapping.class);
                String url = baseUrl + "/" + myRequestMapping.value();
                url = url.replaceAll("/+", "/");
                handlerMapping.put(url, m);
                System.out.println("mapped url:" + url + "method:" + m);
            }
        }
    }

    private void doAutowired() {
        for (Map.Entry<String, Object> entry : ioc.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field f : fields){
                if(!f.isAnnotationPresent(MyAutowired.class)){continue; }
                MyAutowired myAutowired = f.getAnnotation(MyAutowired.class);
                String beanName = myAutowired.value();
                if("".equals(beanName)){
                    beanName = f.getType().getName();
                }
                f.setAccessible(true);
                try {
                    f.set(entry.getValue(), ioc.get(beanName));
                } catch (Throwable t){
                    t.printStackTrace();
                }
            }
        }
    }

    private void doInstanceIoc(){
        try {
            for (String className : classNames){
                Class clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)){
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(MyService.class)){
                    MyService myService = (MyService)clazz.getAnnotation(MyService.class);
                    String beanName = myService.value();
                    if(!"".equals(beanName)){
                        ioc.put(beanName.trim(), clazz.newInstance());
                        continue;
                    }

                    Class[] classes = clazz.getInterfaces();
                    for (Class c : classes){
                        ioc.put(c.getName(), clazz.newInstance());
                    }
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    private void doScannerPackage(String packagePath) {
        URL url = this.getClass().getResource("/" + packagePath.replaceAll("\\.", "/"));
        File file = new File(url.getFile());
        for (File f : file.listFiles()){
            if(f.isDirectory()){
                doScannerPackage(packagePath + "." + f.getName());
            }
            classNames.add(packagePath + "." + f.getName().replaceAll(".class", "").trim());
        }
    }

    private void doLoadConfig(String path){
        InputStream is = null;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream(path);
            properties.load(is);
        } catch (Throwable t){
            t.printStackTrace();
        } finally {
            if(null != is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private String lowerFirstCase(String str){
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return new String(chars);
    }
}
