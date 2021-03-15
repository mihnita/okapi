/**
 * 
 */
package net.sf.okapi.filters.xliff2;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.lib.xliff2.core.Skeleton;

/**
 * @author jimh
 *
 */
public class Xliff2Skeleton implements ISkeleton {
	private IResource parent;
	private Skeleton skeleton;

	public Xliff2Skeleton(Skeleton skeleton) {
		this.skeleton = skeleton;
	}

	@Override
	public ISkeleton clone() {
		// FIXME: copy constructor for Skeleton?
		return new Xliff2Skeleton(skeleton);
	}

	@Override
	public void setParent(IResource parent) {
		this.parent = parent;
	}

	@Override
	public IResource getParent() {
		return this.parent;
	}
	
	Skeleton getXliff2Skeleton() {
		return skeleton;
	}

}
