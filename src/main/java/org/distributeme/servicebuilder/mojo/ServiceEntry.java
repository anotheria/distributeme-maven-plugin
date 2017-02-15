package org.distributeme.servicebuilder.mojo;

/**
 * Represents a single service @ Runtime.
 *
 * @author lrosenberg
 * @since 14/02/2017 14:57
 */
public class ServiceEntry {
	/**
	 * Name of the service.
	 */
	private String name;
	/**
	 * Class of the service.
	 */
	private String startClass;

	/**
	 * Port of the service
	 */
	private int rmiPort;

	private boolean autostart;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStartClass() {
		return startClass;
	}

	public void setStartClass(String startClass) {
		this.startClass = startClass;
	}

	public int getRmiPort() {
		return rmiPort;
	}

	public void setRmiPort(int rmiPort) {
		this.rmiPort = rmiPort;
	}

	public boolean isAutostart() {
		return autostart;
	}

	public void setAutostart(boolean autostart) {
		this.autostart = autostart;
	}

	@Override
	public String toString() {
		return "ServiceEntry{" +
				"name='" + name + '\'' +
				", startClass='" + startClass + '\'' +
				", rmiPort=" + rmiPort +
				", autostart=" + autostart +
				'}';
	}
}

