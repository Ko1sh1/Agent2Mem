package com.example.springdemo.Controller;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

@Controller
public class IndexController {
    @RequestMapping(value={"/","/index"})
    @ResponseBody
    public String index() {
        return "Hello Hello!!!";
    }

    @RequestMapping(value = {"/poc"})
    @ResponseBody
    public String poc(@RequestParam(value = "poc",defaultValue = "") String poc) {
        if (poc.length() == 0){
            return "give me poc";
        }
        byte[] bpoc = Base64.decodeBase64(poc);
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bpoc));
            ois.readObject();
            ois.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "wow!";
    }
}
