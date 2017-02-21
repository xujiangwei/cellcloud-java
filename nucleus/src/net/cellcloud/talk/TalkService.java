package net.cellcloud.talk;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import net.cellcloud.common.MessageInterceptor;
import net.cellcloud.common.Service;
import net.cellcloud.core.Cellet;
import net.cellcloud.core.CelletSandbox;
import net.cellcloud.core.Endpoint;
import net.cellcloud.core.NucleusContext;
import net.cellcloud.exception.InvalidException;
import net.cellcloud.exception.SingletonException;
import net.cellcloud.http.CapsuleHolder;
import net.cellcloud.talk.dialect.Dialect;

/**
 * 会话服务。
 *
 * @author Ambrose Xu
 * 
 */
public class TalkService implements Service {

	private static TalkService instance = null;

	protected TalkServiceKernel kernel;

	public TalkService(NucleusContext nucleusContext) throws SingletonException {
		if (null == TalkService.instance) {
			TalkService.instance = this;

			this.kernel = new TalkServiceKernel(nucleusContext);
		}
		else {
			throw new SingletonException(TalkServiceKernel.class.getName());
		}
	}

	/**
	 * 返回会话服务单例。
	 */
	public static TalkService getInstance() {
		return TalkService.instance;
	}

	@Override
	public boolean startup() {
		return this.kernel.startup();
	}

	@Override
	public void shutdown() {
		this.kernel.shutdown();
	}

	public void setPort(int port) throws InvalidException {
		this.kernel.setPort(port);
	}

	public int getPort() {
		return this.kernel.getPort();
	}

	public void setBlockSize(int size) {
		this.kernel.setBlockSize(size);
	}

	public void setMaxConnections(int num) throws InvalidException {
		this.kernel.setMaxConnections(num);
	}

	public void setWorkerThreadNum(int num) throws InvalidException {
		this.kernel.setWorkerThreadNum(num);
	}

	public void httpEnabled(boolean enabled) throws InvalidException {
		this.kernel.httpEnabled(enabled);
	}

	public void setHttpPort(int port) {
		this.kernel.setHttpPort(port);
	}

	public void setHttpsPort(int port) {
		this.kernel.setHttpsPort(port);
	}

	public void setHttpQueueSize(int value) {
		this.kernel.setHttpQueueSize(value);
	}

	public void settHttpSessionTimeout(long timeoutInMillisecond) {
		this.kernel.settHttpSessionTimeout(timeoutInMillisecond);
	}

	public void startDaemon() {
		this.kernel.startDaemon();
	}

	public void stopDaemon() {
		this.kernel.stopDaemon();
	}

	public void startExtendHolder() {
		this.kernel.startExtendHolder();
	}

	public TalkSnapshoot snapshot() {
		return this.kernel.snapshot();
	}

	public Set<String> getTalkerList() {
		return this.kernel.getTalkerList();
	}

	public boolean notice(String targetTag, final Primitive primitive,
			final Cellet cellet, final CelletSandbox sandbox) {
		return this.kernel.notice(targetTag, primitive, cellet, sandbox);
	}

	public boolean notice(final String targetTag, final Dialect dialect,
			final Cellet cellet, final CelletSandbox sandbox) {
		return this.kernel.notice(targetTag, dialect, cellet, sandbox);
	}

	public boolean kick(final String targetTag, final Cellet cellet, final CelletSandbox sandbox) {
		return this.kernel.kick(targetTag, cellet, sandbox);
	}

	
	public int numSessions(String tag) {
		return this.kernel.numSessions(tag);
	}

	public Endpoint findEndpoint(String remoteTag) {
		return this.kernel.findEndpoint(remoteTag);
	}

	public boolean call(String[] identifiers, InetSocketAddress address) {
		return this.kernel.call(identifiers, address);
	}

	public boolean call(String[] identifiers, InetSocketAddress address, TalkCapacity capacity) {
		return this.kernel.call(identifiers, address, capacity);
	}

	public boolean call(List<String> identifiers, InetSocketAddress address) {
		return this.kernel.call(identifiers, address);
	}

	public boolean call(List<String> identifiers, InetSocketAddress address, boolean http) {
		return this.kernel.call(identifiers, address, http);
	}

	public boolean call(List<String> identifiers, InetSocketAddress address, TalkCapacity capacity) {
		return this.kernel.call(identifiers, address, capacity);
	}

	public boolean call(List<String> identifiers, InetSocketAddress address, TalkCapacity capacity, boolean http) {
		return this.kernel.call(identifiers, address, capacity, http);
	}

	public void hangUp(String[] identifiers) {
		this.kernel.hangUp(identifiers);
	}

	public void hangUp(List<String> identifiers) {
		this.kernel.hangUp(identifiers);
	}

	public boolean talk(String identifier, Primitive primitive) {
		return this.kernel.talk(identifier, primitive);
	}

	public boolean talk(String identifier, Dialect dialect) {
		return this.kernel.talk(identifier, dialect);
	}

	public boolean isCalled(final String identifier) {
		return this.kernel.isCalled(identifier);
	}

	public void addExtendHolder(CapsuleHolder holder) {
		this.kernel.addExtendHolder(holder);
	}

	public void removeExtendHolder(CapsuleHolder holder) {
		this.kernel.removeExtendHolder(holder);
	}

	public void addListener(TalkListener listener) {
		this.kernel.addListener(listener);
	}

	public void removeListener(TalkListener listener) {
		this.kernel.removeListener(listener);
	}

	public void setInterceptor(MessageInterceptor interceptor) {
		this.kernel.setInterceptor(interceptor);
	}

}
