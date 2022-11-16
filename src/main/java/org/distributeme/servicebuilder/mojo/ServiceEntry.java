package org.distributeme.servicebuilder.mojo;

import java.util.ArrayList;
import java.util.List;

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

	/**
	 * Additional JVM options.
	 */
	private String jvmOptions;

	/**
	 * Google Application Credentials file for service account.
	 */
	private String googleApplicationCredentialsFile;


	private List<String> profiles = new ArrayList<>();


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

	public String getJvmOptions() {
		return jvmOptions;
	}

	public List<String> getProfiles() {
		return profiles;
	}

	public void setProfiles(List<String> profiles) {
		this.profiles = profiles;
	}

	public void setJvmOptions(String jvmOptions) {
		this.jvmOptions = jvmOptions;
	}

	public String getGoogleApplicationCredentialsFile() {
		return googleApplicationCredentialsFile;
	}

	public void setGoogleApplicationCredentialsFile(String googleApplicationCredentialsFile) {
		this.googleApplicationCredentialsFile = googleApplicationCredentialsFile;
	}

	@Override
	public String toString() {
		return "ServiceEntry{" +
				"name='" + name + '\'' +
				", startClass='" + startClass + '\'' +
				", rmiPort=" + rmiPort +
				", jvmOptions="+ jvmOptions +
				", profiles="+ profiles +
				'}';
	}
}

