package com.tarena.bo;

/**
 * 该类用于表示一组配对日志
 * 
 * @author soft01
 * 
 */
public class LogMatch {
	private LogData login;
	private LogData logout;

	public LogMatch(LogData login, LogData logout) {
		super();
		this.login = login;
		this.logout = logout;
	}

	public LogMatch() {

	}

	public LogData getLogin() {
		return login;
	}

	public void setLogin(LogData login) {
		this.login = login;
	}

	public LogData getLogout() {
		return logout;
	}

	public void setLogout(LogData logout) {
		this.logout = logout;
	}

	@Override
	public String toString() {
		return login + "|" + logout;
	}
}
