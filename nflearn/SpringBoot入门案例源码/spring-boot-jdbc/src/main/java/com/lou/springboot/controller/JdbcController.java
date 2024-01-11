package com.lou.springboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class JdbcController {

    //自动配置，因此可以直接通过 @Autowired 注入进来
    @Autowired
    JdbcTemplate jdbcTemplate;

    // 查询所有记录
    @GetMapping("/users/queryAll")
    public List<Map<String, Object>> queryAll() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("select * from tb_user");
        return list;
    }

    // 新增一条记录
//    @GetMapping("/users/insert")
//    public Object insert(String name, String password) {
//        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(password)) {
//            return false;
//        }
//        jdbcTemplate.execute("insert into tb_user(`name`,`password`) value (\"" + name + "\",\"" + password + "\")");
//        return true;
//    }

    @PostMapping("/users/insert")
    public Object insert(@RequestBody UserDto userDto) {
        // Insert a new record method body
        if (StringUtils.isEmpty(userDto.getName()) || StringUtils.isEmpty(userDto.getPassword())) {
            return false;
        }
        jdbcTemplate.execute("insert into tb_user(`name`,`password`) value (\"" + userDto.getName() + "\",\"" + userDto.getPassword() + "\")");
        return true;
    }

    // DTO class to represent user data in the request body
    public static class UserDto {
        private String name;
        private String password;

        // Getters and setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

}
