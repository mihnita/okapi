/*

 */
package net.sf.okapi.filters.xliff2;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.lib.xliff2.metadata.Metadata;

/**
 * @author jimh
 */
public class MetadataSkeleton implements ISkeleton {
	private IResource parent;
	private final Metadata metadata;

	public MetadataSkeleton(Metadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public ISkeleton clone() {
		return new MetadataSkeleton(new Metadata(metadata));
	}

	@Override
	public void setParent(IResource parent) {
		this.parent = parent;
	}

	@Override
	public IResource getParent() {
		return this.parent;
	}

	public Metadata getMetaData() {
		return metadata;
	}
}
