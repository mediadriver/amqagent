package com.phillips66.activemq.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JmxConnections {

	Map<String, JmxAdapter> connections = new HashMap<String,JmxAdapter>();
	
	String jmxUsername;
	String jmxPassword;
	
	
	
	public String getJmxUsername() {
		return jmxUsername;
	}
	public void setJmxUsername(String jmxUsername) {
		this.jmxUsername = jmxUsername;
	}

	public String getJmxPassword() {
		return jmxPassword;
	}
	public void setJmxPassword(String jmxPassword) {
		this.jmxPassword = jmxPassword;
	}


	public synchronized JmxAdapter getConnection(String jmxUrl) {
		JmxAdapter jmxAdapter = connections.get(jmxUrl);
		if (jmxAdapter == null) {
			Properties prop = new Properties();
			prop.put("jmxUsername", jmxUsername);
			prop.put("jmxPassword", jmxPassword);
			prop.put("jmxUrl", jmxUrl);
			jmxAdapter = new JmxAdapter(prop);
			jmxAdapter.connect();
			connections.put(jmxUrl, jmxAdapter);
		}
		return jmxAdapter;
	}
}
