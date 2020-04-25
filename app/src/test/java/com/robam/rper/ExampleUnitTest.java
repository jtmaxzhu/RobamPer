package com.robam.rper;

import com.robam.rper.injector.cache.ClassInfoCache;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    private static ClassInfoCache cache;
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    /**
     * 序列化
     * @throws IOException
     */
    @Test
    public void xlh() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(new File("string_file"));
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject("Hello World!");
        objectOutputStream.flush();
    }

    /**
     * 反序列化
     * @throws IOException
     */
    @Test
    public void fxlh() throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(new File("string_file"));
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        String string = (String) objectInputStream.readObject();
        System.out.println(string);
    }


}