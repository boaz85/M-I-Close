package com.boazsh.m_i_close.app.services;

public enum AlarmServiceMessage {
	
	ALARM_STARTED(0),
	ALARM_STOPPED(1),
	UNKNOWN(-1);
	
	private final int value;
	
	public static AlarmServiceMessage valueOf(int value) {
		
		switch (value) {
			case 0:
				return ALARM_STARTED;
			case 1:
				return ALARM_STOPPED;
			default:
				return UNKNOWN;
		}
	}

    private AlarmServiceMessage(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}