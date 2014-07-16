package net.cellcloud.storage.ehcache;

import net.cellcloud.exception.StorageException;
import net.cellcloud.storage.ResultSet;

public class CacheResultSet implements ResultSet{

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
		return cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	@Override
	public boolean absolute(int cursor) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean relative(int cursor) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean first() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean last() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean next() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean previous() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFirst() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLast() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public char getChar(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public char getChar(String label) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInt(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInt(String label) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLong(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLong(String label) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getString(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getString(String label) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getBool(int index) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getBool(String label) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte[] getRaw(String label, long offset, long length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateChar(int index, char value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateChar(String label, char value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateInt(int index, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateInt(String label, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateLong(int index, long value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateLong(String label, long value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateString(int index, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateString(String label, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBool(int index, boolean value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBool(String label, boolean value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateRaw(String label, byte[] src, int offset, int length)
			throws StorageException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateRaw(String label, byte[] src, long offset, long length)
			throws StorageException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
	
	

}
