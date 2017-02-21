package net.cellcloud.talk;

import net.cellcloud.talk.stuff.StuffVersion;

/**
 * 兼容性辅助函数。
 * 
 * @author Ambrose Xu
 */
public class CompatibilityHelper {

	protected CompatibilityHelper() {
	}

	public static StuffVersion match(int versionNumber) {
		if (versionNumber >= 150) {
			return StuffVersion.V2;
		}
		else {
			return StuffVersion.V1;
		}
	}
}
