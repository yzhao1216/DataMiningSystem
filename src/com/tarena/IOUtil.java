package com.tarena;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.tarena.bo.LogData;

/**
 * 该类是一个工具类，负责DMSClient所有的读写操作
 * 
 * @author soft01
 * 
 */

public class IOUtil {

	/**
	 * 从给定的文件中读取第一行字符串，并将其转化为一个long值后返回
	 * 
	 * @param file
	 * @return
	 */
	public static long readLong(File file) throws Exception {

		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					file), "UTF-8"));

			String line = br.readLine();
			return Long.parseLong(line);
		} finally {
			if (br != null) {
				br.close();
			}
		}

	}

	/**
	 * 该方法会从给定的RandomAccessFile指针当前位置开始连续读取给定 长度的字节，并转换为字符串后返回。
	 * 
	 * @param raf
	 * @param len
	 * @return
	 */
	public static String readString(RandomAccessFile raf, int len)
			throws Exception {

		byte[] data = new byte[len];
		raf.read(data);
		return new String(data, "ISO8859-1");

	}

	/**
	 * 将给定的集合中的每个元素的toString返回的字符串以行为单位写入给定的文件中
	 * 
	 * @param c
	 * @param file
	 * @throws Exception
	 */

	public static void saveCollection(Collection c, File file) throws Exception {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			for (Object obj : c) {
				pw.println(obj);
			}

		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	/**
	 * 将给定的long值以字符串的形式写入给定的文件第一行
	 * 
	 * @param l
	 * @param file
	 * @throws Exception
	 */
	public static void saveLong(long lon, File file) throws Exception {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			pw.println(lon);
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	/**
	 * 从给定的文件中读取每一行字符串， 并将其转换为一个LogData实例，然后将这些日志存入 一个集合后返回。
	 */
	public static List<LogData> readLogs(File file) throws Exception {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					file)));
			String line = null;
			List<LogData> logs = new ArrayList<LogData>();
			while ((line = br.readLine()) != null) {
				// 将其转换为LogData实例
				LogData log = new LogData(line);
				logs.add(log);

			}
			return logs;
		} finally {
			if (br != null) {
				br.close();
			}

		}
	}

	/**
	 * 将给定文件中的每一行字符串表示的一条配对日志读取出来并存入到一个List集合中后 返回该集合
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static List<String> readMatchLog(File file) throws Exception {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					file)));
			String line = null;
			List<String> list = new ArrayList<String>();
			while((line=br.readLine())!=null){
				list.add(line);
			}
			return list;
		} finally {
			if (br != null) {
				br.close();
			}
		}

	}
}
