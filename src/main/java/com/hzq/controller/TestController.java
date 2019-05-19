package com.hzq.controller;

import com.hzq.annotation.MyController;
import com.hzq.annotation.MyRequestMapping;
import com.hzq.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@MyController
@MyRequestMapping("/test")
public class TestController {

    public void query(HttpServletRequest request, HttpServletResponse response,
                      @MyRequestParam("userId") String userId){

        try {
            response.getWriter().write(userId + ", welcome to TestController.query");
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

}
