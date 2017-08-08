package com.phillips66.activemq.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FabricConnections {

	Map<String, FabricAdapter> connections = new HashMap<String, FabricAdapter>();
	
	String username;
	String password;
	String adapter = "http";
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setAdapter(String adapter) {
		this.adapter = adapter;
	}
	
	public String getAdapter() { 
		return this.adapter;
	}
 
	public synchronized FabricAdapter getConnection(String url) {
		FabricAdapter fabricAdapter = connections.get(url);
		if (fabricAdapter == null) {
			Properties prop = new Properties();
			prop.put("username", username);
			prop.put("password", password);
			prop.put("url", url);
			
			if(getAdapter().equals("jmx")) {
				fabricAdapter = new JmxAdapter();
			} else {
				fabricAdapter = new HttpAdapter();
			}
			fabricAdapter.connect(prop);
			connections.put(url, fabricAdapter);
		}
		return fabricAdapter;
	}
	
	public void shutdown() {
		for (Map.Entry<String, FabricAdapter> entry : connections.entrySet()) {
			FabricAdapter fabricAdapter = entry.getValue();
		    fabricAdapter.shutdown();
		}
		connections.clear();
	}
}
