package com.hzq.controller;

import com.hzq.annotation.MyController;
import com.hzq.annotation.MyRequestMapping;
import com.hzq.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@MyController
@MyRequestMapping("/test")
public class TestController {

    @MyRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @MyRequestParam("userId") String userId){

        System.out.println("query,   ....   " + "query");
        try {
            resp.getWriter().write(userId + ", welcome to TestController.query");
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

}
