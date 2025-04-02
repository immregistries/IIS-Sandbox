package org.immregistries.iis.kernal.model;

import org.apache.commons.lang3.builder.DiffBuilder;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.builder.ReflectionDiffBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public abstract class AbstractMappedObject extends AbstractDiffable<AbstractMappedObject> {

	public DiffResult diff(AbstractMappedObject obj) {
		// No need for null check, as NullPointerException correct if obj is null
		return ReflectionDiffBuilder.builder()
			.setExcludeFieldNames("patientReported", "enteredBy")
			.setDiffBuilder(DiffBuilder.builder()
				.setLeft(this)
				.setRight(obj)
				.setStyle(ToStringStyle.SHORT_PREFIX_STYLE)
				.build()
			)
			.build().build();
	}
}
