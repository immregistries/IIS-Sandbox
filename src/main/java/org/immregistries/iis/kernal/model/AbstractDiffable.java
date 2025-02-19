package org.immregistries.iis.kernal.model;

import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.builder.Diffable;
import org.apache.commons.lang3.builder.ReflectionDiffBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public abstract class AbstractDiffable<T> implements Diffable<T> {

	public DiffResult diff(T obj) {
		// No need for null check, as NullPointerException correct if obj is null
		return new ReflectionDiffBuilder(this, obj, ToStringStyle.SHORT_PREFIX_STYLE)
			.build();
	}
}
