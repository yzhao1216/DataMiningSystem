package com.tarena.bo;

/**
 * 该类用于描述unix系统日志文件中的一条日志
 * 
 * @author soft01
 * 
 */
public class LogData {
	/*
	 * 常量定义：每条日志都一样的内容
	 */
	/**
	 * 日志长度（字节量
	 * 
	 */
	public static final int LOG_LENGTH = 372;
	
	/**
	 * 用户名在一条日志的起始位置
	 */
	public static final int USER_OFFEST = 0; 
	
	
	/**
	 * 用户名在一条日志中占的字节量
	 * 
	 */
	public static final int USER_LENGTH = 32;
	
	/**
	 * 进程ID在一条日志中的起始位置
	 */
	public static final int PID_OFFEST = 68;
	
	/**
	 * 
	 */
	public static final int TYPE_OFFSET = 72;
	/**
	 * 昨日值生成第一条日志中的其实位置
	 * 
	 */
	public static final int TIME_OFFSET= 80;
	
	/**
	 * 用户地址在一条日志中的其实位置
	 */
	public static final int HOST_OFFEST = 114;
	/**
	 * 用户地址在一条日志中占有的时间量
	 */
	 public static final int HOST_LENGTH = 258;
	 
	 /**
	  * 日类文件一旦登录操作
	  */
	 public static final short TYPE_LOGIN=7;
	 /**
	  *日誌類型：登出
	  */
	 public static final short TYPE_LOGTOU = 8;
	
	// 用户名
	private String user;
	// 进程ID
	private int pid;
	// 日志类型
	private short type;
	// 日志生成时间
	private int time;
	// 用户IP地址
	private String host;

	public LogData() {

	}

	public LogData(String user, int pid, short type, int time, String host) {
		super();
		this.user = user;
		this.pid = pid;
		this.type = type;
		this.time = time;
		this.host = host;
	}
	/**
	 * 将给定的字符串进行解析，转换为一个LogData实例保存，前提是该字符串的格式
	 * 必须是由当前类的toString返回的格式
	 * 
	 * lidz,441232,7,1375334515,192.168.1.61
	 * @param log
	 */
	public LogData(String log){
		String[] data = log.split(",");
		this.user = data[0];
		this.pid = Integer.parseInt(data[1]);
		this.type = Short.parseShort(data[2]);
		this.time = Integer.parseInt(data[3]);
		this.host = data[4];
	}
	

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public String toString() {
		return user + "," + pid + "," + type + "," + time + "," + host;
	}
	
}
