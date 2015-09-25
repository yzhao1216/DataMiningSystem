package com.tarena;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.tarena.bo.LogData;
import com.tarena.bo.LogMatch;

/**
 * 该客户端运行在电信运营的Unix云服务器上 用于周期性的读取系统日志文件wtmpx， 搜集在该服务器上 使用的用户信息。
 * 将每个用户的登入与等处日志进行配对，然后发送给计费服务器保存 搜集分为3步：
 * 1:批量读取wtmpx文件中的多条日志并进行解析，将解析后的日志写入另一个文件保存
 * 2:读取保存解析后的日志文件，将日志进行登入登出配对，并将配对的日志存入另一个文件中 等待发送 3:读取保存配对日志的文件，将每一组配对日志发送给服务端
 * 
 * 
 * 
 * @author soft01
 * 
 * 
 */
public class DMSClient {
	/*
	 * 第一步需要用到的属性
	 */
	// 文件：unix系统日志文件（wtmpx）
	private File logFile;

	// 文件：解析后的
	private File textlogFile;

	// 文件：配对后的,记录上次解析后读取到wtmpx的位置（书签）
	private File lastPositionFile;

	// 一次从系统日志中解析日志的记录数
	private int batch;
	/*
	 * 第二步需要用到的属性
	 */

	// 保存所有配对日志文件
	private File logMatchFile;

	// 保存所有没有配对成功的日志
	private File logNoMatchFile;

	/*
	 * 第三步需要用的属性
	 */
	// 服务端的地址
	private String serverHost;

	// 服务端的端口
	private int serverPort;

	/*
	 * 初始化客户端
	 */
	public DMSClient() throws Exception {
		// 1 加载配置文件
		// 这里应该直接向外抛出异常，因为如果配置不成功，无法初始化，main中无法执行
		Map<String, String> config = loadConfig();
		// 打桩
		// System.out.println(config);

		// 2根据配置文件中的配置项初始化相关属性
		init(config);
	}

	/**
	 * 初始化客户端需要的属性
	 */
	private void init(Map<String, String> config) throws Exception {
		/*
		 * 初始化第一步需要的属性
		 */
		// File f = new File();
		logFile = new File(config.get("logfile"));

		textlogFile = new File(config.get("textLogfile"));

		lastPositionFile = new File(config.get("lastpositionfile"));

		batch = Integer.parseInt(config.get("batch"));

		/*
		 * 初始化第二部需要的属性
		 */
		logMatchFile = new File(config.get("logMatchFile"));

		logNoMatchFile = new File(config.get("logNoMatchFile"));

		/*
		 * 初始化第三步需要的属性
		 */
		serverHost = config.get("serverhost");
		serverPort = Integer.parseInt(config.get("serverport"));

	}

	/**
	 * 读取项目根目录下的配置文件config.xml 并将标签名作为key， 标签中间的文本作为value存入Map后返回
	 * 
	 * @return config.xml 里所有的配置项
	 * @throws 若加载配置文件的过程中出现错误抛出异常
	 */
	private Map<String, String> loadConfig() throws Exception {
		Map<String, String> config = new HashMap<String, String>();

		SAXReader reader = new SAXReader();
		Document doc = reader.read(new FileInputStream("config.xml"));
		Element root = doc.getRootElement();

		// 获取config标签下的子标签
		List<Element> elements = root.elements();

		// 循环每一个配置项，将标签名作为key，文本作为value 存入map（）
		for (Element e : elements) {
			config.put(e.getName(), e.getTextTrim());

		}

		return config;

	}

