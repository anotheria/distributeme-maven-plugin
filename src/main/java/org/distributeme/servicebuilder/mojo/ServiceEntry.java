package org.distributeme.servicebuilder.mojo;

/**
 * TODO comment this class
 *
 * @author lrosenberg
 * @since 14/02/2017 14:57
 */
public class ServiceEntry {
	private String name;
	private String startClass;

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

	@Override
	public String toString() {
		return "ServiceEntry{" +
				"name='" + name + '\'' +
				", startClass='" + startClass + '\'' +
				'}';
	}
}

