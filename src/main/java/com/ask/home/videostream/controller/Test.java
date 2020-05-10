package com.ask.home.videostream.controller;

import java.io.File;
import java.io.IOException;

public class Test {

    public static void main(String[] args) throws IOException {
        
   System.out.println(getFileSize());
}

public static Long getFileSize() {

   //     File file = new File("/Users/sourav/Downloads/sunflower.mp4");

   File file = new File("/Users/sourav/Documents/Java Projects/video/toystory.mp4");
        if(file.exists())
        return file.length();
        else{
            return 1L;
        }
    }
}
    