package com.tarena;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * DMS服务端 接受所有客户发送过来的配对日志并存入到本地文件中保存
 * 
 * @author soft01
 * 
 */
public class DMSServer {
	// 服务端的 Serverocket
	private ServerSocket server;

	// 线程池 用来管理与客户端交互的线程
	private ExecutorService threadPool;

	// 用来保存所有客户端发送过来的配对日志文件
	private File serverLogFile;

	/*
	 * 消息队列，用于临时存放所有客户端发送过来的配对日志。方便写入文件的线程 获取日志后写出。所有与客户端交互的线程在获取类日志后都会将日志存入该队列
	 */
	private BlockingQueue<String> messageQueue;

	/**
	 * 服务端构造方法，用来初始化服务端
	 * 
	 * @throws Exception
	 */
	public DMSServer() throws Exception {
		try {
			// 1 加载配置文件
			Map<String, String> config = loadConfig();

			// 2初始化属性
			init(config);

		} catch (Exception e) {
			System.out.println("Failed");
			throw e;
		}
	}

	private void init(Map<String, String> config) throws Exception {
		//初始化消息队列
		messageQueue = new LinkedBlockingQueue<String>();
		
		                            
		
		
		// 初始化ServerSocket
		server = new ServerSocket(Integer.parseInt(config.get("serverport")));

		// 初始化线程池
		threadPool = Executors.newFixedThreadPool(Integer.parseInt(config
				.get("threadcount")));

		// 初始化保存配对日志的文件
		serverLogFile = new File(config.get("serverlogfile"));
		
		
		
	}

	/**
	 * 读取项目根目录下的server_config 配置文件 标签名作为key，文本作为value存入map
	 * 
	 * @return config
	 * @throws Exception
	 */
	public Map<String, String> loadConfig() throws Exception {

		Map<String, String> config = new HashMap<String, String>();
		SAXReader reader = new SAXReader();
		Document doc = reader.read(new FileInputStream("server_config.xml"));
		// 获取根标签
		Element root = doc.getRootElement();
		// 获取根标签下的子标签
		List<Element> elements = root.elements();

		for (Element e : elements) {
			config.put(e.getName(), e.getTextTrim());
		}

		return config;

	}

	/**
	 * 服务端开始工作的方法
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		try {
			// 启动用于保存客户端发送日志的线程
			new Thread(new SaveLogHandler()).setDaemon(true);
			new Thread(new SaveLogHandler()).start();

			// 循环监听服务端口等待客户端的链接
			while (true) {
				Socket socket = server.accept();
				threadPool.execute(new CliendHandler(socket));
			}

		} catch (Exception e) {
			throw e;
		}

	}

	/**
	 * 该线程用与与每个客户端进行交互。 具体工作为，接收该客户端发送过来的所有配对日志并保存，保存完毕后响应客户端
	 * 
	 * @author soft01
	 * 
	 */
	private class CliendHandler implements Runnable {

		private Socket socket;

		public CliendHandler(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			// 用于向客户端发送消息的输出流
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(new OutputStreamWriter(socket
						.getOutputStream(), "UTF-8"));
				/*
				 * 步骤： 1：通过socket 获取输入流，并包装为缓冲字符输入流，准备 读取客户端发送过来的配对日志
				 * 2：循环读取每一条日志，知道读取到“OVER”为止，并将每一条 日志写入文件保存。
				 * 3：若全部接受成功并存入文件后，回应客户端“OK“ 若异常 回应“ERROR”
				 */

				BufferedReader br = new BufferedReader(new InputStreamReader(
						socket.getInputStream(), "UTF-8"));

				String log = null;
				while ((log = br.readLine()) != null) {
					if ("OVER".equals(log)) {
						break;
					}
					messageQueue.offer(log);

				}
				pw.println("OK");
				pw.flush();

			} catch (Exception e) {
				if (pw != null) {
					pw.println("ERROR");
					pw.flush();
				}
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * 该线程负责循环从消息队列中取出每一条日志并写入到serverLogFile文件中。
	 * 
	 * @author soft01
	 * 
	 */
	private class SaveLogHandler implements Runnable {

		@Override
		public void run() {
			/*
			 * 步骤 1:创建用于向serverLogFile中写出数据的缓冲字符输出流 2：死循环一下工作
			 * 
			 * 2.1：判断消息队列中是否还有日志 2.2：若有日志，则取出一条，将其写入文件中
			 * 2.3：若没有，则先将当前输出流中缓存的所有日志全部写入文件然后休息一会儿
			 * 
			 */
			PrintWriter pw = null;
			try {
				// 1
				pw = new PrintWriter(serverLogFile);

				// 2
				while (true) {
					if (messageQueue.size() > 0) {
						pw.println(messageQueue.poll());
					} else {
						pw.flush();
						Thread.sleep(500);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (pw != null) {
					pw.close();
				}
			}

		}

	}

	public static void main(String[] args) {
		try {
			DMSServer server = new DMSServer();
			server.start();

		} catch (Exception e) {
			System.out.println("Server start failed");
			e.printStackTrace();
		}
	}
}
