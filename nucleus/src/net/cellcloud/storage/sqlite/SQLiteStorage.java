/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (www.cellcloud.net)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
-----------------------------------------------------------------------------
*/

package net.cellcloud.storage.sqlite;

import java.io.File;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.exception.StorageException;
import net.cellcloud.storage.ResultSet;
import net.cellcloud.storage.Schema;
import net.cellcloud.storage.Storage;
import net.cellcloud.util.property.Properties;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetTransaction;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

/** SQLite 存储器。
 * 
 * @author Jiangwei Xu
 */
public class SQLiteStorage implements Storage {

	public final static String TYPE_NAME = "SQLiteStorage";

	private String instanceName;

	private SQLiteStorageProperties properties;
	private SqlJetDb db;

	public SQLiteStorage(final String instanceName) {
		this.instanceName = instanceName;
	}

	@Override
	public String getName() {
		return this.instanceName;
	}

	@Override
	public String getTypeName() {
		return SQLiteStorage.TYPE_NAME;
	}

	@Override
	public boolean open(Properties properties) throws StorageException {
		if (!(properties instanceof SQLiteStorageProperties)) {
			return false;
		}

		if (null != this.db && this.db.isOpen()) {
			try {
				this.db.close();
			} catch (SqlJetException e) {
				Logger.log(SQLiteStorage.class, e, LogLevel.WARNING);
			}
		}

		this.properties = (SQLiteStorageProperties) properties;

		File dbFile = new File(this.properties.getDBFile());

		// 创建数据库
		try {
			this.db = SqlJetDb.open(dbFile, true);
			this.db.getOptions().setAutovacuum(true);
		} catch (SqlJetException e) {
			Logger.log(SQLiteStorage.class, e, LogLevel.WARNING);
			return false;
		}

		try {
			// 在事务里执行设置
			this.db.runTransaction(new ISqlJetTransaction() {
				@Override
			    public Object run(SqlJetDb db) throws SqlJetException {
			        db.getOptions().setUserVersion(1);
			        return true;
			    }
			}, SqlJetTransactionMode.WRITE);
		} catch (SqlJetException e) {
			Logger.log(SQLiteStorage.class, e, LogLevel.WARNING);
			return false;
		}

		return true;
	}

	@Override
	public void close() throws StorageException {
	}

	@Override
	public ResultSet store(String statement) throws StorageException {
		return null;
	}

	@Override
	public ResultSet store(Schema schema) throws StorageException {
		return null;
	}
}
