package de.metas.ui.web.window.datatypes.json;

import java.time.format.DateTimeFormatter;

import org.compiere.util.Env;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2019 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@Value
@Builder
public class JSONDateConfig
{
	public static final JSONDateConfig DEFAULT = JSONDateConfig.builder()
			.zonedDateTimeFormatter(Env.DATE_FORMAT)
			.timestampFormatter(Env.DATE_FORMAT)
			.localDateTimeFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
			.localDateFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
			.localTimeFormatter(DateTimeFormatter.ofPattern("HH:mm"))
			.timeZoneFormatter(DateTimeFormatter.ofPattern("XXX"))
			.build();

	public static final JSONDateConfig LEGACY = JSONDateConfig.builder()
			.zonedDateTimeFormatter(Env.DATE_FORMAT)
			.timestampFormatter(Env.DATE_FORMAT)
			.localDateTimeFormatter(Env.DATE_FORMAT)
			.localDateFormatter(Env.DATE_FORMAT)
			.localTimeFormatter(Env.DATE_FORMAT)
			.timeZoneFormatter(Env.DATE_FORMAT)
			.build();

	@NonNull
	DateTimeFormatter zonedDateTimeFormatter;
	
	@NonNull
	DateTimeFormatter timestampFormatter;
	

	@NonNull
	DateTimeFormatter localDateTimeFormatter;

	@NonNull
	DateTimeFormatter localDateFormatter;

	@NonNull
	DateTimeFormatter localTimeFormatter;

	@NonNull
	DateTimeFormatter timeZoneFormatter;

	public boolean isLegacy()
	{
		return this == LEGACY;
	}
}
