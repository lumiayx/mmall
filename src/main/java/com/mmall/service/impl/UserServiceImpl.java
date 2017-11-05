package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * Created by yinx on 17/10/23.
 */
@Service("iUserService")
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        String md5Password = MD5Util.MD5EncodeUtf8(password);

        User user = userMapper.selectLogin(username, md5Password);
        if (null == user) {
            return ServerResponse.createByErrorMessage("密码错误");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登录成功", user);
    }

    @Override
    public ServerResponse<String> register(User user) {

        ServerResponse validResponse = this.checkValid(user.getUsername(), Const.USER_NAME);
        if (!validResponse.isSuccess()) {
            return validResponse;
        }

        validResponse = this.checkValid(user.getUsername(), Const.EMAIL);
        if (!validResponse.isSuccess()) {
            return validResponse;
        }

        user.setRole(Const.Role.ROLE_CUSTOMER);
        //md5加密 md5工具类
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        int resultCount = userMapper.insert(user);
        if (resultCount != 1) {
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("恭喜,注册成功! ");

    }

    @Override
    public ServerResponse<String> checkValid(String str, String type) {
        if (!StringUtils.isNotBlank(str)) {
            return ServerResponse.createByErrorMessage("校验信息不能为空");
        }

        if (StringUtils.isNotBlank(type)) {
            int resultCount;
            if (Const.EMAIL.equals(type)) {
                resultCount = userMapper.checkUserEmail(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("邮箱已存在!");
                }
            } else if (Const.USER_NAME.equals(type)) {
                resultCount = userMapper.checkUsername(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("用户名已存在!");
                }
            } else {
                return ServerResponse.createByErrorMessage("没有要校验的类型!");
            }
        } else {
            return ServerResponse.createByErrorMessage("参数错误!");
        }

        return ServerResponse.createBySuccess("校验成功");
    }

    @Override
    public ServerResponse<User> getUserInfo(HttpSession session) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user != null) {
            return ServerResponse.createBySuccess(user);
        }
        return ServerResponse.createByErrorMessage("用户未登录,无法获取当前用户信息");
    }

    @Override
    public ServerResponse<String> forgetGetQuestion(String username) {
        ServerResponse<String> validResponse = this.checkValid(username, Const.USER_NAME);
        if (validResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if (StringUtils.isNotBlank(question)) {
            return ServerResponse.createBySuccessMessage(question);
        }
        return ServerResponse.createByErrorMessage("找回密码的问题是空");
    }

    @Override
    public ServerResponse<String> forgetCheckAnswer(String username, String question, String answer) {
        int resultCount = userMapper.checkAnswer(username, question, answer);
        if (resultCount > 0) {
            //TODO 为什么要token
            String forgetToken = UUID.randomUUID().toString();
            //放到本地cache中,设置有效期
            TokenCache.setKey("token_" + username, forgetToken);
            return ServerResponse.createBySuccessMessage(forgetToken);

        }
        return ServerResponse.createByErrorMessage("问题的回答错误");
    }

}