	/**
	 * 第一步：解析工作
	 * 
	 * 读取unix系统日志文件中的batch条日志，将每一条日志解析为一条可读日志，最终将这些日志写入到属性 textLogFile表示的文件中。
	 * 
	 * @return 解析成功返回true
	 */
	private boolean parseLogs() {
		/*
		 * 实现步骤： 1：必要的判断工作 1.1：unix系统日志文件必须存在！ 属性logFile表示的文件（wtmpx）
		 * 1.2:i根据书签信息，判断unix系统日志文件中是否还有日志可以解读若有日志还可以解析，
		 * 将上回读取的位置返回，返回-1则表示没有日志可以解析了。
		 * 1.3：a判断textLogFile文件是否以及存在，如果不存在，说明第二步成功执行
		 * 因为第二不执行完毕后，会将该文件中的日志分别保存到两个不同的文件中，从而
		 * 将该文件（log.txt）删除。那么这里就可以再次冲unix系统日志文件中
		 * 解析新的日志出来并存入该文件。若存在，说明第一步执行了，但是第二部没有 成功，所以第一步无需再次执行。
		 * 2：解析，创建RandomAccessFile来读取unix系统日志文件（logFile）
		 * 3：先将RandomAccessFile的指针seek到上一次最后读取的位置，准备开始解析。
		 * 4：创建一个List集合，用来保存每一条解出来的日志。
		 * 5：循环batch次，来进行解析日志。并将每一条日志都存入List集合中，将每一条日志 转换为一个Logdata实例 JAVA BEAN
		 * 
		 * 6：将集合中额每一条日志（LogData 实例）toString 返回的字符串 按行写入到textLogFile表示的文件中
		 * 7：将RandomAccessFile当前指针位置写入到lastPositionFile文件中
		 * 
		 * 
		 */

		// 1
		// 1.1
		if (!logFile.exists()) {
			System.out.println(logFile + " Not exits");
		}
		System.out.println(logFile + " exits");
		RandomAccessFile raf = null;
		try {
			// 1.2
			long lastPosition = hasLogs();
			if (lastPosition < 0) {
				System.out.println("There is no file to be analized");
				return false;
			}
			// 打桩
			System.out.println("lastPosition:" + lastPosition);
			// 1.3
			if (textlogFile.exists()) {
				return true;
			}

			// 2
			raf = new RandomAccessFile(logFile, "r");

			// 3
			raf.seek(lastPosition);

			// 4

			List<LogData> list = new ArrayList<LogData>();
			// 5
			// System.out.println("batch:"+batch);
			for (int i = 0; i < batch; i++) {
				/*
				 * 每当解析一条日志前，先判断时候还有日志可以解析 若没有，停止循环
				 */
				if (logFile.length() - lastPosition < LogData.LOG_LENGTH) {
					break;
				}
				// 读用户名
				raf.seek(lastPosition + LogData.USER_OFFEST);
				String user = IOUtil.readString(raf, LogData.USER_LENGTH)
						.trim();
				// 读取PID
				raf.seek(lastPosition + LogData.PID_OFFEST);
				int pid = raf.readInt();
				// 读取type
				raf.seek(lastPosition + LogData.TYPE_OFFSET);
				short type = raf.readShort();
				// 读取time
				raf.seek(lastPosition + LogData.TIME_OFFSET);
				int time = raf.readInt();
				// 读取host
				raf.seek(lastPosition + LogData.HOST_OFFEST);
				String host = IOUtil.readString(raf, LogData.HOST_LENGTH)
						.trim();

				// 当读取完一条后，将raf指针位置更新到lastPosition上
				lastPosition = raf.getFilePointer();
				LogData log = new LogData(user, pid, type, time, host);
				list.add(log);
				// dazhuan
				// System.out.println(log);

			}
			// 6
			IOUtil.saveCollection(list, textlogFile);
			// 7
			IOUtil.saveLong(lastPosition, lastPositionFile);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		return false;
	}

	/**
	 * 第一步中的一个判断环节。 该方法用于判断unix系列日志文件中是否还有日志可以被解析 判断依据：系统日志文件的总长度与书签文件中记录的上次解析
	 * 后的位置之间的差异是否还够一条日志的长度
	 * 
	 * @return
	 */
	private long hasLogs() throws Exception {
		/*
		 * 若书签文件不存在，说明是第一次解析，那么直接返回0， 从头开始解析。
		 */
		if (!lastPositionFile.exists()) {
			return 0;
		}
		try {
			/*
			 * 若有，则读取该文件第一行内容（上次读取位置） 将其转换为long值等待判断。
			 */
			long lastPosition = IOUtil.readLong(lastPositionFile);

			if (logFile.length() - lastPosition >= LogData.LOG_LENGTH) {
				return lastPosition;
			}
			return -1;

		} catch (Exception e) {
			System.out.println("Read " + lastPositionFile + " faild");
			// 将异常抛出，告诉调用者读取失败
			throw e;
		}

	}

	/**
	 * 第二步：配对日志
	 * 
	 * @return
	 */
	private boolean matchLogs() {
		/*
		 * 步骤： 1：必要的判断 1.1：判断第一步生成的用于保存解析后的日志文件textLogFile要存在！
		 * 1.2:判断保存配对日志的文件是否存在，若存在无需重复配对，若不存在 则开始新的配对工作.
		 * 2：将txtLogFile中等待配对的日志都读取出来，并存入一个集合等待配对。
		 * 3：查看上次第二步执行完毕后生存的用于保存所有没有配对成功的日志文件logNoMatch
		 * 是否存在，若存在则将上次没有配对成功的日志也读取出来，放入List集合等待一起配对。 4:归类日志
		 * 创建一个map用于保存所有登入日志，key使用日志的“user,pid,host” 创建一个map用于保存所有登出日志，key格式同上
		 * 创建一个List，用于保存所有配对日志。
		 * 
		 * 遍历保存所有待配对的集合，将每一条日志按照登入和登出分别存入两个map中 5：根据登出找登录进行配对。
		 * 遍历保存所有登出日志的map，取出每条登出日志，并使用其对应的key取登入map
		 * 中提取登入日志，然后将这两条日志转换为一个LogMatch实例保存并存入用于保存配对日志
		 * 的集合中，然后将该登入日志从登入日志map中删除。 这样以来， 保存配对日志的集合中就有这次所有配对成功的日志类，并且登入map中
		 * 也仅仅剩下所有没有配对成功的日志类。
		 * 
		 * 6：保存 将所有配对成功的日志写入logMatchFile文件
		 * 
		 * 将保存登入map中剩下的所有登入日志写入到logNoMatchFile中；
		 * 
		 * 
		 */
		// 1
		// 1.1
		if (!textlogFile.exists()) {
			System.out.println(textlogFile + "not exists");
			return false;
		}
		// 1.2
		if (logMatchFile.exists()) {
			return true;
		}
		try {
			// 2
			List<LogData> list = IOUtil.readLogs(textlogFile);

			// 3
			if (logNoMatchFile.exists()) {
				list.addAll(IOUtil.readLogs(logNoMatchFile));

			}
			// 4
			Map<String, LogData> loginMap = new HashMap<String, LogData>();

			Map<String, LogData> logoutMap = new HashMap<String, LogData>();

			List<LogMatch> matches = new ArrayList<LogMatch>();

			for (LogData log : list) {
				String key = log.getUser() + "," + log.getPid() + ","
						+ log.getHost();
				if (log.getType() == LogData.TYPE_LOGIN) {
					loginMap.put(key, log);

				} else if (log.getType() == LogData.TYPE_LOGTOU) {
					logoutMap.put(key, log);
				}
			}
			// dazhuang
			// System.out.println(loginMap);
			// System.out.println(logoutMap);
			// dazhuang
			// for (LogData log : list) {
			// System.out.println(log);
			// }
			// 5
			Set<Entry<String, LogData>> entrySet = logoutMap.entrySet();
			for (Entry<String, LogData> entry : entrySet) {
				LogData logout = entry.getValue();
				LogData login = loginMap.get(entry.getKey());
				loginMap.remove(entry.getKey());
				LogMatch logMatch = new LogMatch(login, logout);
				matches.add(logMatch);
			}

			// 6 保存配对日志
			IOUtil.saveCollection(matches, logMatchFile);

			// 保存所有没配对成功的日志
			IOUtil.saveCollection(loginMap.values(), logNoMatchFile);

			// 7配对成功后 将第一步生存的textLogFile文件删除
			textlogFile.delete();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 第三步：将所有配对日志发送至服务端
	 * 
	 * @return
	 */
	private boolean sendToServer() {
		/*
		 * 步骤： 1：必要的判断 1.1：保存配对日志的文件要存在！ 2：将所有配对日志读取回来并存入一个List集合中，等待发送。 3：连接服务端
		 * 4：将所有配对日志发送至服务端 5：所有日志发送后，再发送一个“Over”字符串，表示所有日志均发送完毕。
		 * 6：读取服务端发送回来的响应内容，若是“OK” ，表示服务端成功保存，那么 我们将第二部生成的配对文件logMatchFile删除。
		 */
		// 1
		if (!logMatchFile.exists()) {
			System.out.println(logMatchFile + " not exists");
			return false;
		}
		// 2
		Socket socket = null;
		try {
			List<String> matches = IOUtil.readMatchLog(logMatchFile);

			// 3
			socket = new Socket(serverHost, serverPort);

			// 4
			OutputStream out = socket.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(out, "UTF-8");
			PrintWriter pw = new PrintWriter(osw);

			for (String log : matches) {
				pw.println(log);
			}
			// 5
			pw.println("OVER");
			pw.flush();

			// 6
			InputStream in = socket.getInputStream();
			InputStreamReader isr = new InputStreamReader(in, "UTF-8");
			BufferedReader br = new BufferedReader(isr);

			// 读取服务端发送回来的响应
			String response = br.readLine();
			if ("OK".equals(response)) {
				logMatchFile.delete();
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	/*
	 * 客户端启动方法
	 */
	public void start() {
		while (true) {

			if (!parseLogs()) {
				continue;
			}
			if (!matchLogs()) {
				continue;
			}
			sendToServer(); 
		}
	}

	public static void main(String[] args) {
		try {
			DMSClient client = new DMSClient();
			client.start();
		} catch (Exception e) {
			System.out.println("Client start failed");
			e.printStackTrace();
		}
	}
}
