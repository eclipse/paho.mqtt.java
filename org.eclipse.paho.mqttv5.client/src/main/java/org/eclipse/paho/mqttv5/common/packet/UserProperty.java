package org.eclipse.paho.mqttv5.common.packet;

public class UserProperty {
	private final String key;
	private final String value;

	public UserProperty(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return key.hashCode() ^ value.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof UserProperty) {
			UserProperty property = (UserProperty) o;
			return this.key.equals(property.getKey()) && this.value.equals(property.getValue());
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "UserProperty [key=" + key + ", value=" + value + "]";
	}

}
