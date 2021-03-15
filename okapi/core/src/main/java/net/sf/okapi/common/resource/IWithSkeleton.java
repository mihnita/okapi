package net.sf.okapi.common.resource;

import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.exceptions.OkapiNotImplementedException;

public interface IWithSkeleton {
	/**
	 * Gets the skeleton object for this resource.
	 * @return the skeleton object for this resource or null if there is none.
	 */
	default ISkeleton getSkeleton () {
		throw new OkapiNotImplementedException("Dummy implementation for ISkeleton. Not all resources have skeleton");
	}

	/**
	 * Sets the skeleton object for this resource.
	 * @param skeleton the skeleton object to set.
	 */
	default void setSkeleton (final ISkeleton skeleton) {
		throw new OkapiNotImplementedException("Dummy implementation for ISkeleton. Not all resources have skeleton");
	}
}
