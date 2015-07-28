package net.cellcloud.storage.ehcache;

import net.cellcloud.exception.StorageException;
import net.cellcloud.storage.ResultSet;

public class EhcacheResultSet implements ResultSet {

	private String key;
	private Object value;
	private String cacheName;
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getCacheName() {
		return this.cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	@Override
	public boolean absolute(int cursor) {
		return false;
	}

	@Override
	public boolean relative(int cursor) {
		return false;
	}

	@Override
	public boolean first() {
		return false;
	}

	@Override
	public boolean last() {
		return false;
	}

	@Override
	public boolean next() {
		return false;
	}

	@Override
	public boolean previous() {
		return false;
	}

	@Override
	public boolean isFirst() {
		return false;
	}

	@Override
	public boolean isLast() {
		return false;
	}

	@Override
	public char getChar(int index) {
		return this.value.toString().charAt(index);
	}

	@Override
	public char getChar(String label) {
		return this.value.toString().charAt(0);
	}

	@Override
	public int getInt(int index) {
		return Integer.parseInt(this.value.toString());
	}

	@Override
	public int getInt(String label) {
		return Integer.parseInt(this.value.toString());
	}

	@Override
	public long getLong(int index) {
		return Long.parseLong(this.value.toString());
	}

	@Override
	public long getLong(String label) {
		return Long.parseLong(this.value.toString());
	}

	@Override
	public String getString(int index) {
		return this.value.toString();
	}

	@Override
	public String getString(String label) {
		return this.value.toString();
	}

	@Override
	public boolean getBool(int index) {
		return Boolean.parseBoolean(this.value.toString());
	}

	@Override
	public boolean getBool(String label) {
		return Boolean.parseBoolean(this.value.toString());
	}

	@Override
	public Object getObject(int index) {
		return this.value;
	}

	@Override
	public Object getObject(final String label) {
		return this.value;
	}

	@Override
	public byte[] getRaw(String label, long offset, long length) {
		return null;
	}

	@Override
	public void updateChar(int index, char value) {
	}

	@Override
	public void updateChar(String label, char value) {
	}

	@Override
	public void updateInt(int index, int value) {
	}

	@Override
	public void updateInt(String label, int value) {
	}

	@Override
	public void updateLong(int index, long value) {
		
	}

	@Override
	public void updateLong(String label, long value) {
	}

	@Override
	public void updateString(int index, String value) {
	}

	@Override
	public void updateString(String label, String value) {
	}

	@Override
	public void updateBool(int index, boolean value) {
	}

	@Override
	public void updateBool(String label, boolean value) {
	}

	@Override
	public void updateRaw(String label, byte[] src, int offset, int length)
			throws StorageException {
	}

	@Override
	public void updateRaw(String label, byte[] src, long offset, long length)
			throws StorageException {
	}

	@Override
	public void close() {
	}
}
